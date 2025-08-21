/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.fir.signatures.KaFirFunctionSubstitutorBasedSignature
import org.jetbrains.kotlin.analysis.api.fir.signatures.KaFirVariableSubstitutorBasedSignature
import org.jetbrains.kotlin.analysis.api.fir.symbols.*
import org.jetbrains.kotlin.analysis.api.fir.types.*
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.toVariance
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseStarTypeProjection
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirFieldImpl
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
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectOrStaticData
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Maps FirElement to KaSymbol & ConeType to KaType, thread safe
 */
internal class KaSymbolByFirBuilder(
    private val project: Project,
    val analysisSession: KaFirSession,
    val token: KaLifetimeToken,
) {
    private val resolutionFacade: LLResolutionFacade get() = analysisSession.resolutionFacade
    private val firProvider: FirSymbolProvider get() = rootSession.symbolProvider
    val rootSession: LLFirSession get() = resolutionFacade.useSiteFirSession

    val classifierBuilder = ClassifierSymbolBuilder()
    val functionBuilder = FunctionSymbolBuilder()
    val variableBuilder = VariableSymbolBuilder()
    val callableBuilder = CallableSymbolBuilder()
    val typeBuilder = TypeBuilder()

    fun buildSymbol(fir: FirDeclaration): KaSymbol = buildSymbol(fir.symbol)
    fun buildSymbol(firSymbol: FirBasedSymbol<*>): KaSymbol = when (firSymbol) {
        is FirClassLikeSymbol<*> -> classifierBuilder.buildClassLikeSymbol(firSymbol)
        is FirTypeParameterSymbol -> classifierBuilder.buildTypeParameterSymbol(firSymbol)
        is FirCallableSymbol<*> -> callableBuilder.buildCallableSymbol(firSymbol)
        is FirFileSymbol -> buildFileSymbol(firSymbol)
        is FirScriptSymbol -> buildScriptSymbol(firSymbol)
        is FirReceiverParameterSymbol -> buildReceiverParameterSymbol(firSymbol)
        else -> throwUnexpectedElementError(firSymbol)
    }

    fun buildEnumEntrySymbol(firSymbol: FirEnumEntrySymbol): KaEnumEntrySymbol = KaFirEnumEntrySymbol(firSymbol, analysisSession)

    fun buildFileSymbol(firSymbol: FirFileSymbol): KaFileSymbol = KaFirFileSymbol(firSymbol, analysisSession)

    fun buildScriptSymbol(firSymbol: FirScriptSymbol): KaScriptSymbol = KaFirScriptSymbol(firSymbol, analysisSession)

    fun buildReceiverParameterSymbol(firSymbol: FirReceiverParameterSymbol): KaDeclarationSymbol {
        val containingDeclarationSymbol = firSymbol.containingDeclarationSymbol
        return when (containingDeclarationSymbol) {
            is FirCallableSymbol -> callableBuilder.buildExtensionReceiverSymbol(firSymbol)!!
            is FirClassSymbol -> classifierBuilder.buildClassLikeSymbol(containingDeclarationSymbol)
            else -> throwUnexpectedElementError(containingDeclarationSymbol)
        }
    }

    private val packageProvider: KotlinPackageProvider get() = analysisSession.useSitePackageProvider

    fun createPackageSymbolIfOneExists(packageFqName: FqName): KaPackageSymbol? {
        val exists = packageProvider.doesPackageExist(packageFqName, analysisSession.targetPlatform)
        if (!exists) {
            return null
        }

        return createPackageSymbol(packageFqName)
    }

    fun createPackageSymbol(packageFqName: FqName): KaPackageSymbol = KaFirPackageSymbol(packageFqName, project, token)

    inner class ClassifierSymbolBuilder {
        fun buildClassifierSymbol(firSymbol: FirClassifierSymbol<*>): KaClassifierSymbol = when (firSymbol) {
            is FirClassLikeSymbol<*> -> classifierBuilder.buildClassLikeSymbol(firSymbol)
            is FirTypeParameterSymbol -> buildTypeParameterSymbol(firSymbol)
        }

        fun buildClassLikeSymbol(firSymbol: FirClassLikeSymbol<*>): KaClassLikeSymbol = when (firSymbol) {
            is FirAnonymousObjectSymbol -> buildAnonymousObjectSymbol(firSymbol)
            is FirRegularClassSymbol -> buildNamedClassSymbol(firSymbol)
            is FirTypeAliasSymbol -> buildTypeAliasSymbol(firSymbol)
        }

        fun buildNamedClassSymbol(symbol: FirRegularClassSymbol): KaNamedClassSymbol {
            return KaFirNamedClassSymbol(symbol, analysisSession)
        }

        fun buildAnonymousObjectSymbol(symbol: FirAnonymousObjectSymbol): KaAnonymousObjectSymbol = when (symbol.classKind) {
            ClassKind.ENUM_ENTRY -> KaFirEnumEntryInitializerSymbol(symbol, analysisSession)
            else -> KaFirAnonymousObjectSymbol(symbol, analysisSession)
        }

        fun buildTypeAliasSymbol(symbol: FirTypeAliasSymbol): KaTypeAliasSymbol {
            return KaFirTypeAliasSymbol(symbol, analysisSession)
        }

        fun buildTypeParameterSymbol(firSymbol: FirTypeParameterSymbol): KaTypeParameterSymbol {
            val callableSymbol = firSymbol.containingDeclarationSymbol as? FirCallableSymbol<*>
            callableSymbol?.fir?.unwrapSubstitutionOverrideIfNeeded()?.let { unwrappedCallable ->
                val originalIndex = callableSymbol.typeParameterSymbols.indexOf(firSymbol)
                if (originalIndex == -1) {
                    errorWithAttachment("Containing callable doesn't have the corresponding parameter") {
                        withFirSymbolEntry("typeParameter", firSymbol)
                        withFirSymbolEntry("containingCallable", callableSymbol)
                    }
                }

                val unwrappedParameter = unwrappedCallable.symbol.typeParameterSymbols[originalIndex]
                return buildTypeParameterSymbol(unwrappedParameter)
            }

            return KaFirTypeParameterSymbol(firSymbol, analysisSession)
        }

        fun buildClassLikeSymbolByClassId(classId: ClassId): KaClassLikeSymbol? {
            val firClassLikeSymbol = firProvider.getClassLikeSymbolByClassId(classId) ?: return null
            return buildClassLikeSymbol(firClassLikeSymbol)
        }

        fun buildClassLikeSymbolByLookupTag(lookupTag: ConeClassLikeLookupTag): KaClassLikeSymbol? {
            val firClassLikeSymbol = lookupTag.toSymbol(analysisSession.firSession) ?: return null
            return buildClassLikeSymbol(firClassLikeSymbol)
        }
    }

    inner class FunctionSymbolBuilder {
        fun buildFunctionSymbol(firSymbol: FirFunctionSymbol<*>): KaFunctionSymbol = when (firSymbol) {
            is FirNamedFunctionSymbol -> {
                if (firSymbol.origin == FirDeclarationOrigin.SamConstructor) {
                    buildSamConstructorSymbol(firSymbol)
                } else {
                    buildNamedFunctionSymbol(firSymbol)
                }
            }
            is FirConstructorSymbol -> buildConstructorSymbol(firSymbol)
            is FirAnonymousFunctionSymbol -> buildAnonymousFunctionSymbol(firSymbol)
            is FirPropertyAccessorSymbol -> buildPropertyAccessorSymbol(firSymbol)
            else -> throwUnexpectedElementError(firSymbol)
        }

        fun buildFunctionSignature(fir: FirFunctionSymbol<*>): KaFunctionSignature<KaFunctionSymbol> {
            if (fir is FirNamedFunctionSymbol && fir.origin != FirDeclarationOrigin.SamConstructor)
                return buildNamedFunctionSignature(fir)

            return with(analysisSession) { buildFunctionSymbol(fir).asSignature() }
        }

        fun buildNamedFunctionSymbol(firSymbol: FirNamedFunctionSymbol): KaNamedFunctionSymbol {
            firSymbol.fir.unwrapSubstitutionOverrideIfNeeded()?.let {
                return buildNamedFunctionSymbol(it.symbol)
            }

            if (firSymbol.dispatchReceiverType?.contains { it is ConeStubType } == true) {
                return buildNamedFunctionSymbol(
                    firSymbol.originalIfFakeOverride()
                        ?: errorWithFirSpecificEntries("Stub type in real declaration", fir = firSymbol.fir)
                )
            }

            firSymbol.unwrapImportedFromObjectOrStatic(::buildNamedFunctionSymbol)?.let { return it }

            check(firSymbol.origin != FirDeclarationOrigin.SamConstructor)
            return KaFirNamedFunctionSymbol(firSymbol, analysisSession)
        }

        fun buildNamedFunctionSignature(firSymbol: FirNamedFunctionSymbol): KaFunctionSignature<KaNamedFunctionSymbol> {
            return KaFirFunctionSubstitutorBasedSignature(analysisSession.token, firSymbol, analysisSession.firSymbolBuilder)
        }

        fun buildAnonymousFunctionSymbol(firSymbol: FirAnonymousFunctionSymbol): KaAnonymousFunctionSymbol {
            return KaFirAnonymousFunctionSymbol(firSymbol, analysisSession)
        }

        fun buildConstructorSymbol(firSymbol: FirConstructorSymbol): KaConstructorSymbol {
            val unwrapped = firSymbol.fir.unwrapSubstitutionOverrideIfNeeded()?.symbol ?: firSymbol
            return KaFirConstructorSymbol(unwrapped, analysisSession)
        }

        fun buildSamConstructorSymbol(firSymbol: FirNamedFunctionSymbol): KaSamConstructorSymbol {
            check(firSymbol.origin == FirDeclarationOrigin.SamConstructor)
            return KaFirSamConstructorSymbol(firSymbol, analysisSession)
        }

        fun buildPropertyAccessorSymbol(firSymbol: FirPropertyAccessorSymbol): KaPropertyAccessorSymbol {
            val propertySymbol = variableBuilder.buildVariableSymbol(firSymbol.propertySymbol)
            requireWithAttachment(
                propertySymbol is KaPropertySymbol,
                { "Unexpected property symbol type: ${propertySymbol::class.simpleName}" },
            ) {
                withFirSymbolEntry("propertySymbol", firSymbol.propertySymbol)
            }

            val accessorSymbol = if (firSymbol.isGetter) propertySymbol.getter else propertySymbol.setter
            requireWithAttachment(
                accessorSymbol != null,
                { "Inconsistent state: property accessor is null while property symbol is not null" },
            ) {
                withFirSymbolEntry("propertySymbol", firSymbol.propertySymbol)
            }

            return accessorSymbol
        }
    }

    inner class VariableSymbolBuilder {
        fun buildVariableSymbol(firSymbol: FirVariableSymbol<*>): KaVariableSymbol = when (firSymbol) {
            is FirErrorPropertySymbol -> buildErrorVariableSymbol(firSymbol)
            is FirPropertySymbol -> when {
                firSymbol.isLocal -> buildLocalVariableSymbol(firSymbol)
                firSymbol is FirSyntheticPropertySymbol -> buildSyntheticJavaPropertySymbol(firSymbol)
                else -> buildPropertySymbol(firSymbol)
            }
            is FirValueParameterSymbol -> buildParameterSymbol(firSymbol)
            is FirFieldSymbol -> buildFieldSymbol(firSymbol)
            is FirEnumEntrySymbol -> buildEnumEntrySymbol(firSymbol) // TODO enum entry should not be callable
            is FirBackingFieldSymbol -> buildBackingFieldSymbol(firSymbol)
            is FirDelegateFieldSymbol -> throwUnexpectedElementError(firSymbol)
        }

        fun buildVariableLikeSignature(firSymbol: FirVariableSymbol<*>): KaVariableSignature<KaVariableSymbol> {
            if (firSymbol is FirPropertySymbol && !firSymbol.isLocal && firSymbol !is FirSyntheticPropertySymbol) {
                return buildPropertySignature(firSymbol)
            }

            return with(analysisSession) { buildVariableSymbol(firSymbol).asSignature() }
        }

        fun buildPropertySymbol(firSymbol: FirPropertySymbol): KaVariableSymbol {
            checkRequirementForBuildingSymbol<KaKotlinPropertySymbol>(firSymbol, !firSymbol.isLocal)
            checkRequirementForBuildingSymbol<KaKotlinPropertySymbol>(firSymbol, firSymbol !is FirSyntheticPropertySymbol)

            firSymbol.fir.unwrapSubstitutionOverrideIfNeeded()?.let {
                return buildVariableSymbol(it.symbol)
            }

            firSymbol.unwrapImportedFromObjectOrStatic(::buildPropertySymbol)?.let { return it }

            return KaFirKotlinPropertySymbol.create(firSymbol, analysisSession)
        }

        fun buildPropertySignature(firSymbol: FirPropertySymbol): KaVariableSignature<KaVariableSymbol> {
            firSymbol.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
            return KaFirVariableSubstitutorBasedSignature(analysisSession.token, firSymbol, analysisSession.firSymbolBuilder)
        }

        fun buildLocalVariableSymbol(firSymbol: FirPropertySymbol): KaLocalVariableSymbol {
            return KaFirLocalVariableSymbol(firSymbol, analysisSession)
        }

        fun buildErrorVariableSymbol(firSymbol: FirErrorPropertySymbol): KaLocalVariableSymbol {
            return KaFirErrorVariableSymbol(firSymbol, analysisSession)
        }

        fun buildSyntheticJavaPropertySymbol(firSymbol: FirSyntheticPropertySymbol): KaSyntheticJavaPropertySymbol {
            return KaFirSyntheticJavaPropertySymbol(firSymbol, analysisSession)
        }

        fun buildParameterSymbol(firSymbol: FirValueParameterSymbol): KaParameterSymbol = when (firSymbol.fir.valueParameterKind) {
            FirValueParameterKind.Regular -> buildValueParameterSymbol(firSymbol)
            FirValueParameterKind.ContextParameter, FirValueParameterKind.LegacyContextReceiver -> buildContextParameterSymbol(firSymbol)
        }

        fun buildValueParameterSymbol(firSymbol: FirValueParameterSymbol): KaValueParameterSymbol {
            requireWithAttachment(
                firSymbol.fir.valueParameterKind == FirValueParameterKind.Regular,
                { "${FirValueParameterKind.Regular} is expected, but found ${firSymbol.fir.valueParameterKind}" },
            ) {
                withFirSymbolEntry("symbol", firSymbol)
            }


            val functionSymbol = firSymbol.containingDeclarationSymbol

            (functionSymbol as? FirFunctionSymbol)?.fir?.unwrapSubstitutionOverrideIfNeeded()?.let { unwrappedFunction ->
                val originalIndex = functionSymbol.valueParameterSymbols.indexOf(firSymbol)
                if (originalIndex == -1) {
                    errorWithAttachment("Containing function doesn't have the corresponding parameter") {
                        withFirSymbolEntry("valueParameter", firSymbol)
                        withFirSymbolEntry("function", functionSymbol)
                    }
                }

                val unwrappedParameter = unwrappedFunction.symbol.valueParameterSymbols[originalIndex]
                return buildValueParameterSymbol(unwrappedParameter)
            }

            return when (functionSymbol) {
                is FirPropertyAccessorSymbol if functionSymbol.fir is FirDefaultPropertyAccessor -> {
                    val owner = functionBuilder.buildPropertyAccessorSymbol(functionSymbol)
                    requireWithAttachment(
                        owner is KaFirDefaultPropertySetterSymbol,
                        { "Unexpected owner type: ${owner::class.simpleName}" }
                    ) {
                        withFirSymbolEntry("function", functionSymbol)
                    }

                    KaFirDefaultSetterValueParameter(owner)
                }

                else -> KaFirValueParameterSymbol(firSymbol, analysisSession)
            }
        }

        fun buildContextParameterSymbol(firSymbol: FirValueParameterSymbol): KaContextParameterSymbol {
            requireWithAttachment(
                firSymbol.fir.valueParameterKind != FirValueParameterKind.Regular,
                { "${FirValueParameterKind.Regular} is not expected" },
            ) {
                withFirSymbolEntry("symbol", firSymbol)
            }

            val callableSymbol = firSymbol.containingDeclarationSymbol as? FirCallableSymbol<*>
            callableSymbol?.fir?.unwrapSubstitutionOverrideIfNeeded()?.let { unwrappedCallable ->
                val originalIndex = callableSymbol.fir.contextParameters.indexOf(firSymbol.fir)
                if (originalIndex == -1) {
                    errorWithAttachment("Containing callable doesn't have the corresponding parameter") {
                        withFirSymbolEntry("contextParameter", firSymbol)
                        withFirSymbolEntry("callable", callableSymbol)
                    }
                }

                val unwrappedParameter = unwrappedCallable.contextParameters[originalIndex]
                return buildContextParameterSymbol(unwrappedParameter.symbol)
            }

            return KaFirContextParameterSymbol(firSymbol, analysisSession)
        }

        fun buildFieldSymbol(firSymbol: FirFieldSymbol): KaJavaFieldSymbol {
            firSymbol.unwrapImportedFromObjectOrStatic(::buildFieldSymbol)?.let { return it }
            firSymbol.fir.unwrapSubstitutionOverrideIfNeeded()?.let { return buildFieldSymbol(it.symbol) }

            checkRequirementForBuildingSymbol<KaFirJavaFieldSymbol>(
                firSymbol,
                firSymbol.fir.isJavaFieldOrFakeOverrideOfJavaField()
            )
            return KaFirJavaFieldSymbol(firSymbol, analysisSession)
        }

        fun buildBackingFieldSymbol(firSymbol: FirBackingFieldSymbol): KaBackingFieldSymbol {
            val propertySymbol = buildPropertySymbol(firSymbol.propertySymbol)
            requireWithAttachment(
                propertySymbol is KaPropertySymbol,
                { "Inconsistent state: property symbol is not a ${KaPropertySymbol::class.simpleName}" },
            ) {
                withFirSymbolEntry("property", propertySymbol.firSymbol)
                withFirSymbolEntry("backingField", firSymbol)
            }

            val backingFieldSymbol = propertySymbol.backingFieldSymbol
            requireWithAttachment(
                backingFieldSymbol != null,
                { "Inconsistent state: backing field symbol is null" },
            ) {
                withFirSymbolEntry("property", propertySymbol.firSymbol)
                withFirSymbolEntry("backingField", firSymbol)
            }

            return backingFieldSymbol
        }

        private fun FirField.isJavaFieldOrFakeOverrideOfJavaField(): Boolean = when (this) {
            is FirJavaField -> true
            is FirFieldImpl -> {
                // KT-75894: not just type substitution, but intersection is possible too.
                (this as FirField).originalIfFakeOverride()?.isJavaFieldOrFakeOverrideOfJavaField() == true
            }
            else -> throwUnexpectedElementError(this)
        }
    }

    inner class CallableSymbolBuilder {
        fun buildCallableSymbol(firSymbol: FirCallableSymbol<*>): KaCallableSymbol = when (firSymbol) {
            is FirPropertyAccessorSymbol -> functionBuilder.buildPropertyAccessorSymbol(firSymbol)
            is FirFunctionSymbol<*> -> functionBuilder.buildFunctionSymbol(firSymbol)
            is FirVariableSymbol<*> -> variableBuilder.buildVariableSymbol(firSymbol)
            else -> throwUnexpectedElementError(firSymbol)
        }

        fun buildCallableSignature(firSymbol: FirCallableSymbol<*>): KaCallableSignature<KaCallableSymbol> = when (firSymbol) {
            is FirPropertyAccessorSymbol -> with(analysisSession) { functionBuilder.buildPropertyAccessorSymbol(firSymbol).asSignature() }
            is FirFunctionSymbol<*> -> functionBuilder.buildFunctionSignature(firSymbol)
            is FirVariableSymbol<*> -> variableBuilder.buildVariableLikeSignature(firSymbol)
            else -> throwUnexpectedElementError(firSymbol)
        }


        fun buildExtensionReceiverSymbol(firSymbol: FirReceiverParameterSymbol): KaReceiverParameterSymbol? {
            val referencedSymbol = firSymbol.containingDeclarationSymbol
            if (referencedSymbol is FirCallableSymbol && referencedSymbol.fir.receiverParameter != null) {
                return buildCallableSymbol(referencedSymbol).receiverParameter
            }
            return null
        }
    }

    inner class TypeBuilder {
        fun buildKtType(coneType: ConeKotlinType): KaType = when (coneType) {
            is ConeClassLikeTypeImpl -> when {
                coneType.lookupTag.toSymbol(rootSession) == null -> {
                    KaFirClassErrorType(
                        coneType = coneType,
                        coneDiagnostic = ConeUnresolvedSymbolError(coneType.lookupTag.classId),
                        builder = this@KaSymbolByFirBuilder
                    )
                }
                hasFunctionalClassId(coneType) -> KaFirFunctionType(coneType, this@KaSymbolByFirBuilder)
                else -> KaFirUsualClassType(coneType, this@KaSymbolByFirBuilder)
            }

            is ConeTypeParameterType -> KaFirTypeParameterType(coneType, this@KaSymbolByFirBuilder)
            is ConeErrorType -> when (val diagnostic = coneType.diagnostic) {
                is ConeUnresolvedError, is ConeUnmatchedTypeArgumentsError -> {
                    KaFirClassErrorType(coneType, diagnostic, this@KaSymbolByFirBuilder)
                }

                else -> KaFirErrorType(coneType, this@KaSymbolByFirBuilder)
            }

            is ConeDynamicType -> KaFirDynamicType(coneType, this@KaSymbolByFirBuilder)
            is ConeFlexibleType -> KaFirFlexibleType(coneType, this@KaSymbolByFirBuilder)
            is ConeIntersectionType -> KaFirIntersectionType(coneType, this@KaSymbolByFirBuilder)
            is ConeDefinitelyNotNullType -> KaFirDefinitelyNotNullType(coneType, this@KaSymbolByFirBuilder)
            is ConeCapturedType -> KaFirCapturedType(coneType, this@KaSymbolByFirBuilder)
            is ConeIntegerLiteralType -> buildKtType(coneType.getApproximatedType())

            is ConeTypeVariableType -> {
                val diagnostic = when (val typeParameter = coneType.typeConstructor.originalTypeParameter) {
                    null -> ConeSimpleDiagnostic("Cannot infer parameter type for ${coneType.typeConstructor.debugName}")
                    else -> ConeCannotInferTypeParameterType((typeParameter as ConeTypeParameterLookupTag).typeParameterSymbol)
                }

                buildKtType(ConeErrorType(diagnostic, isUninferredParameter = true, attributes = coneType.attributes))
            }

            else -> throwUnexpectedElementError(coneType)
        }

        private fun hasFunctionalClassId(coneType: ConeClassLikeTypeImpl): Boolean {
            // Avoid expansion of `coneType` when checking if it is a function type. Otherwise, a type alias pointing to a function type
            // will be treated as a function type itself. Then, `TypeBuilder` will build a `KaFirFunctionType` instead of a
            // `KaFirUsualClassType` to represent the type alias.
            //
            // If we have such a type alias pointing to a function type, it is most likely the abbreviation of an expanded function type. An
            // abbreviation shouldn't be expanded, and so there shouldn't be any implicit expansion here.
            return coneType.functionTypeKind(analysisSession.resolutionFacade.useSiteFirSession, expandTypeAliases = false) != null
        }

        fun buildKtType(coneType: FirTypeRef): KaType = buildKtType(coneType.coneType)

        fun buildTypeProjection(coneType: ConeTypeProjection): KaTypeProjection = when (coneType) {
            is ConeStarProjection -> KaBaseStarTypeProjection(token)
            is ConeKotlinTypeProjection -> KaBaseTypeArgumentWithVariance(
                buildKtType(coneType.type),
                coneType.kind.toVariance(),
                token,
            )
        }

        fun buildSubstitutor(substitutor: ConeSubstitutor): KaSubstitutor = when (substitutor) {
            ConeSubstitutor.Empty -> KaSubstitutor.Empty(token)
            is ConeSubstitutorByMap -> KaFirMapBackedSubstitutor(substitutor, this@KaSymbolByFirBuilder)
            is ChainedSubstitutor -> KaFirChainedSubstitutor(substitutor, this@KaSymbolByFirBuilder)
            else -> KaFirGenericSubstitutor(substitutor, this@KaSymbolByFirBuilder)
        }
    }

    /**
     * We shouldn't expose imported callables as they may have different [org.jetbrains.kotlin.name.CallableId]s
     * than the original callables.
     * Resolved FIR has explicitly declared original objects receivers instead of such synthetic callables.
     */
    private inline fun <reified T : FirCallableSymbol<*>, R> T.unwrapImportedFromObjectOrStatic(builder: (T) -> R): R? {
        return if (origin == FirDeclarationOrigin.ImportedFromObjectOrStatic) {
            val originalSymbol = fir.importedFromObjectOrStaticData!!.original.symbol
            // The symbol has to be the same type as it is just a copy with possibly different `CallableId`
            builder(originalSymbol as T)
        } else {
            null
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
        return originalDeclaration.takeIf { this.origin is FirDeclarationOrigin.SubstitutionOverride.CallSite }
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
        if (this is FirConstructor && typeAliasConstructorInfo?.typeAliasSymbol != null) {
            // we don't want to unwrap typealiased constructors
            // because they are stable from the substitution standpoint
            // and can be properly handled by KaSymbols
            return null
        }

        val containingClass = getContainingClass() ?: return null
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
            errorWithAttachment("Unexpected ${element::class.simpleName}") {
                withFirSymbolEntry("firSymbol", element)
            }
        }

        private fun throwUnexpectedElementError(element: FirElement): Nothing {
            errorWithAttachment("Unexpected ${element::class.simpleName}") {
                withFirEntry("firElement", element)
            }
        }

        private fun throwUnexpectedElementError(element: ConeKotlinType): Nothing {
            errorWithAttachment("Unexpected ${element::class.simpleName}") {
                withConeTypeEntry("coneType", element)
            }
        }

        @OptIn(ExperimentalContracts::class)
        private inline fun <reified S : KaSymbol> checkRequirementForBuildingSymbol(
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

internal fun FirElement.buildSymbol(builder: KaSymbolByFirBuilder): KaSymbol? = (this as? FirDeclaration)?.symbol?.let(builder::buildSymbol)
internal fun FirDeclaration.buildSymbol(builder: KaSymbolByFirBuilder): KaSymbol = builder.buildSymbol(symbol)
internal fun FirBasedSymbol<*>.buildSymbol(builder: KaSymbolByFirBuilder): KaSymbol = builder.buildSymbol(this)

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
            val resolvedType = resolvedTypeRef.coneType

            resolvedType.forEachType {
                if (it is ConeTypeParameterType) {
                    allUsedTypeParameters.add(it.lookupTag)
                }
            }
        }
    })

    return allUsedTypeParameters
}
