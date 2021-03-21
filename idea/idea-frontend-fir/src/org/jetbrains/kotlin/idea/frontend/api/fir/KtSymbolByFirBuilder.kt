/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import com.google.common.collect.MapMaker
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirFieldImpl
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.resolve.calls.originalConstructorIfTypeAlias
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.getSymbolByLookupTag
import org.jetbrains.kotlin.fir.resolve.inference.isFunctionalType
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.*
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.fir.types.*
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
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
    private val filesCache: BuilderCache<FirFile, KtFileSymbol>,
    private val backingFieldCache: BuilderCache<FirBackingFieldSymbol, KtBackingFieldSymbol>,
    private val typesCache: BuilderCache<ConeKotlinType, KtType>,
) : ValidityTokenOwner {
    private val resolveState by weakRef(resolveState)

    private val firProvider get() = resolveState.rootModuleSession.symbolProvider
    val rootSession: FirSession = resolveState.rootModuleSession

    val classifierBuilder = ClassifierSymbolBuilder()
    val functionLikeBuilder = FunctionLikeSymbolBuilder()
    val variableLikeBuilder = VariableLikeSymbolBuilder()
    val callableBuilder = CallableSymbolBuilder()
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
            typesCache = typesCache.createReadOnlyCopy(),
            filesCache = filesCache.createReadOnlyCopy(),
            backingFieldCache = backingFieldCache.createReadOnlyCopy(),
        )
    }


    fun buildSymbol(fir: FirDeclaration): KtSymbol {
        return when (fir) {
            is FirClassLikeDeclaration<*> -> classifierBuilder.buildClassLikeSymbol(fir)
            is FirTypeParameter -> classifierBuilder.buildTypeParameterSymbol(fir)
            is FirCallableDeclaration<*> -> callableBuilder.buildCallableSymbol(fir)
            else -> throwUnexpectedElementError(fir)
        }
    }


    fun buildEnumEntrySymbol(fir: FirEnumEntry) = symbolsCache.cache(fir) { KtFirEnumEntrySymbol(fir, resolveState, token, this) }


    fun buildFileSymbol(fir: FirFile) = filesCache.cache(fir) { KtFirFileSymbol(fir, resolveState, token) }


    fun createPackageSymbolIfOneExists(packageFqName: FqName): KtFirPackageSymbol? {
        val exists = PackageIndexUtil.packageExists(packageFqName, GlobalSearchScope.allScope(project), project)
        if (!exists) {
            return null
        }
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


        fun buildClassLikeSymbol(fir: FirClassLikeDeclaration<*>): KtClassLikeSymbol {
            return when (fir) {
                is FirClass<*> -> buildClassOrObjectSymbol(fir)
                is FirTypeAlias -> buildTypeAliasSymbol(fir)
                else -> throwUnexpectedElementError(fir)
            }
        }

        fun buildClassOrObjectSymbol(fir: FirClass<*>): KtClassOrObjectSymbol {
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
            return symbolsCache.cache(fir) { KtFirTypeAliasSymbol(fir, resolveState, token) }
        }

        fun buildTypeParameterSymbol(fir: FirTypeParameter): KtFirTypeParameterSymbol {
            return symbolsCache.cache(fir) { KtFirTypeParameterSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder) }
        }

        fun buildTypeParameterSymbolByLookupTag(lookupTag: ConeTypeParameterLookupTag): KtTypeParameterSymbol? {
            val firTypeParameterSymbol = firProvider.getSymbolByLookupTag(lookupTag) as? FirTypeParameterSymbol ?: return null
            return buildTypeParameterSymbol(firTypeParameterSymbol.fir)
        }

        fun buildClassLikeSymbolByClassId(classId: ClassId): KtClassLikeSymbol? {
            val firClassLikeSymbol = firProvider.getClassLikeSymbolByFqName(classId) ?: return null
            return buildClassLikeSymbol(firClassLikeSymbol.fir)
        }

        fun buildClassLikeSymbolByLookupTag(lookupTag: ConeClassLikeLookupTag): KtClassLikeSymbol? {
            val firClassLikeSymbol = firProvider.getSymbolByLookupTag(lookupTag) ?: return null
            return buildClassLikeSymbol(firClassLikeSymbol.fir)
        }
    }

    inner class FunctionLikeSymbolBuilder {
        fun buildFunctionLikeSymbol(fir: FirFunction<*>): KtFunctionLikeSymbol {
            return when (fir) {
                is FirSimpleFunction -> buildFunctionSymbol(fir)
                is FirConstructor -> buildConstructorSymbol(fir)
                is FirAnonymousFunction -> buildAnonymousFunctionSymbol(fir)
                else -> throwUnexpectedElementError(fir)
            }
        }

        fun buildFunctionSymbol(fir: FirSimpleFunction): KtFirFunctionSymbol {
            return symbolsCache.cache(fir) { KtFirFunctionSymbol(fir, resolveState, token, this@KtSymbolByFirBuilder) }
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
    }

    inner class VariableLikeSymbolBuilder {
        fun buildVariableLikeSymbol(fir: FirVariable<*>): KtVariableLikeSymbol {
            return when (fir) {
                is FirProperty -> buildVariableSymbol(fir)
                is FirValueParameter -> buildValueParameterSymbol(fir)
                is FirField -> buildFieldSymbol(fir)
                is FirEnumEntry -> buildEnumEntrySymbol(fir) // TODO enum entry should not be callable
                else -> throwUnexpectedElementError(fir)
            }
        }

        fun buildVariableSymbol(fir: FirProperty): KtVariableSymbol {
            return when {
                fir.isLocal -> buildLocalVariableSymbol(fir)
                fir is FirSyntheticProperty -> buildSyntheticJavaPropertySymbol(fir)
                else -> buildPropertySymbol(fir)
            }
        }

        fun buildPropertySymbol(fir: FirProperty): KtKotlinPropertySymbol {
            checkRequirementForBuildingSymbol<KtKotlinPropertySymbol>(fir, !fir.isLocal)
            checkRequirementForBuildingSymbol<KtKotlinPropertySymbol>(fir, fir !is FirSyntheticProperty)
            return symbolsCache.cache(fir) {
                KtFirKotlinPropertySymbol(fir, resolveState, token, this@KtSymbolByFirBuilder)
            }
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

        fun buildBackingFieldSymbol(fir: FirBackingFieldSymbol): KtFirBackingFieldSymbol {
            return backingFieldCache.cache(fir) { KtFirBackingFieldSymbol(fir.fir, resolveState, token, this@KtSymbolByFirBuilder) }
        }

        fun buildBackingFieldSymbolByProperty(fir: FirProperty): KtFirBackingFieldSymbol {
            val backingFieldSymbol = fir.backingFieldSymbol
            return buildBackingFieldSymbol(backingFieldSymbol)
        }

        private fun FirField.isJavaFieldOrSubstitutionOverrideOfJavaField(): Boolean = when (this) {
            is FirJavaField -> true
            is FirFieldImpl -> (this as FirField).originalForSubstitutionOverride?.isJavaFieldOrSubstitutionOverrideOfJavaField() == true
            else -> throwUnexpectedElementError(this)
        }
    }

    inner class CallableSymbolBuilder {
        fun buildCallableSymbol(fir: FirCallableDeclaration<*>): KtCallableSymbol {
            return when (fir) {
                is FirPropertyAccessor -> buildPropertyAccessorSymbol(fir)
                is FirFunction<*> -> functionLikeBuilder.buildFunctionLikeSymbol(fir)
                is FirVariable<*> -> variableLikeBuilder.buildVariableLikeSymbol(fir)
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
    }

    inner class TypeBuilder {
        fun buildKtType(coneType: ConeKotlinType): KtType {
            return typesCache.cache(coneType) {
                when (coneType) {
                    is ConeClassLikeTypeImpl -> {
                        if (coneType.isFunctionalType(rootSession)) KtFirFunctionalType(coneType, token, this@KtSymbolByFirBuilder)
                        else KtFirUsualClassType(coneType, token, this@KtSymbolByFirBuilder)
                    }
                    is ConeTypeParameterType -> KtFirTypeParameterType(coneType, token, this@KtSymbolByFirBuilder)
                    is ConeClassErrorType -> KtFirErrorType(coneType, token)
                    is ConeFlexibleType -> KtFirFlexibleType(coneType, token, this@KtSymbolByFirBuilder)
                    is ConeIntersectionType -> KtFirIntersectionType(coneType, token, this@KtSymbolByFirBuilder)
                    is ConeDefinitelyNotNullType -> buildKtType(coneType.original)
                    else -> throwUnexpectedElementError(coneType)
                }
            }
        }

        fun buildKtType(coneType: FirTypeRef): KtType {
            return buildKtType(coneType.coneType)
        }

        fun buildTypeArgument(coneType: ConeTypeProjection): KtTypeArgument = when (coneType) {
            is ConeStarProjection -> KtStarProjectionTypeArgument
            is ConeKotlinTypeProjection -> KtFirTypeArgumentWithVariance(
                buildKtType(coneType.type),
                coneType.kind.toVariance()
            )
        }

        private fun ProjectionKind.toVariance(): Variance = when (this) {
            ProjectionKind.OUT -> Variance.OUT_VARIANCE
            ProjectionKind.IN -> Variance.IN_VARIANCE
            ProjectionKind.INVARIANT -> Variance.INVARIANT
            ProjectionKind.STAR -> error("KtStarProjectionTypeArgument should not be directly created")
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


private class BuilderCache<From, To: Any> private constructor(
    private val cache: ConcurrentMap<From, To>,
    private val isReadOnly: Boolean
) {
    constructor() : this(cache = MapMaker().weakKeys().makeMap(), isReadOnly = false)

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
