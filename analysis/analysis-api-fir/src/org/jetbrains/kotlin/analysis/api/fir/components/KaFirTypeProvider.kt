/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.kotlin.analysis.api.components.KaBuiltinTypes
import org.jetbrains.kotlin.analysis.api.components.KaTypeProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.dispatchReceiverType
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirErrorType
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.fir.types.PublicTypeApproximator
import org.jetbrains.kotlin.analysis.api.fir.utils.ConeSupertypeCalculationMode
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.getAllStrictSupertypes
import org.jetbrains.kotlin.analysis.api.fir.utils.getDirectSupertypes
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.InvalidFirElementTypeException
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfTypeSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
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
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnsupported
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirTypeProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaTypeProvider, KaFirSessionComponent {
    override val builtinTypes: KaBuiltinTypes by lazy {
        KaFirBuiltInTypes(rootModuleSession.builtinTypes, firSymbolBuilder, token)
    }

    override fun KaType.approximateToSuperPublicDenotable(approximateLocalTypes: Boolean): KaType? = withValidityAssertion {
        require(this is KaFirType)
        val coneType = coneType
        val approximatedConeType = PublicTypeApproximator.approximateTypeToPublicDenotable(
            coneType,
            rootModuleSession,
            approximateLocalTypes = approximateLocalTypes,
        )

        return approximatedConeType?.asKtType()
    }

    override fun KaType.approximateToSubPublicDenotable(approximateLocalTypes: Boolean): KaType? = withValidityAssertion {
        require(this is KaFirType)
        val coneType = coneType
        val approximatedConeType = rootModuleSession.typeApproximator.approximateToSubType(
            coneType,
            PublicTypeApproximator.PublicApproximatorConfiguration(approximateLocalTypes = approximateLocalTypes),
        )

        return approximatedConeType?.asKtType()
    }

    override val KaType.enhancedType: KaType?
        get() = withValidityAssertion {
            require(this is KaFirType)
            val coneType = coneType
            val substitutor = EnhancedForWarningConeSubstitutor(typeContext)
            val enhancedConeType = substitutor.substituteType(coneType)

            return enhancedConeType?.asKtType()
        }

    override val KaClassifierSymbol.defaultType: KaType
        get() = withValidityAssertion {
            with(analysisSession) {
                val firSymbol = firSymbol
                val defaultConeType = when (firSymbol) {
                    is FirTypeParameterSymbol -> firSymbol.defaultType
                    is FirClassLikeSymbol<*> -> firSymbol.defaultType()
                    else -> errorWithAttachment("Unexpected ${firSymbol::class.simpleName}") {
                        withFirSymbolEntry("symbol", firSymbol)
                    }
                }

                defaultConeType.asKtType()
            }
        }

    override val Iterable<KaType>.commonSupertype: KaType
        get() = withValidityAssertion {
            val coneTypes = map { it.coneType }
            if (coneTypes.isEmpty()) {
                throw IllegalArgumentException("Got no types")
            }

            return analysisSession.firSession.typeContext.commonSuperTypeOrNull(coneTypes)!!.asKtType()
        }

    override val KtTypeReference.type: KaType
        get() = withPsiValidityAssertion {
            when (val fir = getFirBySymbols() ?: getOrBuildFir(resolutionFacade)) {
                is FirResolvedTypeRef -> fir.coneType.asKtType()
                is FirDelegatedConstructorCall -> fir.constructedTypeRef.coneType.asKtType()
                is FirTypeProjectionWithVariance -> {
                    when (val typeRef = fir.typeRef) {
                        is FirResolvedTypeRef -> typeRef.coneType.asKtType()
                        else -> handleUnexpectedFirElementError(fir, this)
                    }
                }

                else -> handleUnexpectedFirElementError(fir, this)
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
                        else -> return null
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

    override val KtDoubleColonExpression.receiverType: KaType?
        get() = withPsiValidityAssertion {
            when (val fir = getOrBuildFir(resolutionFacade)) {
                is FirGetClassCall -> {
                    fir.resolvedType.getReceiverOfReflectionType()?.asKtType()
                }
                is FirCallableReferenceAccess -> {
                    when (val explicitReceiver = fir.explicitReceiver) {
                        is FirThisReceiverExpression -> {
                            explicitReceiver.resolvedType.asKtType()
                        }
                        is FirPropertyAccessExpression -> {
                            explicitReceiver.resolvedType.asKtType()
                        }
                        is FirFunctionCall -> {
                            explicitReceiver.resolvedType.asKtType()
                        }
                        is FirResolvedQualifier -> {
                            explicitReceiver.symbol?.toLookupTag()?.constructType(
                                explicitReceiver.typeArguments.map { it.toConeTypeProjection() }.toTypedArray(),
                                isMarkedNullable = explicitReceiver.isNullableLHSForCallableReference
                            )?.asKtType()
                                ?: fir.resolvedType.getReceiverOfReflectionType()?.asKtType()
                        }
                        else -> {
                            fir.resolvedType.getReceiverOfReflectionType()?.asKtType()
                        }
                    }
                }
                else -> handleUnexpectedFirElementError(fir, this)
            }
        }

    private fun ConeKotlinType.getReceiverOfReflectionType(): ConeKotlinType? {
        if (this !is ConeClassLikeType) return null
        if (lookupTag.classId.packageFqName != StandardClassIds.BASE_REFLECT_PACKAGE) return null
        return typeArguments.firstOrNull()?.type
    }

    override fun KaType.withNullability(newNullability: KaTypeNullability): KaType = withValidityAssertion {
        require(this is KaFirType)
        return coneType.withNullability(nullable = newNullability == KaTypeNullability.NULLABLE, rootModuleSession.typeContext).asKtType()
    }

    override fun KaType.hasCommonSubtypeWith(that: KaType): Boolean = withValidityAssertion {
        return analysisSession.firSession.typeContext.isCompatible(
            this.coneType,
            that.coneType
        ) == ConeTypeCompatibilityChecker.Compatibility.COMPATIBLE
    }

    override fun collectImplicitReceiverTypes(position: KtElement): List<KaType> = withPsiValidityAssertion(position) {
        val ktFile = position.containingKtFile
        val firFile = ktFile.getOrBuildFirFile(resolutionFacade)

        val fileSession = firFile.llFirSession
        val sessionHolder = SessionHolderImpl(fileSession, fileSession.getScopeSession())

        val context = ContextCollector.process(firFile, sessionHolder, position)
            ?: errorWithAttachment("Cannot find context for ${position::class}") {
                withPsiEntry("position", position)
            }

        return context.towerDataContext.implicitValueStorage.implicitReceivers.map { it.type.asKtType() }
    }

    override fun KaType.directSupertypes(shouldApproximate: Boolean): Sequence<KaType> = withValidityAssertion {
        require(this is KaFirType)
        return coneType.getDirectSupertypes(
            analysisSession.firSession,
            ConeSupertypeCalculationMode.substitution(shouldApproximate),
        ).map { it.asKtType() }
    }

    override fun KaType.allSupertypes(shouldApproximate: Boolean): Sequence<KaType> = withValidityAssertion {
        require(this is KaFirType)
        return coneType.getAllStrictSupertypes(
            analysisSession.firSession,
            ConeSupertypeCalculationMode.substitution(shouldApproximate),
        ).map { it.asKtType() }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override val KaCallableSymbol.dispatchReceiverType: KaType?
        get() = withValidityAssertion {
            when (this) {
                is KaReceiverParameterSymbol -> null
                else -> {
                    require(this is KaFirSymbol<*>)
                    val firSymbol = firSymbol
                    check(firSymbol is FirCallableSymbol<*>) {
                        "Fir declaration should be FirCallableDeclaration; instead it was ${firSymbol::class}"
                    }
                    return firSymbol.dispatchReceiverType(analysisSession.firSymbolBuilder)
                }
            }
        }

    override val KaType.arrayElementType: KaType?
        get() = withValidityAssertion {
            require(this is KaFirType)
            return coneType.arrayElementType()?.asKtType()
        }

}

