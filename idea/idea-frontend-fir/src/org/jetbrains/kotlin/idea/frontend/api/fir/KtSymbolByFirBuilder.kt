/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import com.google.common.collect.MapMaker
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.getSymbolByLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.*
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.fir.types.*
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.threadLocal
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.util.concurrent.ConcurrentMap

/**
 * Maps FirElement to KtSymbol & ConeType to KtType, thread safe
 */
internal class KtSymbolByFirBuilder private constructor(
    private val project: Project,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    val withReadOnlyCaching: Boolean,
    private val symbolsCache: BuilderCache<FirDeclaration, KtSymbol>,
    private val typesCache: BuilderCache<ConeKotlinType, KtType>
) : ValidityTokenOwner {
    private val typeCheckerContext by threadLocal {
        ConeTypeCheckerContext(
            isErrorTypeEqualsToAnything = true,
            isStubTypeEqualsToAnything = true,
            resolveState.currentModuleSourcesSession
        )
    }

    private val firProvider get() = resolveState.currentModuleSourcesSession.firSymbolProvider

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
        typesCache = BuilderCache()
    )

    private val resolveState by weakRef(resolveState)

    fun createReadOnlyCopy(newResolveState: FirModuleResolveState): KtSymbolByFirBuilder {
        check(!withReadOnlyCaching) { "Cannot create readOnly KtSymbolByFirBuilder from a readonly one" }
        return KtSymbolByFirBuilder(
            project,
            token = token,
            resolveState = newResolveState,
            withReadOnlyCaching = true,
            symbolsCache = symbolsCache.createReadOnlyCopy(),
            typesCache = typesCache.createReadOnlyCopy()
        )
    }


    fun buildSymbol(fir: FirDeclaration): KtSymbol = symbolsCache.cache(fir) {
        when (fir) {
            is FirRegularClass -> buildClassSymbol(fir)
            is FirSimpleFunction -> buildFunctionSymbol(fir)
            is FirProperty -> buildVariableSymbol(fir)
            is FirValueParameter -> buildParameterSymbol(fir)
            is FirConstructor -> buildConstructorSymbol(fir)
            is FirTypeParameter -> buildTypeParameterSymbol(fir)
            is FirTypeAlias -> buildTypeAliasSymbol(fir)
            is FirEnumEntry -> buildEnumEntrySymbol(fir)
            is FirField -> buildFieldSymbol(fir)
            is FirAnonymousFunction -> buildAnonymousFunctionSymbol(fir)
            is FirPropertyAccessor -> buildPropertyAccessorSymbol(fir)
            else ->
                TODO(fir::class.toString())
        }
    }

    // TODO Handle all relevant cases
    fun buildCallableSymbol(fir: FirCallableDeclaration<*>): KtCallableSymbol = buildSymbol(fir) as KtCallableSymbol

    fun buildClassLikeSymbol(fir: FirClassLikeDeclaration<*>): KtClassLikeSymbol = when (fir) {
        is FirRegularClass -> buildClassSymbol(fir)
        is FirTypeAlias -> buildTypeAliasSymbol(fir)
        else ->
            TODO(fir::class.toString())
    }

    fun buildClassifierSymbol(firSymbol: FirClassifierSymbol<*>): KtClassifierSymbol = when (val fir = firSymbol.fir) {
        is FirClassLikeDeclaration -> buildClassLikeSymbol(fir)
        is FirTypeParameter -> buildTypeParameterSymbol(fir)
        else ->
            TODO(fir::class.toString())
    }

    fun buildClassSymbol(fir: FirRegularClass) = symbolsCache.cache(fir) { KtFirClassOrObjectSymbol(fir, resolveState, token, this) }

    // TODO it can be a constructor parameter, which may be split into parameter & property
    // we should handle them both
    fun buildParameterSymbol(fir: FirValueParameter) =
        symbolsCache.cache(fir) { KtFirFunctionValueParameterSymbol(fir, resolveState, token, this) }

    fun buildFirConstructorParameter(fir: FirValueParameter) =
        symbolsCache.cache(fir) { KtFirConstructorValueParameterSymbol(fir, resolveState, token, this) }

    fun buildFirSetterParameter(fir: FirValueParameter): KtFirSetterParameterSymbol =
        symbolsCache.cache(fir) { KtFirSetterParameterSymbol(fir, resolveState, token, this) }

    fun buildFunctionSymbol(fir: FirSimpleFunction) = symbolsCache.cache(fir) {
        KtFirFunctionSymbol(fir, resolveState, token, this)
    }

    fun buildConstructorSymbol(fir: FirConstructor) = symbolsCache.cache(fir) { KtFirConstructorSymbol(fir, resolveState, token, this) }
    fun buildTypeParameterSymbol(fir: FirTypeParameter) = symbolsCache.cache(fir) { KtFirTypeParameterSymbol(fir, resolveState, token) }

    fun buildTypeAliasSymbol(fir: FirTypeAlias) = symbolsCache.cache(fir) { KtFirTypeAliasSymbol(fir, resolveState, token) }
    fun buildEnumEntrySymbol(fir: FirEnumEntry) = symbolsCache.cache(fir) { KtFirEnumEntrySymbol(fir, resolveState, token, this) }
    fun buildFieldSymbol(fir: FirField) = symbolsCache.cache(fir) { KtFirJavaFieldSymbol(fir, resolveState, token, this) }
    fun buildAnonymousFunctionSymbol(fir: FirAnonymousFunction) =
        symbolsCache.cache(fir) { KtFirAnonymousFunctionSymbol(fir, resolveState, token, this) }

    fun buildVariableSymbol(fir: FirProperty): KtVariableSymbol = symbolsCache.cache(fir) {
        when {
            fir.isLocal -> KtFirLocalVariableSymbol(fir, resolveState, token, this)
            else -> KtFirPropertySymbol(fir, resolveState, token, this)
        }
    }

    fun buildPropertyAccessorSymbol(fir: FirPropertyAccessor): KtPropertyAccessorSymbol = symbolsCache.cache(fir) {
        when {
            fir.isGetter -> KtFirPropertyGetterSymbol(fir, resolveState, token, this)
            else -> KtFirPropertySetterSymbol(fir, resolveState, token, this)
        }
    }

    fun buildClassLikeSymbolByLookupTag(lookupTag: ConeClassLikeLookupTag): KtClassLikeSymbol? = withValidityAssertion {
        firProvider.getSymbolByLookupTag(lookupTag)?.fir?.let(::buildClassLikeSymbol)
    }

    fun buildTypeParameterSymbolByLookupTag(lookupTag: ConeTypeParameterLookupTag): KtTypeParameterSymbol? = withValidityAssertion {
        (firProvider.getSymbolByLookupTag(lookupTag) as? FirTypeParameterSymbol)?.fir?.let(::buildTypeParameterSymbol)
    }

    fun buildClassLikeSymbolByClassId(classId: ClassId): FirRegularClass? = withValidityAssertion {
        firProvider.getClassLikeSymbolByFqName(classId)?.fir as? FirRegularClass
    }


    fun createPackageSymbolIfOneExists(packageFqName: FqName): KtFirPackageSymbol? {
        val exists = PackageIndexUtil.packageExists(packageFqName, GlobalSearchScope.allScope(project), project)
        if (!exists) {
            return null
        }
        return KtFirPackageSymbol(packageFqName, project, token)
    }

    fun buildTypeArgument(coneType: ConeTypeProjection): KtTypeArgument = when (coneType) {
        is ConeStarProjection -> KtStarProjectionTypeArgument
        is ConeKotlinTypeProjection -> KtFirTypeArgumentWithVariance(
            buildKtType(coneType.type),
            coneType.kind.toVariance()
        )
    }

    private fun ProjectionKind.toVariance() = when (this) {
        ProjectionKind.OUT -> KtTypeArgumentVariance.COVARIANT
        ProjectionKind.IN -> KtTypeArgumentVariance.CONTRAVARIANT
        ProjectionKind.INVARIANT -> KtTypeArgumentVariance.INVARIANT
        ProjectionKind.STAR -> error("KtStarProjectionTypeArgument be directly created")
    }


    fun buildKtType(coneType: FirTypeRef): KtType = buildKtType(coneType.coneTypeUnsafe<ConeKotlinType>())

    fun buildKtType(coneType: ConeKotlinType): KtType = typesCache.cache(coneType) {
        when (coneType) {
            is ConeClassLikeTypeImpl -> KtFirClassType(coneType, typeCheckerContext, token, this)
            is ConeTypeParameterType -> KtFirTypeParameterType(coneType, typeCheckerContext, token, this)
            is ConeClassErrorType -> KtFirErrorType(coneType, typeCheckerContext, token)
            is ConeFlexibleType -> KtFirFlexibleType(coneType, typeCheckerContext, token, this)
            is ConeIntersectionType -> KtFirIntersectionType(coneType, typeCheckerContext, token, this)
            else -> TODO()
        }
    }
}

private class BuilderCache<From, To> private constructor(
    private val cache: ConcurrentMap<From, To>,
    private val isReadOnly: Boolean
) {
    constructor() : this(cache = MapMaker().weakKeys().makeMap(), isReadOnly = false)

    fun createReadOnlyCopy(): BuilderCache<From, To> {
        check(!isReadOnly) { "Cannot create readOnly BuilderCache from a readonly one" }
        return BuilderCache(cache, isReadOnly = true)
    }

    inline fun <reified S : To> cache(key: From, calculation: () -> S): S {
        if (isReadOnly) {
            return (cache[key] ?: calculation()) as S
        }
        return cache.getOrPut(key, calculation) as S
    }
}

internal fun FirElement.buildSymbol(builder: KtSymbolByFirBuilder) =
    (this as? FirDeclaration)?.let(builder::buildSymbol)

internal fun FirDeclaration.buildSymbol(builder: KtSymbolByFirBuilder) =
    builder.buildSymbol(this)
