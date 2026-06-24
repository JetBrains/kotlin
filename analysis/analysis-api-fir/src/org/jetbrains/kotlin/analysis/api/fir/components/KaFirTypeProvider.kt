/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirErrorType
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.fir.types.PublicTypeApproximator
import org.jetbrains.kotlin.analysis.api.fir.utils.ConeSupertypeCalculationMode
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.getAllStrictSupertypes
import org.jetbrains.kotlin.analysis.api.fir.utils.getDirectSupertypes
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseTypeProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.types.KaBuiltinTypes
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.InvalidFirElementTypeException
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfTypeSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.ConeTypeCompatibilityChecker
import org.jetbrains.kotlin.fir.analysis.checkers.ConeTypeCompatibilityChecker.isCompatible
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.java.enhancement.EnhancedForWarningConeSubstitutor
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnsupported
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirTypeProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseTypeProvider<KaFirSession>(), KaFirSessionComponent {
    private val cachedBuiltinTypes: KaBuiltinTypes by lazy {
        KaFirBuiltInTypes(rootModuleSession.builtinTypes, firSymbolBuilder, token)
    }

    override fun builtinTypes(): KaBuiltinTypes = cachedBuiltinTypes

    override fun approximateToDenotableSupertype(type: KaType, allowLocalDenotableTypes: Boolean): KaType? = withValidityAssertion {
        val coneType = type.coneType
        val approximatedConeType = PublicTypeApproximator.approximateToDenotableSupertype(
            coneType,
            rootModuleSession,
            approximateLocalTypes = true,
            shouldApproximateLocalType = { _, _ -> !allowLocalDenotableTypes }
        )

        return approximatedConeType?.asKaType()
    }

    override fun approximateToDenotableSubtype(type: KaType): KaType? = withValidityAssertion {
        val coneType = type.coneType
        val approximatedConeType = PublicTypeApproximator.approximateToDenotableSubtype(
            coneType,
            rootModuleSession
        )

        return approximatedConeType?.asKaType()
    }

    override fun approximateToDenotableSupertype(type: KaType, position: KtElement): KaType? = withPsiValidityAssertion(position) {
        val firFile = position.containingKtFile.getOrBuildFirFile(resolutionFacade)
        val scopeContext = ContextCollector.process(resolutionFacade, firFile, position)
        val scopeClassifiers = scopeContext?.towerDataContext?.localScopes?.map { localScope ->
            localScope.classLikeSymbols
        }

        /**
         * This map construction is required to avoid shadowed local types:
         *
         * ```kotlin
         * fun test(flag: Boolean) {
         *     class Foo
         *     val x = Foo()
         *
         *     if (flag) {
         *         class Fo<caret>o
         *         <expr>x</expr>
         *     }
         * }
         * ```
         *
         * In the example above there are two local classes `Foo`.
         * `x` has a type of outer local `Foo`, however, the denotable approximation is required in scope where another `Foo` is introduced.
         * Hence, we cannot approximate `x` as `Foo`, as it would resolve to another `Foo` type in this scope.
         *
         * That's why here we build a map of class symbols that are resolved for different names in the current context.
         * It iterates through all the scopes from the outermost to the localmost and puts all named classifiers into the map.
         * If there is already some classifier stored for a given name, then another more local classifier shadows the previous one in the map.
         */
        val allAccessibleClassifiers = HashMap<Name, FirClassLikeSymbol<*>>().apply {
            scopeClassifiers?.forEach { currentScope ->
                this.putAll(currentScope)
            }
        }

        val approximatedConeType = PublicTypeApproximator.approximateToDenotableSupertype(
            type.coneType,
            rootModuleSession,
            approximateLocalTypes = true,
            shouldApproximateLocalType = { _, typeMarker ->
                if (typeMarker !is ConeLookupTagBasedType) return@approximateToDenotableSupertype false
                allAccessibleClassifiers.get(typeMarker.lookupTag.name) != typeMarker.toRegularClassSymbol(analysisSession.firSession)
            }
        )

        return approximatedConeType?.asKaType()
    }

    override fun augmentedByWarningLevelAnnotations(type: KaType): KaType = withValidityAssertion {
        require(type is KaFirType)
        val coneType = type.coneType
        val substitutor = EnhancedForWarningConeSubstitutor(typeContext)
        val enhancedConeType = substitutor.substituteOrNull(coneType)

        return enhancedConeType?.asKaType() ?: type
    }

    override fun defaultType(symbol: KaClassifierSymbol): KaType = withValidityAssertion {
        with(analysisSession) {
            val firSymbol = symbol.firSymbol
            val defaultConeType = when (firSymbol) {
                is FirTypeParameterSymbol -> firSymbol.defaultType
                is FirClassLikeSymbol<*> -> firSymbol.defaultType()
                else -> errorWithAttachment("Unexpected ${firSymbol::class.simpleName}") {
                    withFirSymbolEntry("symbol", firSymbol)
                }
            }

            defaultConeType.asKaType()
        }
    }

    override fun commonSupertype(types: Iterable<KaType>): KaType = withValidityAssertion {
        val coneTypes = types.map { it.coneType }
        if (coneTypes.isEmpty()) {
            throw IllegalArgumentException("Got no types")
        }

        return analysisSession.firSession.typeContext.commonSuperTypeOrNull(coneTypes)!!.asKaType()
    }

    override fun type(typeReference: KtTypeReference): KaType = withPsiValidityAssertion(typeReference) {
        with(typeReference) {
            when (val fir = getFirBySymbols() ?: getOrBuildFir(resolutionFacade)) {
                is FirResolvedTypeRef -> fir.coneType.asKaType()
                is FirDelegatedConstructorCall -> fir.constructedTypeRef.coneType.asKaType()
                is FirTypeProjectionWithVariance -> {
                    when (val typeRef = fir.typeRef) {
                        is FirResolvedTypeRef -> typeRef.coneType.asKaType()
                        else -> handleUnexpectedFirElementError(fir, this@with)
                    }
                }

                else -> handleUnexpectedFirElementError(fir, this@with)
            }
        }
    }

    /**
     * Sometimes, we don't have a proper mapping between PSI and FIR elements yet,
     * so we have to mitigate the damage and return an error type.
     * Usually, this happens with incorrect code because it is hard to predict all possible ways
     * in which code can be broken in the source file.
     */
    private fun handleUnexpectedFirElementError(fir: FirElement?, element: KtElement): KaErrorType {
        val exception = InvalidFirElementTypeException(fir, element, emptyList())
        logger<KaFirTypeProvider>().error(exception)

        val coneErrorType = ConeErrorType(
            diagnostic = ConeUnsupported(
                reason = "The construction is not supported in the Analysis API yet",
                source = element.toKtPsiSourceElement(),
            )
        )

        return KaFirErrorType(coneErrorType, firSymbolBuilder)
    }

    /**
     * Try to get fir element for type reference through symbols.
     * When the type is declared in compiled code this is faster than building FIR from decompiled text.
     */
    private fun KtTypeReference.getFirBySymbols(): FirElement? {
        val parent = parent
        return when {
            parent is KtParameter && parent.ownerDeclaration != null && parent.typeReference === this ->
                parent.resolveToFirSymbolOfTypeSafe<FirValueParameterSymbol>(resolutionFacade, FirResolvePhase.TYPES)?.fir?.returnTypeRef

            parent is KtCallableDeclaration && (parent is KtNamedFunction || parent is KtProperty)
                    && (parent.receiverTypeReference === this || parent.typeReference === this) -> {
                val firCallable = parent.resolveToFirSymbolOfTypeSafe<FirCallableSymbol<*>>(
                    resolutionFacade, FirResolvePhase.TYPES
                )?.fir
                if (parent.receiverTypeReference === this) {
                    firCallable?.receiverParameter?.typeRef
                } else firCallable?.returnTypeRef
            }

            parent is KtConstructorCalleeExpression && parent.parent is KtAnnotationEntry -> {
                fun getFirDeclaration(annotationEntry: KtAnnotationEntry, ktTypeReference: KtTypeReference): FirMemberDeclaration? {
                    if (annotationEntry.typeReference !== ktTypeReference) return null
                    val declaration = annotationEntry.parent?.parent as? KtNamedDeclaration ?: return null
                    return when {
                        declaration is KtClassOrObject -> declaration.resolveToFirSymbolOfTypeSafe<FirClassLikeSymbol<*>>(
                            resolutionFacade, FirResolvePhase.TYPES
                        )?.fir
                        declaration is KtParameter && declaration.ownerFunction != null ->
                            declaration.resolveToFirSymbolOfTypeSafe<FirValueParameterSymbol>(
                                resolutionFacade, FirResolvePhase.TYPES
                            )?.fir
                        declaration is KtCallableDeclaration && (declaration is KtNamedFunction || declaration is KtProperty) -> {
                            declaration.resolveToFirSymbolOfTypeSafe<FirCallableSymbol<*>>(
                                resolutionFacade, FirResolvePhase.TYPES
                            )?.fir
                        }
                        else -> null
                    }
                }

                fun FirMemberDeclaration.findAnnotationTypeRef(annotationEntry: KtAnnotationEntry) = annotations.find {
                    it.psi === annotationEntry
                }?.annotationTypeRef

                val annotationEntry = parent.parent as KtAnnotationEntry
                val firDeclaration = getFirDeclaration(annotationEntry, this)
                if (firDeclaration != null) {
                    firDeclaration.findAnnotationTypeRef(annotationEntry) ?: (firDeclaration as? FirProperty)?.run {
                        backingField?.findAnnotationTypeRef(annotationEntry)
                            ?: getter?.findAnnotationTypeRef(annotationEntry)
                            ?: setter?.findAnnotationTypeRef(annotationEntry)
                    }
                } else null
            }
            else -> null
        }
    }

    override fun receiverType(expression: KtDoubleColonExpression): KaType? = withPsiValidityAssertion(expression) {
        with(expression) {
            when (val fir = getOrBuildFir(resolutionFacade)) {
                is FirGetClassCall -> {
                    fir.resolvedType.getReceiverOfReflectionType()?.asKaType()
                }
                is FirCallableReferenceAccess -> {
                    when (val explicitReceiver = fir.explicitReceiver) {
                        is FirThisReceiverExpression -> {
                            explicitReceiver.resolvedType.asKaType()
                        }
                        is FirPropertyAccessExpression -> {
                            explicitReceiver.resolvedType.asKaType()
                        }
                        is FirFunctionCall -> {
                            explicitReceiver.resolvedType.asKaType()
                        }
                        is FirResolvedQualifier -> {
                            explicitReceiver.symbol?.toLookupTag()?.constructType(
                                explicitReceiver.typeArguments.map { it.toConeTypeProjection() }.toTypedArray(),
                                isMarkedNullable = explicitReceiver.isNullableLhsForCallableReference
                            )?.asKaType()
                                ?: fir.resolvedType.getReceiverOfReflectionType()?.asKaType()
                        }
                        else -> {
                            fir.resolvedType.getReceiverOfReflectionType()?.asKaType()
                        }
                    }
                }
                else -> handleUnexpectedFirElementError(fir, this@with)
            }
        }
    }

    private fun ConeKotlinType.getReceiverOfReflectionType(): ConeKotlinType? {
        if (this !is ConeClassLikeType) return null
        if (lookupTag.classId.packageFqName != StandardClassIds.BASE_REFLECT_PACKAGE) return null
        return typeArguments.firstOrNull()?.type
    }

    override fun withNullability(type: KaType, isMarkedNullable: Boolean): KaType = withValidityAssertion {
        require(type is KaFirType)
        return type.coneType.withNullability(isMarkedNullable, rootModuleSession.typeContext).asKaType()
    }

    override fun hasCommonSubtypeWith(type: KaType, that: KaType): Boolean = withValidityAssertion {
        return analysisSession.firSession.typeContext.isCompatible(
            type.coneType,
            that.coneType
        ) == ConeTypeCompatibilityChecker.Compatibility.COMPATIBLE
    }

    override fun collectImplicitReceiverTypes(position: KtElement): List<KaType> = withPsiValidityAssertion(position) {
        val ktFile = position.containingKtFile
        val firFile = ktFile.getOrBuildFirFile(resolutionFacade)

        val context = ContextCollector.process(resolutionFacade, firFile, position)
            ?: errorWithAttachment("Cannot find context for ${position::class}") {
                withPsiEntry("position", position)
            }

        return context.towerDataContext.implicitValueStorage.implicitReceivers.map { it.type.asKaType() }
    }

    override fun directSupertypes(type: KaType, shouldApproximate: Boolean): Sequence<KaType> = withValidityAssertion {
        require(type is KaFirType)

        val substitution = ConeSupertypeCalculationMode.substitution(shouldApproximate)
        return sequence {
            for (supertype in type.coneType.getDirectSupertypes(analysisSession.firSession, substitution)) {
                yield(supertype.asKaType())
            }
        }
    }

    override fun allSupertypes(type: KaType, shouldApproximate: Boolean): Sequence<KaType> = withValidityAssertion {
        require(type is KaFirType)

        val substitution = ConeSupertypeCalculationMode.substitution(shouldApproximate)
        return sequence {
            for (supertype in type.coneType.getAllStrictSupertypes(analysisSession.firSession, substitution)) {
                yield(supertype.asKaType())
            }
        }
    }

    override fun arrayElementType(type: KaType): KaType? = withValidityAssertion {
        require(type is KaFirType)
        return type.coneType.arrayElementType()?.asKaType()
    }
}
