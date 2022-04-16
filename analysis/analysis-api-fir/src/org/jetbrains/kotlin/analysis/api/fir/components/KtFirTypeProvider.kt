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
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.analysis.checkers.ConeTypeCompatibilityChecker
import org.jetbrains.kotlin.fir.analysis.checkers.ConeTypeCompatibilityChecker.isCompatible
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClass
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.util.bfs

internal class KtFirTypeProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtTypeProvider(), KtFirAnalysisSessionComponent {
    override val builtinTypes: KtBuiltinTypes = KtFirBuiltInTypes(rootModuleSession.builtinTypes, firSymbolBuilder, token)

    override fun approximateToSuperPublicDenotableType(type: KtType): KtType? {
        require(type is KtFirType)
        val coneType = type.coneType
        val approximatedConeType = PublicTypeApproximator.approximateTypeToPublicDenotable(
            coneType,
            rootModuleSession,
            approximateLocalTypes = true,
        )

        return approximatedConeType?.asKtType()
    }

    override fun buildSelfClassType(symbol: KtNamedClassOrObjectSymbol): KtType {
        require(symbol is KtFirNamedClassOrObjectSymbol)
        symbol.firSymbol.ensureResolved(FirResolvePhase.SUPER_TYPES)
        val firClass = symbol.firSymbol.fir
        val type = ConeClassLikeTypeImpl(
            firClass.symbol.toLookupTag(),
            firClass.typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), isNullable = false) }.toTypedArray(),
            isNullable = false
        )

        return type.asKtType()
    }

    override fun commonSuperType(types: Collection<KtType>): KtType? {
        return analysisSession.rootModuleSession.typeContext
            .commonSuperTypeOrNull(types.map { it.coneType })
            ?.asKtType()
    }

    override fun getKtType(ktTypeReference: KtTypeReference): KtType = withValidityAssertion {
        when (val fir = ktTypeReference.getOrBuildFir(firResolveState)) {
            is FirResolvedTypeRef -> fir.coneType.asKtType()
            is FirDelegatedConstructorCall -> fir.constructedTypeRef.coneType.asKtType()
            else -> throwUnexpectedFirElementError(fir, ktTypeReference)
        }
    }

    override fun getReceiverTypeForDoubleColonExpression(expression: KtDoubleColonExpression): KtType? = withValidityAssertion {
        when (val fir = expression.getOrBuildFir(firResolveState)) {
            is FirGetClassCall ->
                fir.typeRef.coneType.getReceiverOfReflectionType()?.asKtType()
            is FirCallableReferenceAccess ->
                fir.typeRef.coneType.getReceiverOfReflectionType()?.asKtType()
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
        return analysisSession.rootModuleSession.typeContext.isCompatible(
            a.coneType,
            b.coneType
        ) == ConeTypeCompatibilityChecker.Compatibility.COMPATIBLE
    }

    override fun getImplicitReceiverTypesAtPosition(position: KtElement): List<KtType> {
        return analysisSession.firResolveState.getTowerContextProvider(position.containingKtFile)
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
                ConeDefinitelyNotNullType.create(it, analysisSession.rootModuleSession.typeContext) ?: it
            }
            is ConeIntersectionType -> intersectedTypes.asSequence().flatMap { it.getDirectSuperTypes(shouldApproximate) }
            is ConeErrorType -> emptySequence()
            is ConeLookupTagBasedType -> getSubstitutedSuperTypes(shouldApproximate)
            else -> emptySequence()
        }.distinct()
    }

    private fun ConeLookupTagBasedType.getSubstitutedSuperTypes(shouldApproximate: Boolean): Sequence<ConeKotlinType> {
        val session = analysisSession.firResolveState.rootModuleSession
        val symbol = lookupTag.toSymbol(session)
        val superTypes = when (symbol) {
            is FirAnonymousObjectSymbol -> symbol.superConeTypes
            is FirRegularClassSymbol -> symbol.superConeTypes
            is FirTypeAliasSymbol -> symbol.fullyExpandedClass(session)?.superConeTypes ?: return emptySequence()
            is FirTypeParameterSymbol -> symbol.resolvedBounds.map { it.type }
            else -> return emptySequence()
        }

        val typeParameterSymbols = symbol.typeParameterSymbols ?: return superTypes.asSequence()
        val argumentTypes = (session.typeContext.captureArguments(this, CaptureStatus.FROM_EXPRESSION)?.toList()
            ?: this.typeArguments.mapNotNull { it.type })

        require(typeParameterSymbols.size == argumentTypes.size) {
            "'${symbol.fir.render(FirRenderer.RenderMode.NoBodies)}' expects '${typeParameterSymbols.size}' type arguments " +
                    "but type '${this.render()}' has ${argumentTypes.size} type arguments."
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
}

