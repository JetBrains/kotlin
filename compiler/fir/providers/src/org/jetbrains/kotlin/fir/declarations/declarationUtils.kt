/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectOrStaticData
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.PrivateForInline

fun FirClass.constructors(session: FirSession): List<FirConstructorSymbol> {
    val result = mutableListOf<FirConstructorSymbol>()
    session.declaredMemberScope(this, memberRequiredPhase = null).processDeclaredConstructors { result += it }
    return result
}

fun FirClassSymbol<*>.constructors(session: FirSession): List<FirConstructorSymbol> {
    val result = mutableListOf<FirConstructorSymbol>()
    session.declaredMemberScope(this, memberRequiredPhase = null).processDeclaredConstructors { result += it }
    return result
}

fun FirClassSymbol<*>.processAllDeclaredCallables(
    session: FirSession,
    memberRequiredPhase: FirResolvePhase = FirResolvePhase.STATUS,
    processor: (FirCallableSymbol<*>) -> Unit
) {
    session.declaredMemberScope(this, memberRequiredPhase).processAllCallables(processor)
}

fun FirClassSymbol<*>.declaredProperties(
    session: FirSession,
    memberRequiredPhase: FirResolvePhase = FirResolvePhase.STATUS,
): List<FirPropertySymbol> {
    val result = mutableListOf<FirPropertySymbol>()
    processAllDeclaredCallables(session, memberRequiredPhase) {
        if (it is FirPropertySymbol) {
            result += it
        }
    }
    return result
}

fun FirClassSymbol<*>.declaredFunctions(
    session: FirSession,
    memberRequiredPhase: FirResolvePhase = FirResolvePhase.STATUS,
): List<FirNamedFunctionSymbol> {
    val result = mutableListOf<FirNamedFunctionSymbol>()
    processAllDeclaredCallables(session, memberRequiredPhase) {
        if (it is FirNamedFunctionSymbol) {
            result += it
        }
    }
    return result
}

fun FirClassSymbol<*>.processAllClassifiers(
    session: FirSession,
    memberRequiredPhase: FirResolvePhase = FirResolvePhase.STATUS,
    processor: (FirClassifierSymbol<*>) -> Unit
) {
    session.declaredMemberScope(this, memberRequiredPhase).processAllClassifiers(processor)
}

fun FirClass.processAllDeclarations(
    session: FirSession,
    memberRequiredPhase: FirResolvePhase = FirResolvePhase.STATUS,
    processor: (FirBasedSymbol<*>) -> Unit
) {
    session.declaredMemberScope(this, memberRequiredPhase).let {
        it.processAllClassifiers(processor)
        it.processAllCallables(processor)
        it.processDeclaredConstructors(processor)
    }
    declarations.forEach {
        if (it !is FirAnonymousInitializer) return@forEach
        processor(it.symbol)
    }
}

fun FirClass.primaryConstructorIfAny(session: FirSession): FirConstructorSymbol? {
    return constructors(session).find(FirConstructorSymbol::isPrimary)
}

fun FirClassSymbol<*>.primaryConstructorIfAny(session: FirSession): FirConstructorSymbol? {
    return constructors(session).find(FirConstructorSymbol::isPrimary)
}

fun FirClass.collectEnumEntries(session: FirSession): List<FirEnumEntry> {
    assert(classKind == ClassKind.ENUM_CLASS)
    val result = mutableListOf<FirEnumEntry>()
    session.declaredMemberScope(this, memberRequiredPhase = null).processAllProperties {
        if (it is FirEnumEntrySymbol) {
            result.add(it.fir)
        }
    }
    return result
}

fun FirClassSymbol<*>.collectEnumEntries(session: FirSession): List<FirEnumEntrySymbol> {
    return fir.collectEnumEntries(session).map { it.symbol }
}

/**
 * Returns the FirClassLikeDeclaration that the
 * sequence of FirTypeAlias'es points to starting
 * with `this`. Or null if something goes wrong or we have anonymous object symbol.
 */
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
        .toRegularClassSymbol(session)
}

fun FirRegularClassSymbol.isInlineOrValueClass(): Boolean {
    if (this.classKind != ClassKind.CLASS) return false

    return isInlineOrValue
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

fun FirNamedFunctionSymbol.isEquals(session: FirSession): Boolean {
    if (name != OperatorNameConventions.EQUALS) return false
    if (valueParameterSymbols.size != 1) return false
    if (contextParameterSymbols.isNotEmpty()) return false
    if (receiverParameterSymbol != null) return false
    val parameter = valueParameterSymbols.first()
    return parameter.resolvedReturnTypeRef.coneType.fullyExpandedType(session).isNullableAny
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

fun Collection<MemberWithBaseScope<FirCallableSymbol<*>>>.getNonSubsumedNonPhantomOverriddenSymbols(): List<MemberWithBaseScope<FirCallableSymbol<*>>> {
    // It's crucial that we only unwrap phantom intersection overrides.
    // See comments in the following tests for explanation:
    // - intersectionWithMultipleDefaultsInJavaOverriddenByIntersectionInKotlin.kt
    // - intersectionOverridesIntersection.kt
    return flatMap { it.flattenPhantomIntersectionsRecursively() }
        .nonSubsumed()
        // To learn why `distinctBy` is needed, see:
        // - intersectionWithMultipleDefaultsInJavaWithAdditionalSymbolsAfterNonSubsumed.kt
        .distinctBy { it.member.unwrapSubstitutionOverrides<FirCallableSymbol<*>>() }
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

fun MemberWithBaseScope<FirCallableSymbol<*>>.flattenPhantomIntersectionsRecursively(): List<MemberWithBaseScope<FirCallableSymbol<*>>> {
    val symbol = member.unwrapSubstitutionOverrides<FirCallableSymbol<*>>()

    if (symbol !is FirIntersectionCallableSymbol || symbol.containsMultipleNonSubsumed) {
        return listOf(this)
    }

    return baseScope.getDirectOverriddenMembersWithBaseScope(member).flatMap { it.flattenPhantomIntersectionsRecursively() }
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
