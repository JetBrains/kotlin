/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.analysis.api.KtStarProjectionTypeArgument
import org.jetbrains.kotlin.analysis.api.KtTypeArgument
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.fir.symbols.*
import org.jetbrains.kotlin.analysis.api.fir.types.*
import org.jetbrains.kotlin.analysis.api.fir.utils.weakRef
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirFieldImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.resolve.getSymbolByLookupTag
import org.jetbrains.kotlin.fir.resolve.originalConstructorIfTypeAlias
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance
import java.util.concurrent.ConcurrentMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Maps FirElement to KtSymbol & ConeType to KtType, thread safe
 */
internal class KtSymbolByFirBuilder private constructor(
    private val project: Project,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    val withReadOnlyCaching: Boolean,
    private val symbolsCache: BuilderCache<FirDeclaration, KtSymbol>,
    private val extensionReceiverSymbolsCache: BuilderCache<FirCallableDeclaration, KtSymbol>,
    private val filesCache: BuilderCache<FirFile, KtFileSymbol>,
    private val backingFieldCache: BuilderCache<FirBackingField, KtBackingFieldSymbol>,
    private val typesCache: BuilderCache<ConeKotlinType, KtType>,
) : ValidityTokenOwner {
    private val resolveState by weakRef(resolveState)

    private val firProvider get() = resolveState.rootModuleSession.symbolProvider
    val rootSession: FirSession = resolveState.rootModuleSession

    val classifierBuilder = ClassifierSymbolBuilder()
    val functionLikeBuilder = FunctionLikeSymbolBuilder()
    val variableLikeBuilder = VariableLikeSymbolBuilder()
    val callableBuilder = CallableSymbolBuilder()
    val anonymousInitializerBuilder = AnonymousInitializerBuilder()
    val typeBuilder = TypeBuilder()

    constructor(
        resolveState: FirModuleResolveState,
        project: Project,
        token: ValidityToken
    ) : this(
        project = project,
        token = token,
        resolveState = resolveState,
        withReadOnlyCaching = false,
        symbolsCache = BuilderCache(),
        extensionReceiverSymbolsCache = BuilderCache(),
        typesCache = BuilderCache(),
        backingFieldCache = BuilderCache(),
        filesCache = BuilderCache(),
    )

    fun createReadOnlyCopy(newResolveState: FirModuleResolveState): KtSymbolByFirBuilder {
        check(!withReadOnlyCaching) { "Cannot create readOnly KtSymbolByFirBuilder from a readonly one" }
        return KtSymbolByFirBuilder(
            project,
            token = token,
            resolveState = newResolveState,
            withReadOnlyCaching = true,
            symbolsCache = symbolsCache.createReadOnlyCopy(),
            extensionReceiverSymbolsCache = extensionReceiverSymbolsCache.createReadOnlyCopy(),
            typesCache = typesCache.createReadOnlyCopy(),
            filesCache = filesCache.createReadOnlyCopy(),
            backingFieldCache = backingFieldCache.createReadOnlyCopy(),
        )
    }

    fun buildSymbol(fir: FirDeclaration): KtSymbol {
        return when (fir) {
            is FirClassLikeDeclaration -> classifierBuilder.buildClassLikeSymbol(fir)
            is FirTypeParameter -> classifierBuilder.buildTypeParameterSymbol(fir)
            is FirCallableDeclaration -> callableBuilder.buildCallableSymbol(fir)
            else -> throwUnexpectedElementError(fir)
        }
    }

    fun buildEnumEntrySymbol(fir: FirEnumEntry) = symbolsCache.cache(fir) { KtFirEnumEntrySymbol(fir, resolveState, token, this) }

    fun buildFileSymbol(fir: FirFile) = filesCache.cache(fir) { KtFirFileSymbol(fir, resolveState, token) }

    private val packageProvider = project.createPackageProvider(GlobalSearchScope.allScope(project))//todo scope

    fun createPackageSymbolIfOneExists(packageFqName: FqName): KtFirPackageSymbol? {
        val exists =
            packageProvider.doKotlinPackageExists(packageFqName)
                    || JavaPsiFacade.getInstance(project).findPackage(packageFqName.asString()) != null
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
            return when (val fir = firSymbol.fir) {
                is FirClassLikeDeclaration -> classifierBuilder.buildClassLikeSymbol(fir)
                is FirTypeParameter -> buildTypeParameterSymbol(fir)
                else -> throwUnexpectedElementError(fir)
            }
        }


        fun buildClassLikeSymbol(fir: FirClassLikeDeclaration): KtClassLikeSymbol {
            return when (fir) {
                is FirClass -> buildClassOrObjectSymbol(fir)
                is FirTypeAlias -> buildTypeAliasSymbol(fir)
                else -> throwUnexpectedElementError(fir)
            }
        }

        fun buildClassOrObjectSymbol(fir: FirClass): KtClassOrObjectSymbol {
            return when (fir) {
                is FirAnonymousObject -> buildAnonymousObjectSymbol(fir)
                is FirRegularClass -> buildNamedClassOrObjectSymbol(fir)
                else -> throwUnexpectedElementError(fir)
            }
        }

        fun buildNamedClassOrObjectSymbol(fir: FirRegularClass): KtFirNamedClassOrObjectSymbol {
            return symbolsCache.cache(fir) { KtFirNamedClassOrObjectSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder) }
        }

        fun buildAnonymousObjectSymbol(fir: FirAnonymousObject): KtAnonymousObjectSymbol {
            return symbolsCache.cache(fir) { KtFirAnonymousObjectSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder) }
        }

        fun buildTypeAliasSymbol(fir: FirTypeAlias): KtFirTypeAliasSymbol {
            return symbolsCache.cache(fir) { KtFirTypeAliasSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder) }
        }

        fun buildTypeParameterSymbol(fir: FirTypeParameter): KtFirTypeParameterSymbol {
            return symbolsCache.cache(fir) { KtFirTypeParameterSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder) }
        }

        fun buildTypeParameterSymbolByLookupTag(lookupTag: ConeTypeParameterLookupTag): KtTypeParameterSymbol? {
            val firTypeParameterSymbol = firProvider.getSymbolByLookupTag(lookupTag) as? FirTypeParameterSymbol ?: return null
            return buildTypeParameterSymbol(firTypeParameterSymbol.fir)
        }

        fun buildClassLikeSymbolByClassId(classId: ClassId): KtClassLikeSymbol? {
            val firClassLikeSymbol = firProvider.getClassLikeSymbolByClassId(classId) ?: return null
            return buildClassLikeSymbol(firClassLikeSymbol.fir)
        }

        fun buildClassLikeSymbolByLookupTag(lookupTag: ConeClassLikeLookupTag): KtClassLikeSymbol? {
            val firClassLikeSymbol = firProvider.getSymbolByLookupTag(lookupTag) ?: return null
            return buildClassLikeSymbol(firClassLikeSymbol.fir)
        }
    }

    inner class FunctionLikeSymbolBuilder {
        fun buildFunctionLikeSymbol(fir: FirFunction): KtFunctionLikeSymbol {
            return when (fir) {
                is FirSimpleFunction -> {
                    if (fir.origin == FirDeclarationOrigin.SamConstructor) {
                        buildSamConstructorSymbol(fir)
                    } else {
                        buildFunctionSymbol(fir)
                    }
                }
                is FirConstructor -> buildConstructorSymbol(fir)
                is FirAnonymousFunction -> buildAnonymousFunctionSymbol(fir)
                is FirPropertyAccessor -> buildPropertyAccessorSymbol(fir)
                else -> throwUnexpectedElementError(fir)
            }
        }

        fun buildFunctionLikeSignature(fir: FirFunction): KtFunctionLikeSignature<KtFunctionLikeSymbol> {
            if (fir is FirSimpleFunction && fir.origin != FirDeclarationOrigin.SamConstructor)
                return buildFunctionSignature(fir)
            return buildFunctionLikeSymbol(fir).toSignature()
        }

        fun buildFunctionSymbol(fir: FirSimpleFunction): KtFirFunctionSymbol {
            fir.unwrapSubstitutionOverrideIfNeeded()?.let {
                return buildFunctionSymbol(it)
            }
            if (fir.dispatchReceiverType?.contains { it is ConeStubType } == true) {
                return buildFunctionSymbol(fir.originalIfFakeOverride() ?: error("Stub type in real declaration"))
            }

            check(fir.origin != FirDeclarationOrigin.SamConstructor)
            return symbolsCache.cache(fir) { KtFirFunctionSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder) }
        }

        fun buildFunctionSignature(fir: FirSimpleFunction): KtFunctionLikeSignature<KtFirFunctionSymbol> {
            fir.symbol.ensureResolved(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
            val functionSymbol = buildFunctionSymbol(fir)
            return KtFunctionLikeSignature(
                functionSymbol,
                typeBuilder.buildKtType(fir.returnTypeRef),
                fir.receiverTypeRef?.let { typeBuilder.buildKtType(it) },
                functionSymbol.valueParameters.zip(fir.valueParameters).map { (ktSymbol, fir) ->
                    var type = fir.returnTypeRef.coneType
                    if (fir.isVararg) {
                        type = type.arrayElementType() ?: type
                    }
                    KtVariableLikeSignature(ktSymbol, typeBuilder.buildKtType(type), null)
                }
            )
        }

        fun buildAnonymousFunctionSymbol(fir: FirAnonymousFunction): KtFirAnonymousFunctionSymbol {
            return symbolsCache.cache(fir) { KtFirAnonymousFunctionSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder) }
        }

        fun buildConstructorSymbol(fir: FirConstructor): KtFirConstructorSymbol {
            val originalFir = fir.originalConstructorIfTypeAlias ?: fir
            return symbolsCache.cache(originalFir) {
                KtFirConstructorSymbol(originalFir, resolveState, token, this@KtSymbolByFirBuilder)
            }
        }

        fun buildSamConstructorSymbol(fir: FirSimpleFunction): KtFirSamConstructorSymbol {
            check(fir.origin == FirDeclarationOrigin.SamConstructor)
            return symbolsCache.cache(fir) { KtFirSamConstructorSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder) }
        }

        fun buildPropertyAccessorSymbol(fir: FirPropertyAccessor): KtFunctionLikeSymbol {
            return symbolsCache.cache(fir) {
                if (fir.isGetter) {
                    KtFirPropertyGetterSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder)
                } else {
                    KtFirPropertySetterSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder)
                }
            }
        }
    }

    inner class VariableLikeSymbolBuilder {
        fun buildVariableLikeSymbol(fir: FirVariable): KtVariableLikeSymbol {
            return when (fir) {
                is FirProperty -> buildVariableSymbol(fir)
                is FirValueParameter -> buildValueParameterSymbol(fir)
                is FirField -> buildFieldSymbol(fir)
                is FirEnumEntry -> buildEnumEntrySymbol(fir) // TODO enum entry should not be callable
                is FirBackingField -> buildBackingFieldSymbol(fir)

                is FirErrorProperty -> throwUnexpectedElementError(fir)
            }
        }

        fun buildVariableLikeSignature(fir: FirVariable): KtVariableLikeSignature<KtVariableLikeSymbol> {
            if (fir is FirProperty && !fir.isLocal && fir !is FirSyntheticProperty) return buildPropertySignature(fir)
            return buildVariableLikeSymbol(fir).toSignature()
        }

        fun buildVariableSymbol(fir: FirProperty): KtVariableSymbol {
            return when {
                fir.isLocal -> buildLocalVariableSymbol(fir)
                fir is FirSyntheticProperty -> buildSyntheticJavaPropertySymbol(fir)
                else -> buildPropertySymbol(fir)
            }
        }

        fun buildPropertySymbol(fir: FirProperty): KtVariableSymbol {
            checkRequirementForBuildingSymbol<KtKotlinPropertySymbol>(fir, !fir.isLocal)
            checkRequirementForBuildingSymbol<KtKotlinPropertySymbol>(fir, fir !is FirSyntheticProperty)

            fir.unwrapSubstitutionOverrideIfNeeded()?.let {
                return buildVariableSymbol(it)
            }

            return symbolsCache.cache(fir) {
                KtFirKotlinPropertySymbol(fir, resolveState, token, this@KtSymbolByFirBuilder)
            }
        }

        fun buildPropertySignature(fir: FirProperty): KtVariableLikeSignature<KtVariableSymbol> {
            fir.symbol.ensureResolved(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
            return KtVariableLikeSignature(
                buildPropertySymbol(fir),
                typeBuilder.buildKtType(fir.returnTypeRef),
                fir.receiverTypeRef?.let { typeBuilder.buildKtType(it) }
            )
        }

        fun buildLocalVariableSymbol(fir: FirProperty): KtFirLocalVariableSymbol {
            checkRequirementForBuildingSymbol<KtFirLocalVariableSymbol>(fir, fir.isLocal)
            return symbolsCache.cache(fir) {
                KtFirLocalVariableSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder)
            }
        }

        fun buildSyntheticJavaPropertySymbol(fir: FirSyntheticProperty): KtFirSyntheticJavaPropertySymbol {
            return symbolsCache.cache(fir) {
                KtFirSyntheticJavaPropertySymbol(fir, resolveState, token, this@KtSymbolByFirBuilder)
            }
        }

        fun buildValueParameterSymbol(fir: FirValueParameter): KtValueParameterSymbol {
            return symbolsCache.cache(fir) {
                KtFirValueParameterSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder)
            }
        }


        fun buildFieldSymbol(fir: FirField): KtFirJavaFieldSymbol {
            checkRequirementForBuildingSymbol<KtFirJavaFieldSymbol>(fir, fir.isJavaFieldOrSubstitutionOverrideOfJavaField())
            return symbolsCache.cache(fir) { KtFirJavaFieldSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder) }
        }

        fun buildBackingFieldSymbol(fir: FirBackingField): KtFirBackingFieldSymbol {
            return backingFieldCache.cache(fir) {
                KtFirBackingFieldSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder)
            }
        }

        fun buildBackingFieldSymbolByProperty(fir: FirProperty): KtFirBackingFieldSymbol {
            val backingFieldSymbol = fir.backingField
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
        fun buildCallableSymbol(fir: FirCallableDeclaration): KtCallableSymbol {
            return when (fir) {
                is FirPropertyAccessor -> buildPropertyAccessorSymbol(fir)
                is FirFunction -> functionLikeBuilder.buildFunctionLikeSymbol(fir)
                is FirVariable -> variableLikeBuilder.buildVariableLikeSymbol(fir)
                else -> throwUnexpectedElementError(fir)
            }
        }

        fun buildCallableSignature(fir: FirCallableDeclaration): KtSignature<KtCallableSymbol> {
            return when (fir) {
                is FirPropertyAccessor -> buildPropertyAccessorSymbol(fir).toSignature()
                is FirFunction -> functionLikeBuilder.buildFunctionLikeSignature(fir)
                is FirVariable -> variableLikeBuilder.buildVariableLikeSignature(fir)
                else -> throwUnexpectedElementError(fir)
            }
        }


        fun buildPropertyAccessorSymbol(fir: FirPropertyAccessor): KtPropertyAccessorSymbol {
            return when {
                fir.isGetter -> buildGetterSymbol(fir)
                else -> buildSetterSymbol(fir)
            }
        }

        fun buildGetterSymbol(fir: FirPropertyAccessor): KtFirPropertyGetterSymbol {
            checkRequirementForBuildingSymbol<KtFirPropertyGetterSymbol>(fir, fir.isGetter)
            return symbolsCache.cache(fir) { KtFirPropertyGetterSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder) }
        }

        fun buildSetterSymbol(fir: FirPropertyAccessor): KtFirPropertySetterSymbol {
            checkRequirementForBuildingSymbol<KtFirPropertySetterSymbol>(fir, fir.isSetter)
            return symbolsCache.cache(fir) { KtFirPropertySetterSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder) }
        }

        fun buildExtensionReceiverSymbol(fir: FirCallableDeclaration): KtReceiverParameterSymbol? {
            if (fir.receiverTypeRef == null) return null
            return extensionReceiverSymbolsCache.cache(fir) {
                KtFirReceiverParameterSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder)
            }
        }
    }

    inner class AnonymousInitializerBuilder {
        fun buildClassInitializer(fir: FirAnonymousInitializer): KtClassInitializerSymbol {
            return symbolsCache.cache(fir) { KtFirClassInitializerSymbol(fir, resolveState, token) }
        }
    }

    inner class TypeBuilder {
        fun buildKtType(coneType: ConeKotlinType): KtType {
            return typesCache.cache(coneType) {
                when (coneType) {
                    is ConeClassLikeTypeImpl -> {
                        if (hasFunctionalClassId(coneType)) KtFirFunctionalType(coneType, token, this@KtSymbolByFirBuilder)
                        else KtFirUsualClassType(coneType, token, this@KtSymbolByFirBuilder)
                    }
                    is ConeTypeParameterType -> KtFirTypeParameterType(coneType, token, this@KtSymbolByFirBuilder)
                    is ConeClassErrorType -> KtFirClassErrorType(coneType, token, this@KtSymbolByFirBuilder)
                    is ConeFlexibleType -> KtFirFlexibleType(coneType, token, this@KtSymbolByFirBuilder)
                    is ConeIntersectionType -> KtFirIntersectionType(coneType, token, this@KtSymbolByFirBuilder)
                    is ConeDefinitelyNotNullType -> KtFirDefinitelyNotNullType(coneType, token, this@KtSymbolByFirBuilder)
                    is ConeCapturedType -> KtFirCapturedType(coneType, token, this@KtSymbolByFirBuilder)
                    is ConeIntegerLiteralType -> KtFirIntegerLiteralType(coneType, token, this@KtSymbolByFirBuilder)
                    else -> throwUnexpectedElementError(coneType)
                }
            }
        }

        private fun hasFunctionalClassId(coneType: ConeClassLikeTypeImpl): Boolean {
            val classId = coneType.classId ?: return false
            return FunctionClassKind.byClassNamePrefix(classId.packageFqName, classId.relativeClassName.asString()) != null
        }

        fun buildKtType(coneType: FirTypeRef): KtType {
            return buildKtType(coneType.coneType)
        }

        fun buildTypeArgument(coneType: ConeTypeProjection): KtTypeArgument = when (coneType) {
            is ConeStarProjection -> KtStarProjectionTypeArgument(token)
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
                is ConeSubstitutorByMap -> KtFirMapBackedSubstitutor(substitutor, this@KtSymbolByFirBuilder, token)
                else -> KtFirGenericSubstitutor(substitutor, this@KtSymbolByFirBuilder, token)
            }
        }
    }

    /**
     * We want to unwrap a SUBSTITUTION_OVERRIDE wrapper if it doesn't affect the declaration's signature in any way. If the signature
     * is somehow changed, then we want to keep the wrapper.
     *
     * If the declaration references only its own type parameters, or parameters from the outer declarations, then
     * we consider that it's signature will not be changed by the SUBSTITUTION_OVERRIDE, so the wrapper can be unwrapped.
     *
     * This have a few caveats when it comes to the inner classes. TODO Provide a reference to some more in-detail description of that.
     *
     * N.B. This functions lifts only a single layer of SUBSTITUTION_OVERRIDE at a time.
     *
     * @receiver A declaration that needs to be unwrapped.
     * @return An unsubstituted declaration ([originalForSubstitutionOverride]]) if it exists and if it does not have any change
     * in signature; `null` otherwise.
     */
    private inline fun <reified T : FirCallableDeclaration> T.unwrapSubstitutionOverrideIfNeeded(): T? {
        val containingClass = getContainingClass(rootSession) ?: return null
        val originalDeclaration = originalForSubstitutionOverride ?: return null

        @Suppress("RemoveExplicitTypeArguments")
        val allowedTypeParameters = buildSet<ConeTypeParameterLookupTag> {
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
        private fun throwUnexpectedElementError(element: Any): Nothing {
            error("Unexpected ${element::class.simpleName}")
        }

        @OptIn(ExperimentalContracts::class)
        private inline fun <reified S : KtSymbol> checkRequirementForBuildingSymbol(
            fir: FirElement,
            requirement: Boolean,
        ) {
            contract {
                returns() implies requirement
            }
            require(requirement) {
                "Cannot build ${S::class.simpleName} for ${fir.renderWithType(FirRenderer.RenderMode.WithResolvePhases)}"
            }
        }
    }
}


private class BuilderCache<From, To : Any> private constructor(
    private val cache: ConcurrentMap<From, To>,
    private val isReadOnly: Boolean
) {
    constructor() : this(ContainerUtil.createConcurrentSoftMap(), isReadOnly = false)

    fun createReadOnlyCopy(): BuilderCache<From, To> {
        check(!isReadOnly) { "Cannot create readOnly BuilderCache from a readonly one" }
        return BuilderCache(cache, isReadOnly = true)
    }

    inline fun <reified S : To> cache(key: From, calculation: () -> S): S {
        val value = if (isReadOnly) {
            cache[key] ?: calculation()
        } else cache.getOrPut(key, calculation)
        return value as? S
            ?: error("Cannot cast ${value::class} to ${S::class}\n${DebugSymbolRenderer.render(value as KtSymbol)}")
    }
}

internal fun FirElement.buildSymbol(builder: KtSymbolByFirBuilder) =
    (this as? FirDeclaration)?.let(builder::buildSymbol)

internal fun FirDeclaration.buildSymbol(builder: KtSymbolByFirBuilder) =
    builder.buildSymbol(this)

private fun collectReferencedTypeParameters(declaration: FirCallableDeclaration): Set<ConeTypeParameterLookupTag> {
    val allUsedTypeParameters = mutableSetOf<ConeTypeParameterLookupTag>()

    declaration.accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
            simpleFunction.typeParameters.forEach { it.accept(this) }

            simpleFunction.receiverTypeRef?.accept(this)
            simpleFunction.valueParameters.forEach { it.returnTypeRef.accept(this) }
            simpleFunction.returnTypeRef.accept(this)
        }

        override fun visitProperty(property: FirProperty) {
            property.typeParameters.forEach { it.accept(this) }

            property.receiverTypeRef?.accept(this)
            property.returnTypeRef.accept(this)
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
