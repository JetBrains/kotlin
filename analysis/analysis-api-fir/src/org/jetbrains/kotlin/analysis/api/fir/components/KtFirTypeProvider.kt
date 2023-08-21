/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtBuiltinTypes
import org.jetbrains.kotlin.analysis.api.components.KtTypeProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.dispatchReceiverType
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.fir.types.PublicTypeApproximator
import org.jetbrains.kotlin.analysis.api.fir.utils.toConeNullability
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfTypeSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.fir.analysis.checkers.ConeTypeCompatibilityChecker
import org.jetbrains.kotlin.fir.analysis.checkers.ConeTypeCompatibilityChecker.isCompatible
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.util.bfs

internal class KtFirTypeProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken
) : KtTypeProvider(), KtFirAnalysisSessionComponent {
    override val builtinTypes: KtBuiltinTypes = KtFirBuiltInTypes(rootModuleSession.builtinTypes, firSymbolBuilder, token)

    override fun approximateToSuperPublicDenotableType(type: KtType, approximateLocalTypes: Boolean): KtType? {
        require(type is KtFirType)
        val coneType = type.coneType
        val approximatedConeType = PublicTypeApproximator.approximateTypeToPublicDenotable(
            coneType,
            rootModuleSession,
            approximateLocalTypes = approximateLocalTypes,
        )

        return approximatedConeType?.asKtType()
    }

    override fun approximateToSubPublicDenotableType(type: KtType, approximateLocalTypes: Boolean): KtType? {
        require(type is KtFirType)
        val coneType = type.coneType
        val approximatedConeType = rootModuleSession.typeApproximator.approximateToSubType(
            coneType,
            PublicTypeApproximator.PublicApproximatorConfiguration(localTypes = approximateLocalTypes),
        )

        return approximatedConeType?.asKtType()
    }

    override fun buildSelfClassType(symbol: KtNamedClassOrObjectSymbol): KtType {
        require(symbol is KtFirNamedClassOrObjectSymbol)
        symbol.firSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
        val firClass = symbol.firSymbol.fir
        val type = ConeClassLikeTypeImpl(
            firClass.symbol.toLookupTag(),
            firClass.typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), isNullable = false) }.toTypedArray(),
            isNullable = false
        )

        return type.asKtType()
    }

    override fun commonSuperType(types: Collection<KtType>): KtType? {
        return analysisSession.useSiteSession.typeContext
            .commonSuperTypeOrNull(types.map { it.coneType })
            ?.asKtType()
    }

    override fun getKtType(ktTypeReference: KtTypeReference): KtType {
        val parent = ktTypeReference.parent
        val fir = when {
            parent is KtParameter && parent.ownerFunction != null && parent.typeReference === ktTypeReference -> parent.resolveToFirSymbolOfTypeSafe<FirValueParameterSymbol>(
                firResolveSession, FirResolvePhase.TYPES
            )?.fir?.returnTypeRef
            parent is KtCallableDeclaration && (parent is KtNamedFunction || parent is KtProperty) && (parent.receiverTypeReference === ktTypeReference || parent.typeReference === ktTypeReference) -> {
                val firCallable = parent.resolveToFirSymbolOfTypeSafe<FirCallableSymbol<*>>(
                    firResolveSession, FirResolvePhase.TYPES
                )?.fir
                if (parent.receiverTypeReference === ktTypeReference) firCallable?.receiverParameter?.typeRef else firCallable?.returnTypeRef
            }
            parent is KtConstructorCalleeExpression && parent.parent is KtAnnotationEntry -> {
                fun FirMemberDeclaration.findAnnotationTypeRef(annotationEntry: KtAnnotationEntry) = annotations.find {
                    it.psi === annotationEntry
                }?.annotationTypeRef

                val annotationEntry = parent.parent as KtAnnotationEntry
                val firDeclaration = getFirDeclaration(annotationEntry, ktTypeReference)
                if (firDeclaration != null) {
                    firDeclaration.findAnnotationTypeRef(annotationEntry) ?: (firDeclaration as? FirProperty)?.run {
                        backingField?.findAnnotationTypeRef(annotationEntry)
                            ?: getter?.findAnnotationTypeRef(annotationEntry)
                            ?: setter?.findAnnotationTypeRef(annotationEntry)
                    }
                } else {
                    ktTypeReference.getOrBuildFir(firResolveSession)
                }
            }
            else -> ktTypeReference.getOrBuildFir(firResolveSession)
        }
        return when (fir) {
            is FirResolvedTypeRef -> fir.coneType.asKtType()
            is FirDelegatedConstructorCall -> fir.constructedTypeRef.coneType.asKtType()
            is FirTypeProjectionWithVariance -> {
                when (val typeRef = fir.typeRef) {
                    is FirResolvedTypeRef -> typeRef.coneType.asKtType()
                    else -> throwUnexpectedFirElementError(fir, ktTypeReference)
                }
            }
            else -> throwUnexpectedFirElementError(fir, ktTypeReference)
        }
    }

    private fun getFirDeclaration(annotationEntry: KtAnnotationEntry, ktTypeReference: KtTypeReference): FirMemberDeclaration? {
        if (annotationEntry.typeReference !== ktTypeReference) return null
        val declaration = annotationEntry.parent?.parent as? KtNamedDeclaration ?: return null
        return when {
            declaration is KtClassOrObject -> declaration.resolveToFirSymbolOfTypeSafe<FirClassLikeSymbol<*>>(
                firResolveSession, FirResolvePhase.TYPES
            )?.fir
            declaration is KtParameter && declaration.ownerFunction != null ->
                declaration.resolveToFirSymbolOfTypeSafe<FirValueParameterSymbol>(
                    firResolveSession, FirResolvePhase.TYPES
                )?.fir
            declaration is KtCallableDeclaration && (declaration is KtNamedFunction || declaration is KtProperty) -> {
                declaration.resolveToFirSymbolOfTypeSafe<FirCallableSymbol<*>>(
                    firResolveSession, FirResolvePhase.TYPES
                )?.fir
            }
            else -> return null
        }
    }

    override fun getReceiverTypeForDoubleColonExpression(expression: KtDoubleColonExpression): KtType? {
        return when (val fir = expression.getOrBuildFir(firResolveSession)) {
            is FirGetClassCall ->
                fir.resolvedType.getReceiverOfReflectionType()?.asKtType()
            is FirCallableReferenceAccess ->
                fir.resolvedType.getReceiverOfReflectionType()?.asKtType()
            else -> throwUnexpectedFirElementError(fir, expression)
        }
    }

    private fun ConeKotlinType.getReceiverOfReflectionType(): ConeKotlinType? {
        if (this !is ConeClassLikeType) return null
        if (lookupTag.classId.packageFqName != StandardClassIds.BASE_REFLECT_PACKAGE) return null
        return typeArguments.firstOrNull()?.type
    }

    override fun withNullability(type: KtType, newNullability: KtTypeNullability): KtType {
        require(type is KtFirType)
        return type.coneType.withNullability(newNullability.toConeNullability(), rootModuleSession.typeContext).asKtType()
    }

    override fun haveCommonSubtype(a: KtType, b: KtType): Boolean {
        return analysisSession.useSiteSession.typeContext.isCompatible(
            a.coneType,
            b.coneType
        ) == ConeTypeCompatibilityChecker.Compatibility.COMPATIBLE
    }

    override fun getImplicitReceiverTypesAtPosition(position: KtElement): List<KtType> {
        return analysisSession.firResolveSession.getTowerContextProvider(position.containingKtFile)
            .getClosestAvailableParentContext(position)?.implicitReceiverStack?.map { it.type.asKtType() } ?: emptyList()
    }

    override fun getDirectSuperTypes(type: KtType, shouldApproximate: Boolean): List<KtType> {
        require(type is KtFirType)
        return type.coneType.getDirectSuperTypes(shouldApproximate).mapTo(mutableListOf()) { it.asKtType() }
    }

    private fun ConeKotlinType.getDirectSuperTypes(shouldApproximate: Boolean): Sequence<ConeKotlinType> {
        return when (this) {
            // We also need to collect those on `upperBound` due to nullability.
            is ConeFlexibleType -> lowerBound.getDirectSuperTypes(shouldApproximate) + upperBound.getDirectSuperTypes(shouldApproximate)
            is ConeDefinitelyNotNullType -> original.getDirectSuperTypes(shouldApproximate).map {
                ConeDefinitelyNotNullType.create(it, analysisSession.useSiteSession.typeContext) ?: it
            }
            is ConeIntersectionType -> intersectedTypes.asSequence().flatMap { it.getDirectSuperTypes(shouldApproximate) }
            is ConeErrorType -> emptySequence()
            is ConeLookupTagBasedType -> getSubstitutedSuperTypes(shouldApproximate)
            else -> emptySequence()
        }.distinct()
    }

    private fun ConeLookupTagBasedType.getSubstitutedSuperTypes(shouldApproximate: Boolean): Sequence<ConeKotlinType> {
        val session = analysisSession.firResolveSession.useSiteFirSession
        val symbol = lookupTag.toSymbol(session)
        val superTypes = when (symbol) {
            is FirAnonymousObjectSymbol -> symbol.resolvedSuperTypes
            is FirRegularClassSymbol -> symbol.resolvedSuperTypes
            is FirTypeAliasSymbol -> symbol.fullyExpandedClass(session)?.resolvedSuperTypes ?: return emptySequence()
            is FirTypeParameterSymbol -> symbol.resolvedBounds.map { it.type }
            else -> return emptySequence()
        }

        val typeParameterSymbols = symbol.typeParameterSymbols ?: return superTypes.asSequence()
        val argumentTypes = (session.typeContext.captureArguments(this, CaptureStatus.FROM_EXPRESSION)?.toList()
            ?: this.typeArguments.mapNotNull { it.type })

        require(typeParameterSymbols.size == argumentTypes.size) {
            val renderedSymbol = FirRenderer.noAnnotationBodiesAccessorAndArguments().renderElementAsString(symbol.fir)
            "'$renderedSymbol' expects '${typeParameterSymbols.size}' type arguments " +
                    "but type '${this.renderForDebugging()}' has ${argumentTypes.size} type arguments."
        }

        val substitutor = substitutorByMap(typeParameterSymbols.zip(argumentTypes).toMap(), session)
        return superTypes.asSequence().map {
            val type = substitutor.substituteOrSelf(it)
            if (shouldApproximate) {
                session.typeApproximator.approximateToSuperType(
                    type,
                    TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
                ) ?: type
            } else {
                type
            }.withNullability(nullability, session.typeContext)
        }
    }

    override fun getAllSuperTypes(type: KtType, shouldApproximate: Boolean): List<KtType> {
        require(type is KtFirType)
        return listOf(type.coneType)
            .bfs { it.getDirectSuperTypes(shouldApproximate).iterator() }
            .drop(1)
            .mapTo(mutableListOf()) { it.asKtType() }
    }

    override fun getDispatchReceiverType(symbol: KtCallableSymbol): KtType? {
        require(symbol is KtFirSymbol<*>)
        val firSymbol = symbol.firSymbol
        check(firSymbol is FirCallableSymbol<*>) {
            "Fir declaration should be FirCallableDeclaration; instead it was ${firSymbol::class}"
        }
        return firSymbol.dispatchReceiverType(analysisSession.firSymbolBuilder)
    }

    override fun getArrayElementType(type: KtType): KtType? {
        require(type is KtFirType)
        return type.coneType.arrayElementType()?.asKtType()
    }
}

