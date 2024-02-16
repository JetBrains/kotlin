/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectOrStaticData
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.util.OperatorNameConventions

fun FirClass.constructors(session: FirSession): List<FirConstructorSymbol> {
    val result = mutableListOf<FirConstructorSymbol>()
    session.declaredMemberScope(this, memberRequiredPhase = null).processDeclaredConstructors { result += it }
    return result
}

fun FirClass.primaryConstructorIfAny(session: FirSession): FirConstructorSymbol? {
    return constructors(session).find(FirConstructorSymbol::isPrimary)
}

// TODO: dog shit, rewrite with scopes
fun FirClass.collectEnumEntries(): Collection<FirEnumEntry> {
    assert(classKind == ClassKind.ENUM_CLASS)
    return declarations.filterIsInstance<FirEnumEntry>()
}

fun FirClassSymbol<*>.collectEnumEntries(): Collection<FirEnumEntrySymbol> {
    return fir.collectEnumEntries().map { it.symbol }
}

/**
 * Returns the FirClassLikeDeclaration that the
 * sequence of FirTypeAlias'es points to starting
 * with `this`. Or null if something goes wrong or we have anonymous object symbol.
 */
@Suppress("NO_TAIL_CALLS_FOUND", "NON_TAIL_RECURSIVE_CALL") // K2 warning suppression, TODO: KT-62472
tailrec fun FirClassLikeSymbol<*>.fullyExpandedClass(useSiteSession: FirSession): FirRegularClassSymbol? {
    return when (this) {
        is FirRegularClassSymbol -> this
        is FirAnonymousObjectSymbol -> null
        is FirTypeAliasSymbol -> resolvedExpandedTypeRef.coneTypeSafe<ConeClassLikeType>()
            ?.toSymbol(useSiteSession)?.fullyExpandedClass(useSiteSession)
    }
}

fun FirBasedSymbol<*>.isAnnotationConstructor(session: FirSession): Boolean {
    if (this !is FirConstructorSymbol) return false
    return getConstructedClass(session)?.classKind == ClassKind.ANNOTATION_CLASS
}

fun FirBasedSymbol<*>.isPrimaryConstructorOfInlineOrValueClass(session: FirSession): Boolean {
    if (this !is FirConstructorSymbol) return false
    return getConstructedClass(session)?.isInlineOrValueClass() == true && this.isPrimary
}

fun FirConstructorSymbol.getConstructedClass(session: FirSession): FirRegularClassSymbol? {
    return resolvedReturnTypeRef.coneType
        .fullyExpandedType(session)
        .toSymbol(session) as? FirRegularClassSymbol?
}

fun FirRegularClassSymbol.isInlineOrValueClass(): Boolean {
    if (this.classKind != ClassKind.CLASS) return false

    return isInline
}

@PrivateForInline
inline val FirDeclarationOrigin.isJavaOrEnhancement: Boolean
    get() = this is FirDeclarationOrigin.Java || this == FirDeclarationOrigin.Enhancement

@OptIn(PrivateForInline::class)
val FirDeclaration.isJavaOrEnhancement: Boolean
    get() = origin.isJavaOrEnhancement ||
            (this as? FirCallableDeclaration)?.importedFromObjectOrStaticData?.original?.isJavaOrEnhancement == true

@OptIn(PrivateForInline::class)
inline val FirBasedSymbol<*>.isJavaOrEnhancement: Boolean
    get() = origin.isJavaOrEnhancement ||
            (fir as? FirCallableDeclaration)?.importedFromObjectOrStaticData?.original?.isJavaOrEnhancement == true

private fun FirFunction.containsDefaultValue(index: Int): Boolean = valueParameters[index].defaultValue != null

/**
 * Checks, if the value parameter has a default value w.r.t expect/actuals.
 *
 * Requires [FirResolvePhase.EXPECT_ACTUAL_MATCHING]
 *
 * In expect/actual declarations, the presence of default values can be determined by the expect declaration.
 *
 * ```kotlin
 * // MODULE: common
 * expect fun foo(bar: Int = 42)
 *
 * // MODULE: platform()(common)
 * actual fun foo(bar: Int) { ... }
 * ```
 *
 * See `/docs/fir/k2_kmp.md`
 *
 * @param index Index of the value parameter to check
 * @return `true` if a parameter has defined default value, or if there is a default value defined on the expect declaration
 *  for this actual.
 */
fun FirFunction.itOrExpectHasDefaultParameterValue(index: Int): Boolean =
    containsDefaultValue(index) || symbol.getSingleMatchedExpectForActualOrNull()?.fir?.containsDefaultValue(index) == true

fun FirSimpleFunction.isEquals(session: FirSession): Boolean {
    if (name != OperatorNameConventions.EQUALS) return false
    if (valueParameters.size != 1) return false
    if (contextReceivers.isNotEmpty()) return false
    if (receiverParameter != null) return false
    val parameter = valueParameters.first()
    return parameter.returnTypeRef.coneType.fullyExpandedType(session).isNullableAny
}

/**
 * An intersection override is trivial if one of the overridden symbols subsumes all others.
 *
 * @see org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScopeContext.convertGroupedCallablesToIntersectionResults
 */
fun MemberWithBaseScope<FirCallableSymbol<*>>.isTrivialIntersection(): Boolean {
    return baseScope
        .getDirectOverriddenMembersWithBaseScope(member)
        .nonSubsumed()
        .mapTo(mutableSetOf()) { it.member.unwrapSubstitutionOverrides() }.size == 1
}

fun FirIntersectionCallableSymbol.getNonSubsumedOverriddenSymbols(
    session: FirSession,
    scopeSession: ScopeSession,
): List<FirCallableSymbol<*>> {
    require(this is FirCallableSymbol<*>)

    val dispatchReceiverScope = dispatchReceiverScope(session, scopeSession)
    return MemberWithBaseScope(this, dispatchReceiverScope).getNonSubsumedOverriddenSymbols()
}

fun MemberWithBaseScope<FirCallableSymbol<*>>.getNonSubsumedOverriddenSymbols(): List<FirCallableSymbol<*>> {
    return flattenIntersectionsRecursively()
        .nonSubsumed()
        .distinctBy { it.member.unwrapSubstitutionOverrides<FirCallableSymbol<*>>() }
        .map { it.member }
}

fun FirCallableSymbol<*>.dispatchReceiverScope(session: FirSession, scopeSession: ScopeSession): FirTypeScope {
    val dispatchReceiverType = requireNotNull(dispatchReceiverType)
    return dispatchReceiverType.scope(
        session,
        scopeSession,
        CallableCopyTypeCalculator.DoNothing,
        FirResolvePhase.STATUS
    ) ?: FirTypeScope.Empty
}

fun MemberWithBaseScope<FirCallableSymbol<*>>.flattenIntersectionsRecursively(): List<MemberWithBaseScope<FirCallableSymbol<*>>> {
    if (member.unwrapSubstitutionOverrides<FirCallableSymbol<*>>().origin != FirDeclarationOrigin.IntersectionOverride) return listOf(this)

    return baseScope.getDirectOverriddenMembersWithBaseScope(member).flatMap { it.flattenIntersectionsRecursively() }
}

/**
 * A callable declaration D [subsumes](https://kotlinlang.org/spec/inheritance.html#matching-and-subsumption-of-declarations)
 * a callable declaration B if D overrides B.
 */
fun Collection<MemberWithBaseScope<FirCallableSymbol<*>>>.nonSubsumed(): List<MemberWithBaseScope<FirCallableSymbol<*>>> {
    val baseMembers = mutableSetOf<FirCallableSymbol<*>>()
    for ((member, scope) in this) {
        val unwrapped = member.unwrapSubstitutionOverrides<FirCallableSymbol<*>>()
        val addIfDifferent = { it: FirCallableSymbol<*> ->
            val symbol = it.unwrapSubstitutionOverrides()
            if (symbol != unwrapped) {
                baseMembers += symbol
            }
            ProcessorAction.NEXT
        }
        if (member is FirNamedFunctionSymbol) {
            scope.processOverriddenFunctions(member, addIfDifferent)
        } else if (member is FirPropertySymbol) {
            scope.processOverriddenProperties(member, addIfDifferent)
        }
    }
    return filter { (member, _) -> member.unwrapSubstitutionOverrides<FirCallableSymbol<*>>() !in baseMembers }
}