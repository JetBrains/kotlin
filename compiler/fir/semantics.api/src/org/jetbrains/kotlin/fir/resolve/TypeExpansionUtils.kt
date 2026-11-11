/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.util.WeakPair
import org.jetbrains.kotlin.util.component1
import org.jetbrains.kotlin.util.component2

@RequiresOptIn(
    "When a SessionHolder is available as implicit value, " +
            "passing the session explicitly is only required when it's different from the SessionHolder's session. " +
            "The annotated overload has an unused context parameter just to differentiate between callsites that have " +
            "a SessionHolder implicit value in scope and those that don't."
)
annotation class ExplicitlyPassedSession

/**
 * Compute the recursive type-alias expansion in the given type.
 *
 * A type of an expect class, that is actualized by a typealias will be expanded to the expansion of the typealias,
 * when supplied with the session of actual.
 *
 * See `/docs/fir/k2_kmp.md`
 *
 * @param useSiteSession Session to be used for classifier lookups, see [toSymbol]
 * @return Type, that is expanded to the concrete class or union type w.r.t to the [useSiteSession] or the same instance
 * if top-level constructor is not expandable type alias
 */
fun ConeClassLikeType.fullyExpandedType(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeSimpleKotlinType? = FirTypeAlias::expandedConeTypeWithEnsuredPhase,
): ConeSimpleKotlinType {
    if (this is ConeClassLikeTypeImpl) {
        val [cachedSession, cachedExpandedType] = cachedExpandedType
        if (cachedSession === useSiteSession && cachedExpandedType != null) {
            return cachedExpandedType
        }

        val computedExpandedType = fullyExpandedTypeNoCache(useSiteSession, expandedConeType)
        this.cachedExpandedType = WeakPair(useSiteSession, computedExpandedType)
        return computedExpandedType
    }

    return fullyExpandedTypeNoCache(useSiteSession, expandedConeType)
}

context(sessionHolder: SessionHolder)
fun ConeClassLikeType.fullyExpandedType(): ConeSimpleKotlinType {
    return fullyExpandedType(sessionHolder.session, expandedConeType = FirTypeAlias::expandedConeTypeWithEnsuredPhase)
}

@ExplicitlyPassedSession
context(_: SessionHolder)
fun ConeClassLikeType.fullyExpandedType(useSiteSession: FirSession): ConeSimpleKotlinType {
    return fullyExpandedType(useSiteSession, expandedConeType = FirTypeAlias::expandedConeTypeWithEnsuredPhase)
}

fun FirTypeAlias.expandedConeTypeWithEnsuredPhase(): ConeSimpleKotlinType? {
    lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
    return expandedConeType
}

/**
 * @see fullyExpandedType (the first function in the file)
 * @return the expanded type or the same instance if top-level constructor is not expandable type alias
 */
@Suppress("SuspiciousWhenOverConeKotlinType") // Not handling intersection types seems a bit suspicious but adding it breaks some tests
fun ConeKotlinType.fullyExpandedType(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeSimpleKotlinType? = FirTypeAlias::expandedConeTypeWithEnsuredPhase,
): ConeKotlinType = when (this) {
    is ConeDynamicType -> this
    is ConeFlexibleType -> {
        mapTypesOrSelf(useSiteSession.typeContext, dropIdentity = true) {
            it.fullyExpandedType(useSiteSession, expandedConeType)
        }
    }
    is ConeClassLikeType -> fullyExpandedType(useSiteSession, expandedConeType)
    is ConeUnionType -> fullyExpandedUnionType(useSiteSession, expandedConeType)
    else -> this
}

context(sessionHolder: SessionHolder)
fun ConeKotlinType.fullyExpandedType(): ConeKotlinType {
    return fullyExpandedType(sessionHolder.session, expandedConeType = FirTypeAlias::expandedConeTypeWithEnsuredPhase)
}

@ExplicitlyPassedSession
context(_: SessionHolder)
fun ConeKotlinType.fullyExpandedType(useSiteSession: FirSession): ConeKotlinType {
    return fullyExpandedType(useSiteSession, expandedConeType = FirTypeAlias::expandedConeTypeWithEnsuredPhase)
}

/**
 * @see fullyExpandedType (the first function in the file)
 */
@Suppress("SuspiciousWhenOverConeKotlinType") // Not handling intersection types seems a bit suspicious but adding it breaks some tests
fun ConeSimpleKotlinType.fullyExpandedType(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeSimpleKotlinType? = FirTypeAlias::expandedConeTypeWithEnsuredPhase,
): ConeSimpleKotlinType {
    return when (this) {
        is ConeClassLikeType -> fullyExpandedType(useSiteSession, expandedConeType)
        is ConeUnionType -> fullyExpandedUnionType(useSiteSession, expandedConeType)
        else -> this
    }
}

context(sessionHolder: SessionHolder)
fun ConeSimpleKotlinType.fullyExpandedType(): ConeSimpleKotlinType {
    return fullyExpandedType(sessionHolder.session, expandedConeType = FirTypeAlias::expandedConeTypeWithEnsuredPhase)
}

@ExplicitlyPassedSession
context(_: SessionHolder)
fun ConeSimpleKotlinType.fullyExpandedType(useSiteSession: FirSession): ConeSimpleKotlinType {
    return fullyExpandedType(useSiteSession, expandedConeType = FirTypeAlias::expandedConeTypeWithEnsuredPhase)
}

/**
 * @see fullyExpandedType (the first function in the file)
 */
fun ConeRigidType.fullyExpandedType(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeSimpleKotlinType? = FirTypeAlias::expandedConeTypeWithEnsuredPhase,
): ConeRigidType {
    return when (this) {
        is ConeSimpleKotlinType -> fullyExpandedType(useSiteSession, expandedConeType)
        // Expanding DNN type makes no sense, as its original type cannot be class-like type
        is ConeDefinitelyNotNullType -> this
    }
}

context(sessionHolder: SessionHolder)
fun ConeRigidType.fullyExpandedType(): ConeRigidType {
    return fullyExpandedType(sessionHolder.session, expandedConeType = FirTypeAlias::expandedConeTypeWithEnsuredPhase)
}

@Deprecated("Use overload without parameter.", replaceWith = ReplaceWith("fullyExpandedType()"))
context(_: SessionHolder)
fun ConeRigidType.fullyExpandedType(useSiteSession: FirSession): ConeRigidType {
    return fullyExpandedType(useSiteSession, expandedConeType = FirTypeAlias::expandedConeTypeWithEnsuredPhase)
}

private fun ConeClassLikeType.fullyExpandedTypeNoCache(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeSimpleKotlinType?,
): ConeSimpleKotlinType {
    val directExpansionType = directExpansionType(useSiteSession, expandedConeType) ?: return this
    val expansion = directExpansionType.fullyExpandedType(useSiteSession, expandedConeType)
    return expansion.withAbbreviation(AbbreviatedTypeAttribute(this))
}

/**
 * Expands type aliases in the components of a union type and normalizes the result,
 * e.g. flattens a component that is itself a type alias to a union type.
 *
 * @return the same instance if no component was expanded
 */
private fun ConeUnionType.fullyExpandedUnionType(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeSimpleKotlinType?,
): ConeSimpleKotlinType {
    var isAnyComponentExpanded = false

    val expandedPrimary = primaryType.fullyExpandedType(useSiteSession, expandedConeType)
    if (expandedPrimary !== primaryType) isAnyComponentExpanded = true

    val expandedRichErrorTypes = richErrorTypes.map {
        val expandedComponent = it.fullyExpandedType(useSiteSession, expandedConeType)
        if (expandedComponent !== it) isAnyComponentExpanded = true
        expandedComponent
    }

    if (!isAnyComponentExpanded) return this

    val unifiedType = ConeTypeUnifier.unify(expandedPrimary, expandedRichErrorTypes, attributes, useSiteSession.typeContext)
    // Cast should be safe as the only way the result of `unify` is not a `ConeSimpleKotlinType`
    // is when passing `Nothing` and a single flexible error component to `unify` which shouldn't ever happen.
    return unifiedType as ConeSimpleKotlinType
}

fun ConeClassLikeType.directExpansionType(
    useSiteSession: FirSession,
    expandedConeType: (FirTypeAlias) -> ConeSimpleKotlinType? = { alias ->
        alias.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
        alias.expandedConeType
    },
): ConeSimpleKotlinType? {
    if (this is ConeErrorType) return null
    val typeAliasSymbol = lookupTag.toTypeAliasSymbol(useSiteSession) ?: return null
    val typeAlias = typeAliasSymbol.fir

    val resultType = expandedConeType(typeAlias)
        ?.applyNullabilityFrom(useSiteSession, this)
        ?.applyAttributesFrom(this)
        ?: return null

    // A union type has no type arguments of its own, but its components may still use the type parameters of the alias
    if (resultType !is ConeUnionType && resultType.typeArguments.isEmpty()) return resultType
    return mapTypeAliasArguments(typeAlias, this, resultType, useSiteSession).asTypeAliasExpansionOrNull()
}

private fun <T : ConeSimpleKotlinType> T.applyNullabilityFrom(
    session: FirSession,
    abbreviation: ConeClassLikeType
): T {
    if (abbreviation.isMarkedNullable) return withNullability(nullable = true, session.typeContext)
    return this
}

private fun <T : ConeSimpleKotlinType> T.applyAttributesFrom(
    abbreviation: ConeClassLikeType
): T {
    val combinedAttributes = attributes.add(abbreviation.attributes)
    return withAttributes(combinedAttributes)
}

fun FirTypeAlias.mapParametersToArgumentsOf(type: ConeKotlinType): List<Pair<FirTypeParameterSymbol, ConeTypeProjection>> =
    typeParameters.map { it.symbol }.zip(type.typeArguments)

fun createParametersSubstitutor(
    useSiteSession: FirSession,
    typeAliasMap: Map<FirTypeParameterSymbol, ConeTypeProjection>,
): ConeSubstitutor = object : AbstractConeSubstitutor(useSiteSession.typeContext) {
    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        return null
    }

    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
        val type = (projection as? ConeKotlinTypeProjection)?.type ?: return null
        // TODO: Consider making code more generic and "ready" to any kind of types (KT-68497)
        val symbol =
            (type.unwrapToSimpleTypeUsingLowerBound() as? ConeTypeParameterType)?.lookupTag?.symbol
                ?: return super.substituteArgument(projection, index)
        val mappedProjection = typeAliasMap[symbol] ?: return super.substituteArgument(projection, index)

        if (mappedProjection !is ConeKotlinTypeProjection) return mappedProjection

        val mappedType =
            mappedProjection.type
                .updateNullabilityIfNeeded(type)
                .let { mappedType -> mappedType.withAttributes(type.attributes.add(mappedType.attributes)) }

        fun convertProjectionKindToConeTypeProjection(projectionKind: ProjectionKind): ConeTypeProjection {
            return when (projectionKind) {
                ProjectionKind.STAR -> ConeStarProjection
                ProjectionKind.IN -> ConeKotlinTypeProjectionIn(mappedType)
                ProjectionKind.OUT -> ConeKotlinTypeProjectionOut(mappedType)
                ProjectionKind.INVARIANT -> mappedType
            }
        }

        if (mappedProjection.kind == projection.kind) {
            return convertProjectionKindToConeTypeProjection(mappedProjection.kind)
        }

        if (mappedProjection.kind == ProjectionKind.STAR || projection.kind == ProjectionKind.STAR) {
            return ConeStarProjection
        }

        if (mappedProjection.kind == ProjectionKind.INVARIANT) {
            return convertProjectionKindToConeTypeProjection(projection.kind)
        }

        if (projection.kind == ProjectionKind.INVARIANT) {
            return convertProjectionKindToConeTypeProjection(mappedProjection.kind)
        }

        return ConeKotlinTypeConflictingProjection(mappedType)
    }
}

fun FirTypeAlias.createParametersSubstitutor(abbreviatedType: ConeClassLikeType, useSiteSession: FirSession): ConeSubstitutor =
    createParametersSubstitutor(useSiteSession, mapParametersToArgumentsOf(abbreviatedType).toMap())

private fun mapTypeAliasArguments(
    typeAlias: FirTypeAlias,
    abbreviatedType: ConeClassLikeType,
    resultingType: ConeSimpleKotlinType,
    useSiteSession: FirSession,
): ConeKotlinType {
    if (typeAlias.typeParameters.isNotEmpty() && abbreviatedType.typeArguments.isEmpty()) {
        return resultingType.withoutArgumentsForBareTypeAlias(useSiteSession.typeContext)
    }

    return typeAlias.createParametersSubstitutor(abbreviatedType, useSiteSession).substituteOrSelf(resultingType)
}

/**
 * For a bare usage of a generic type alias (`TA` for `typealias TA<T> = List<T>`), the type arguments are dropped from the expansion.
 */
private fun ConeKotlinType.withoutArgumentsForBareTypeAlias(typeContext: ConeTypeContext): ConeKotlinType {
    if (this is ConeClassLikeType) return lookupTag.constructClassType(isMarkedNullable = isMarkedNullable)
    if (this !is ConeUnionType) return this
    return ConeTypeUnifier.unify(
        primaryType.withoutArgumentsForBareTypeAlias(typeContext),
        richErrorTypes.map { it.withoutArgumentsForBareTypeAlias(typeContext) },
        attributes,
        typeContext,
    )
}

/**
 * @see fullyExpandedType
 */
fun FirTypeAlias.fullyExpandedConeType(useSiteSession: FirSession): ConeSimpleKotlinType? {
    return expandedConeType?.fullyExpandedType(useSiteSession)
}

/**
 * @see fullyExpandedType
 */
fun FirTypeAlias.fullyExpandedClass(session: FirSession): FirClassLikeDeclaration? {
    return (fullyExpandedConeType(session) as? ConeClassLikeType)?.toSymbol(session)?.fir
}

inline fun ConeKotlinType.forEachExpandedType(session: FirSession, action: (ConeKotlinType) -> Unit) {
    forEachType(prepareType = { it.fullyExpandedType(session) }, action = action)
}

fun ConeKotlinType.fullyExpandedClassLikeType(session: FirSession): ConeClassLikeType? {
    return fullyExpandedType(session) as? ConeClassLikeType
}

context(holder: SessionHolder)
fun ConeKotlinType.fullyExpandedClassLikeType(): ConeClassLikeType? {
    return fullyExpandedClassLikeType(holder.session)
}
