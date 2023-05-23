/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.analysis.api.KtStarTypeProjection
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.fir.symbols.*
import org.jetbrains.kotlin.analysis.api.fir.types.*
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.signatures.KtCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withConeTypeEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirSymbolEntry
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirFieldImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferTypeParameterType
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.originalIfFakeOverride
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnmatchedTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.resolve.getSymbolByLookupTag
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.originalConstructorIfTypeAlias
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.jetbrains.kotlin.analysis.api.fir.signatures.KtFirFunctionLikeSubstitutorBasedSignature
import org.jetbrains.kotlin.analysis.api.fir.signatures.KtFirVariableLikeSubstitutorBasedSignature

/**
 * Maps FirElement to KtSymbol & ConeType to KtType, thread safe
 */
internal class KtSymbolByFirBuilder constructor(
    private val project: Project,
    private val analysisSession: KtFirAnalysisSession,
    val token: KtLifetimeToken,
) {
    private val firResolveSession: LLFirResolveSession = analysisSession.firResolveSession
    private val firProvider get() = firResolveSession.useSiteFirSession.symbolProvider
    val rootSession: FirSession = firResolveSession.useSiteFirSession

    private val symbolsCache = BuilderCache<FirBasedSymbol<*>, KtSymbol>()

    val classifierBuilder = ClassifierSymbolBuilder()
    val functionLikeBuilder = FunctionLikeSymbolBuilder()
    val variableLikeBuilder = VariableLikeSymbolBuilder()
    val callableBuilder = CallableSymbolBuilder()
    val anonymousInitializerBuilder = AnonymousInitializerBuilder()
    val typeBuilder = TypeBuilder()

    fun buildSymbol(fir: FirDeclaration): KtSymbol =
        buildSymbol(fir.symbol)

    fun buildSymbol(firSymbol: FirBasedSymbol<*>): KtSymbol {
        return when (firSymbol) {
            is FirClassLikeSymbol<*> -> classifierBuilder.buildClassLikeSymbol(firSymbol)
            is FirTypeParameterSymbol -> classifierBuilder.buildTypeParameterSymbol(firSymbol)
            is FirCallableSymbol<*> -> callableBuilder.buildCallableSymbol(firSymbol)
            is FirFileSymbol -> buildFileSymbol(firSymbol)
            is FirScriptSymbol -> buildScriptSymbol(firSymbol)
            else -> throwUnexpectedElementError(firSymbol)
        }
    }

    fun buildEnumEntrySymbol(firSymbol: FirEnumEntrySymbol) =
        symbolsCache.cache(firSymbol) { KtFirEnumEntrySymbol(firSymbol, analysisSession) }

    fun buildFileSymbol(firSymbol: FirFileSymbol) = KtFirFileSymbol(firSymbol, analysisSession)

    fun buildScriptSymbol(firSymbol: FirScriptSymbol) = KtFirScriptSymbol(firSymbol, analysisSession)

    private val packageProvider: KotlinPackageProvider get() = analysisSession.useSitePackageProvider

    fun createPackageSymbolIfOneExists(packageFqName: FqName): KtFirPackageSymbol? {
        val exists = packageProvider.doesPackageExist(packageFqName, analysisSession.targetPlatform)
        if (!exists) {
            return null
        }
        return createPackageSymbol(packageFqName)
    }

    fun createPackageSymbol(packageFqName: FqName): KtFirPackageSymbol {
        return KtFirPackageSymbol(packageFqName, project, token)
    }

    inner class ClassifierSymbolBuilder {
        fun buildClassifierSymbol(firSymbol: FirClassifierSymbol<*>): KtClassifierSymbol {
            return when (firSymbol) {
                is FirClassLikeSymbol<*> -> classifierBuilder.buildClassLikeSymbol(firSymbol)
                is FirTypeParameterSymbol -> buildTypeParameterSymbol(firSymbol)
                else -> throwUnexpectedElementError(firSymbol)
            }
        }


        fun buildClassLikeSymbol(firSymbol: FirClassLikeSymbol<*>): KtClassLikeSymbol {
            return when (firSymbol) {
                is FirClassSymbol<*> -> buildClassOrObjectSymbol(firSymbol)
                is FirTypeAliasSymbol -> buildTypeAliasSymbol(firSymbol)
                else -> throwUnexpectedElementError(firSymbol)
            }
        }

        fun buildClassOrObjectSymbol(firSymbol: FirClassSymbol<*>): KtClassOrObjectSymbol {
            return when (firSymbol) {
                is FirAnonymousObjectSymbol -> buildAnonymousObjectSymbol(firSymbol)
                is FirRegularClassSymbol -> buildNamedClassOrObjectSymbol(firSymbol)
                else -> throwUnexpectedElementError(firSymbol)
            }
        }

        fun buildNamedClassOrObjectSymbol(symbol: FirRegularClassSymbol): KtFirNamedClassOrObjectSymbol {
            return symbolsCache.cache(symbol) { KtFirNamedClassOrObjectSymbol(symbol, analysisSession) }
        }

        fun buildAnonymousObjectSymbol(symbol: FirAnonymousObjectSymbol): KtAnonymousObjectSymbol {
            return symbolsCache.cache(symbol) { KtFirAnonymousObjectSymbol(symbol, analysisSession) }
        }

        fun buildTypeAliasSymbol(symbol: FirTypeAliasSymbol): KtFirTypeAliasSymbol {
            return symbolsCache.cache(symbol) { KtFirTypeAliasSymbol(symbol, analysisSession) }
        }

        fun buildTypeParameterSymbol(firSymbol: FirTypeParameterSymbol): KtFirTypeParameterSymbol {
            return symbolsCache.cache(firSymbol) {
                KtFirTypeParameterSymbol(firSymbol, analysisSession)
            }
        }

        fun buildTypeParameterSymbolByLookupTag(lookupTag: ConeTypeParameterLookupTag): KtTypeParameterSymbol? {
            val firTypeParameterSymbol = firProvider.getSymbolByLookupTag(lookupTag) as? FirTypeParameterSymbol ?: return null
            return buildTypeParameterSymbol(firTypeParameterSymbol)
        }

        fun buildClassLikeSymbolByClassId(classId: ClassId): KtClassLikeSymbol? {
            val firClassLikeSymbol = firProvider.getClassLikeSymbolByClassId(classId) ?: return null
            return buildClassLikeSymbol(firClassLikeSymbol)
        }

        fun buildClassLikeSymbolByLookupTag(lookupTag: ConeClassLikeLookupTag): KtClassLikeSymbol? {
            val firClassLikeSymbol = firProvider.getSymbolByLookupTag(lookupTag) ?: return null
            return buildClassLikeSymbol(firClassLikeSymbol)
        }
    }

    inner class FunctionLikeSymbolBuilder {
        fun buildFunctionLikeSymbol(firSymbol: FirFunctionSymbol<*>): KtFunctionLikeSymbol {
            return when (firSymbol) {
                is FirNamedFunctionSymbol -> {
                    if (firSymbol.origin == FirDeclarationOrigin.SamConstructor) {
                        buildSamConstructorSymbol(firSymbol)
                    } else {
                        buildFunctionSymbol(firSymbol)
                    }
                }
                is FirConstructorSymbol -> buildConstructorSymbol(firSymbol)
                is FirAnonymousFunctionSymbol -> buildAnonymousFunctionSymbol(firSymbol)
                is FirPropertyAccessorSymbol -> buildPropertyAccessorSymbol(firSymbol)
                else -> throwUnexpectedElementError(firSymbol)
            }
        }

        fun buildFunctionLikeSignature(fir: FirFunctionSymbol<*>): KtFunctionLikeSignature<KtFunctionLikeSymbol> {
            if (fir is FirNamedFunctionSymbol && fir.origin != FirDeclarationOrigin.SamConstructor)
                return buildFunctionSignature(fir)
            return with(analysisSession) { buildFunctionLikeSymbol(fir).asSignature() }
        }

        fun buildFunctionSymbol(firSymbol: FirNamedFunctionSymbol): KtFirFunctionSymbol {
            firSymbol.fir.unwrapSubstitutionOverrideIfNeeded()?.let {
                return buildFunctionSymbol(it.symbol)
            }
            if (firSymbol.dispatchReceiverType?.contains { it is ConeStubType } == true) {
                return buildFunctionSymbol(
                    firSymbol.originalIfFakeOverride()
                        ?: errorWithFirSpecificEntries("Stub type in real declaration", fir = firSymbol.fir)
                )
            }

            check(firSymbol.origin != FirDeclarationOrigin.SamConstructor)
            return symbolsCache.cache(firSymbol) { KtFirFunctionSymbol(firSymbol, analysisSession) }
        }

        fun buildFunctionSignature(firSymbol: FirNamedFunctionSymbol): KtFunctionLikeSignature<KtFirFunctionSymbol> {
            firSymbol.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
            return KtFirFunctionLikeSubstitutorBasedSignature(analysisSession.token, firSymbol, analysisSession.firSymbolBuilder)
        }

        fun buildAnonymousFunctionSymbol(firSymbol: FirAnonymousFunctionSymbol): KtFirAnonymousFunctionSymbol {
            return symbolsCache.cache(firSymbol) {
                KtFirAnonymousFunctionSymbol(firSymbol, analysisSession)
            }
        }

        fun buildConstructorSymbol(firSymbol: FirConstructorSymbol): KtFirConstructorSymbol {
            val originalFirSymbol = firSymbol.fir.originalConstructorIfTypeAlias?.symbol ?: firSymbol
            val unwrapped = originalFirSymbol.originalIfFakeOverride() ?: originalFirSymbol
            return symbolsCache.cache(unwrapped) {
                KtFirConstructorSymbol(unwrapped, analysisSession)
            }
        }

        fun buildSamConstructorSymbol(firSymbol: FirNamedFunctionSymbol): KtFirSamConstructorSymbol {
            check(firSymbol.origin == FirDeclarationOrigin.SamConstructor)
            return symbolsCache.cache(firSymbol) {
                KtFirSamConstructorSymbol(firSymbol, analysisSession)
            }
        }

        fun buildPropertyAccessorSymbol(firSymbol: FirPropertyAccessorSymbol): KtFunctionLikeSymbol {
            return symbolsCache.cache(firSymbol) {
                if (firSymbol.isGetter) {
                    KtFirPropertyGetterSymbol(firSymbol, analysisSession)
                } else {
                    KtFirPropertySetterSymbol(firSymbol, analysisSession)
                }
            }
        }
    }

    inner class VariableLikeSymbolBuilder {
        fun buildVariableLikeSymbol(firSymbol: FirVariableSymbol<*>): KtVariableLikeSymbol {
            return when (firSymbol) {
                is FirPropertySymbol -> buildVariableSymbol(firSymbol)
                is FirValueParameterSymbol -> buildValueParameterSymbol(firSymbol)
                is FirFieldSymbol -> buildFieldSymbol(firSymbol)
                is FirEnumEntrySymbol -> buildEnumEntrySymbol(firSymbol) // TODO enum entry should not be callable
                is FirBackingFieldSymbol -> buildBackingFieldSymbol(firSymbol)

                is FirErrorPropertySymbol -> throwUnexpectedElementError(firSymbol)
                is FirDelegateFieldSymbol -> throwUnexpectedElementError(firSymbol)
            }
        }

        fun buildVariableLikeSignature(firSymbol: FirVariableSymbol<*>): KtVariableLikeSignature<KtVariableLikeSymbol> {
            if (firSymbol is FirPropertySymbol && !firSymbol.isLocal && firSymbol !is FirSyntheticPropertySymbol) {
                return buildPropertySignature(firSymbol)
            }
            return with(analysisSession) { buildVariableLikeSymbol(firSymbol).asSignature() }
        }

        fun buildVariableSymbol(firSymbol: FirPropertySymbol): KtVariableSymbol {
            return when {
                firSymbol.isLocal -> buildLocalVariableSymbol(firSymbol)
                firSymbol is FirSyntheticPropertySymbol -> buildSyntheticJavaPropertySymbol(firSymbol)
                else -> buildPropertySymbol(firSymbol)
            }
        }

        fun buildPropertySymbol(firSymbol: FirPropertySymbol): KtVariableSymbol {
            checkRequirementForBuildingSymbol<KtKotlinPropertySymbol>(firSymbol, !firSymbol.isLocal)
            checkRequirementForBuildingSymbol<KtKotlinPropertySymbol>(firSymbol, firSymbol !is FirSyntheticPropertySymbol)
            checkRequirementForBuildingSymbol<KtKotlinPropertySymbol>(firSymbol, firSymbol !is FirSyntheticPropertySymbol)

            firSymbol.fir.unwrapSubstitutionOverrideIfNeeded()?.let {
                return buildVariableSymbol(it.symbol)
            }

            return symbolsCache.cache(firSymbol) {
                KtFirKotlinPropertySymbol(firSymbol, analysisSession)
            }
        }

        fun buildPropertySignature(firSymbol: FirPropertySymbol): KtVariableLikeSignature<KtVariableSymbol> {
            firSymbol.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
            return KtFirVariableLikeSubstitutorBasedSignature(analysisSession.token, firSymbol, analysisSession.firSymbolBuilder)
        }

        fun buildLocalVariableSymbol(firSymbol: FirPropertySymbol): KtFirLocalVariableSymbol {
            checkRequirementForBuildingSymbol<KtFirLocalVariableSymbol>(firSymbol, firSymbol.isLocal)
            return symbolsCache.cache(firSymbol) {
                KtFirLocalVariableSymbol(firSymbol, analysisSession)
            }
        }

        fun buildErrorVariableSymbol(firSymbol: FirErrorPropertySymbol): KtFirErrorVariableSymbol {
            return symbolsCache.cache(firSymbol) {
                KtFirErrorVariableSymbol(firSymbol, analysisSession)
            }
        }

        fun buildSyntheticJavaPropertySymbol(firSymbol: FirSyntheticPropertySymbol): KtFirSyntheticJavaPropertySymbol {
            return symbolsCache.cache(firSymbol) {
                KtFirSyntheticJavaPropertySymbol(firSymbol, analysisSession)
            }
        }

        fun buildValueParameterSymbol(firSymbol: FirValueParameterSymbol): KtValueParameterSymbol {
            firSymbol.fir.unwrapSubstitutionOverrideIfNeeded()?.let {
                return buildValueParameterSymbol(it.symbol)
            }
            return symbolsCache.cache(firSymbol) {
                KtFirValueParameterSymbol(firSymbol, analysisSession)
            }
        }


        fun buildFieldSymbol(firSymbol: FirFieldSymbol): KtFirJavaFieldSymbol {
            checkRequirementForBuildingSymbol<KtFirJavaFieldSymbol>(firSymbol, firSymbol.fir.isJavaFieldOrSubstitutionOverrideOfJavaField())
            return symbolsCache.cache(firSymbol) { KtFirJavaFieldSymbol(firSymbol, analysisSession) }
        }

        fun buildBackingFieldSymbol(firSymbol: FirBackingFieldSymbol): KtFirBackingFieldSymbol {
            return KtFirBackingFieldSymbol(firSymbol, analysisSession)
        }

        fun buildBackingFieldSymbolByProperty(firSymbol: FirPropertySymbol): KtFirBackingFieldSymbol {
            val backingFieldSymbol = firSymbol.backingFieldSymbol
                ?: error("FirProperty backingField is null")
            return buildBackingFieldSymbol(backingFieldSymbol)
        }

        private fun FirField.isJavaFieldOrSubstitutionOverrideOfJavaField(): Boolean = when (this) {
            is FirJavaField -> true
            is FirFieldImpl -> (this as FirField).originalForSubstitutionOverride?.isJavaFieldOrSubstitutionOverrideOfJavaField() == true
            else -> throwUnexpectedElementError(this)
        }
    }

    inner class CallableSymbolBuilder {
        fun buildCallableSymbol(firSymbol: FirCallableSymbol<*>): KtCallableSymbol {
            return when (firSymbol) {
                is FirPropertyAccessorSymbol -> buildPropertyAccessorSymbol(firSymbol)
                is FirFunctionSymbol<*> -> functionLikeBuilder.buildFunctionLikeSymbol(firSymbol)
                is FirVariableSymbol<*> -> variableLikeBuilder.buildVariableLikeSymbol(firSymbol)
                else -> throwUnexpectedElementError(firSymbol)
            }
        }

        fun buildCallableSignature(firSymbol: FirCallableSymbol<*>): KtCallableSignature<KtCallableSymbol> {
            return when (firSymbol) {
                is FirPropertyAccessorSymbol ->  with(analysisSession) { buildPropertyAccessorSymbol(firSymbol).asSignature() }
                is FirFunctionSymbol<*> -> functionLikeBuilder.buildFunctionLikeSignature(firSymbol)
                is FirVariableSymbol<*> -> variableLikeBuilder.buildVariableLikeSignature(firSymbol)
                else -> throwUnexpectedElementError(firSymbol)
            }
        }


        fun buildPropertyAccessorSymbol(firSymbol: FirPropertyAccessorSymbol): KtPropertyAccessorSymbol {
            return when {
                firSymbol.isGetter -> buildGetterSymbol(firSymbol)
                else -> buildSetterSymbol(firSymbol)
            }
        }

        fun buildGetterSymbol(firSymbol: FirPropertyAccessorSymbol): KtFirPropertyGetterSymbol {
            checkRequirementForBuildingSymbol<KtFirPropertyGetterSymbol>(firSymbol, firSymbol.isGetter)
            return symbolsCache.cache(firSymbol) {
                KtFirPropertyGetterSymbol(firSymbol, analysisSession)
            }
        }

        fun buildSetterSymbol(firSymbol: FirPropertyAccessorSymbol): KtFirPropertySetterSymbol {
            checkRequirementForBuildingSymbol<KtFirPropertySetterSymbol>(firSymbol, firSymbol.isSetter)
            return symbolsCache.cache(firSymbol) {
                KtFirPropertySetterSymbol(firSymbol, analysisSession)
            }
        }

        fun buildBackingFieldSymbol(firSymbol: FirBackingFieldSymbol): KtFirBackingFieldSymbol {
            return symbolsCache.cache(firSymbol) {
                KtFirBackingFieldSymbol(firSymbol, analysisSession)
            }
        }

        fun buildExtensionReceiverSymbol(firCallableSymbol: FirCallableSymbol<*>): KtReceiverParameterSymbol? {
            if (firCallableSymbol.fir.receiverParameter == null) return null
            return KtFirReceiverParameterSymbol(firCallableSymbol, analysisSession)
        }
    }

    inner class AnonymousInitializerBuilder {
        fun buildClassInitializer(firSymbol: FirAnonymousInitializerSymbol): KtClassInitializerSymbol {
            return symbolsCache.cache(firSymbol) { KtFirClassInitializerSymbol(firSymbol, analysisSession) }
        }
    }

    inner class TypeBuilder {
        fun buildKtType(coneType: ConeKotlinType): KtType {
            return when (coneType) {
                is ConeClassLikeTypeImpl -> {
                    when {
                        coneType.lookupTag.toSymbol(rootSession) == null -> {
                            KtFirClassErrorType(coneType, ConeUnresolvedSymbolError(coneType.lookupTag.classId), this@KtSymbolByFirBuilder)
                        }
                        hasFunctionalClassId(coneType) -> KtFirFunctionalType(coneType, this@KtSymbolByFirBuilder)
                        else -> KtFirUsualClassType(coneType, this@KtSymbolByFirBuilder)
                    }
                }
                is ConeTypeParameterType -> KtFirTypeParameterType(coneType, this@KtSymbolByFirBuilder)
                is ConeErrorType -> when (val diagnostic = coneType.diagnostic) {
                    is ConeUnresolvedError, is ConeUnmatchedTypeArgumentsError ->
                        KtFirClassErrorType(coneType, diagnostic, this@KtSymbolByFirBuilder)
                    else -> KtFirTypeErrorType(coneType, this@KtSymbolByFirBuilder)
                }
                is ConeDynamicType -> KtFirDynamicType(coneType, this@KtSymbolByFirBuilder)
                is ConeFlexibleType -> KtFirFlexibleType(coneType, this@KtSymbolByFirBuilder)
                is ConeIntersectionType -> KtFirIntersectionType(coneType, this@KtSymbolByFirBuilder)
                is ConeDefinitelyNotNullType -> KtFirDefinitelyNotNullType(coneType, this@KtSymbolByFirBuilder)
                is ConeCapturedType -> KtFirCapturedType(coneType, this@KtSymbolByFirBuilder)
                is ConeIntegerLiteralConstantType -> KtFirIntegerLiteralType(coneType, this@KtSymbolByFirBuilder)
                is ConeIntegerConstantOperatorType -> buildKtType(coneType.getApproximatedType())
                is ConeStubTypeForChainInference -> {
                    // TODO this is a temporary hack to prevent FIR IDE from crashing on builder inference, see KT-50916
                    val typeVariable = coneType.constructor.variable as? ConeTypeParameterBasedTypeVariable
                    val typeParameterSymbol = typeVariable?.typeParameterSymbol ?: throwUnexpectedElementError(coneType)
                    val coneTypeParameterType = (typeParameterSymbol.toConeType() as ConeTypeParameterType)
                        .withNullability(coneType.nullability, rootSession.typeContext)

                    KtFirTypeParameterType(coneTypeParameterType, this@KtSymbolByFirBuilder)
                }

                is ConeTypeVariableType -> {
                    val diagnostic = when ( val typeParameter = coneType.lookupTag.originalTypeParameter) {
                        null -> ConeSimpleDiagnostic("Cannot infer parameter type for ${coneType.lookupTag.debugName}")
                        else -> ConeCannotInferTypeParameterType((typeParameter as ConeTypeParameterLookupTag).typeParameterSymbol)
                    }
                    buildKtType(ConeErrorType(diagnostic, isUninferredParameter = true, attributes = coneType.attributes))
                }
                else -> throwUnexpectedElementError(coneType)
            }
        }

        private fun hasFunctionalClassId(coneType: ConeClassLikeTypeImpl): Boolean {
            return coneType.isSomeFunctionType(analysisSession.firResolveSession.useSiteFirSession)
        }

        fun buildKtType(coneType: FirTypeRef): KtType {
            return buildKtType(coneType.coneType)
        }

        fun buildTypeProjection(coneType: ConeTypeProjection): KtTypeProjection = when (coneType) {
            is ConeStarProjection -> KtStarTypeProjection(token)
            is ConeKotlinTypeProjection -> KtTypeArgumentWithVariance(
                buildKtType(coneType.type),
                coneType.kind.toVariance(),
                token,
            )
        }

        private fun ProjectionKind.toVariance(): Variance = when (this) {
            ProjectionKind.OUT -> Variance.OUT_VARIANCE
            ProjectionKind.IN -> Variance.IN_VARIANCE
            ProjectionKind.INVARIANT -> Variance.INVARIANT
            ProjectionKind.STAR -> error("KtStarProjectionTypeArgument should not be directly created")
        }

        fun buildSubstitutor(substitutor: ConeSubstitutor): KtSubstitutor {
            if (substitutor == ConeSubstitutor.Empty) return KtSubstitutor.Empty(token)
            return when (substitutor) {
                is ConeSubstitutorByMap -> KtFirMapBackedSubstitutor(substitutor, this@KtSymbolByFirBuilder)
                else -> KtFirGenericSubstitutor(substitutor, this@KtSymbolByFirBuilder)
            }
        }
    }

    /**
     * N.B. This functions lifts only a single layer of SUBSTITUTION_OVERRIDE at a time.
     */
    private inline fun <reified T : FirCallableDeclaration> T.unwrapSubstitutionOverrideIfNeeded(): T? {
        unwrapUseSiteSubstitutionOverride()?.let { return it }

        unwrapInheritanceSubstitutionOverrideIfNeeded()?.let { return it }

        return null
    }

    /**
     * Use-site substitution override happens in situations like this:
     *
     * ```
     * interface List<A> { fun get(i: Int): A }
     *
     * fun take(list: List<String>) {
     *   list.get(10) // this call
     * }
     * ```
     *
     * In FIR, `List::get` symbol in the example will be a substitution override with a `String` instead of `A`.
     * We want to lift such substitution overrides.
     *
     * @receiver A declaration that needs to be unwrapped.
     * @return An unsubstituted declaration ([originalForSubstitutionOverride]]) if [this] is a use-site substitution override.
     */
    private inline fun <reified T : FirCallableDeclaration> T.unwrapUseSiteSubstitutionOverride(): T? {
        val originalDeclaration = originalForSubstitutionOverride ?: return null

        val containingClass = getContainingMemberOrSelf().getContainingClass(rootSession) ?: return null
        val originalContainingClass = originalDeclaration.getContainingMemberOrSelf().getContainingClass(rootSession) ?: return null

        // If substitution override does not change the containing class of the FIR declaration,
        // it is a use-site substitution override
        if (containingClass != originalContainingClass) return null

        return originalDeclaration
    }

    private fun FirCallableDeclaration.getContainingMemberOrSelf(): FirCallableDeclaration = when (this) {
        is FirValueParameter -> containingFunctionSymbol.fir
        else -> this
    }

    /**
     * We want to unwrap a SUBSTITUTION_OVERRIDE wrapper if it doesn't affect the declaration's signature in any way. If the signature
     * is somehow changed, then we want to keep the wrapper.
     *
     * Such substitute overrides happen because of inheritance.
     *
     * If the declaration references only its own type parameters, or parameters from the outer declarations, then
     * we consider that it's signature will not be changed by the SUBSTITUTION_OVERRIDE, so the wrapper can be unwrapped.
     *
     * This have a few caveats when it comes to the inner classes. TODO Provide a reference to some more in-detail description of that.
     *
     * @receiver A declaration that needs to be unwrapped.
     * @return An unsubstituted declaration ([originalForSubstitutionOverride]]) if it exists and if it does not have any change
     * in signature; `null` otherwise.
     */
    private inline fun <reified T : FirCallableDeclaration> T.unwrapInheritanceSubstitutionOverrideIfNeeded(): T? {
        val containingClass = getContainingClass(rootSession) ?: return null
        val originalDeclaration = originalForSubstitutionOverride ?: return null

        val allowedTypeParameters = buildSet {
            // declaration's own parameters
            originalDeclaration.typeParameters.mapTo(this) { it.symbol.toLookupTag() }

            // captured outer parameters
            containingClass.typeParameters.mapNotNullTo(this) {
                (it as? FirOuterClassTypeParameterRef)?.symbol?.toLookupTag()
            }
        }

        val usedTypeParameters = collectReferencedTypeParameters(originalDeclaration)

        return if (allowedTypeParameters.containsAll(usedTypeParameters)) {
            originalDeclaration
        } else {
            null
        }
    }

    companion object {
        private fun throwUnexpectedElementError(element: FirBasedSymbol<*>): Nothing {
            buildErrorWithAttachment("Unexpected ${element::class.simpleName}") {
                withFirSymbolEntry("firSymbol", element)
            }
        }

        private fun throwUnexpectedElementError(element: FirElement): Nothing {
            buildErrorWithAttachment("Unexpected ${element::class.simpleName}") {
                withFirEntry("firElement", element)
            }
        }

        private fun throwUnexpectedElementError(element: ConeKotlinType): Nothing {
            buildErrorWithAttachment("Unexpected ${element::class.simpleName}") {
                withConeTypeEntry("coneType", element)
            }
        }

        @OptIn(ExperimentalContracts::class)
        private inline fun <reified S : KtSymbol> checkRequirementForBuildingSymbol(
            firSymbol: FirBasedSymbol<*>,
            requirement: Boolean,
        ) {
            contract {
                returns() implies requirement
            }
            require(requirement) {
                val renderedSymbol = FirRenderer.withResolvePhase().renderElementWithTypeAsString(firSymbol.fir)
                "Cannot build ${S::class.simpleName} for $renderedSymbol}"
            }
        }
    }
}


private class BuilderCache<From, To : KtSymbol> {
    private val cache = ContainerUtil.createConcurrentWeakKeySoftValueMap<From, To>()

    inline fun <reified S : To> cache(key: From, calculation: () -> S): S {
        val value = cache.getOrPut(key, calculation)
        return value as? S
            ?: error("Cannot cast ${value::class} to ${S::class}\n${value}")
    }
}

internal fun FirElement.buildSymbol(builder: KtSymbolByFirBuilder) =
    (this as? FirDeclaration)?.symbol?.let(builder::buildSymbol)

internal fun FirDeclaration.buildSymbol(builder: KtSymbolByFirBuilder): KtSymbol =
    builder.buildSymbol(symbol)

internal fun FirBasedSymbol<*>.buildSymbol(builder: KtSymbolByFirBuilder): KtSymbol =
    builder.buildSymbol(this)

private fun collectReferencedTypeParameters(declaration: FirCallableDeclaration): Set<ConeTypeParameterLookupTag> {
    val allUsedTypeParameters = mutableSetOf<ConeTypeParameterLookupTag>()

    declaration.accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
            simpleFunction.typeParameters.forEach { it.accept(this) }

            simpleFunction.receiverParameter?.accept(this)
            simpleFunction.valueParameters.forEach { it.returnTypeRef.accept(this) }
            simpleFunction.returnTypeRef.accept(this)
        }

        override fun visitProperty(property: FirProperty) {
            property.typeParameters.forEach { it.accept(this) }

            property.receiverParameter?.accept(this)
            property.returnTypeRef.accept(this)
        }

        override fun visitReceiverParameter(receiverParameter: FirReceiverParameter) {
            receiverParameter.typeRef.accept(this)
        }

        override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
            super.visitResolvedTypeRef(resolvedTypeRef)

            handleTypeRef(resolvedTypeRef)
        }

        private fun handleTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
            val resolvedType = resolvedTypeRef.type

            resolvedType.forEachType {
                if (it is ConeTypeParameterType) {
                    allUsedTypeParameters.add(it.lookupTag)
                }
            }
        }
    })

    return allUsedTypeParameters
}
