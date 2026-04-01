/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectOrStaticData
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.functionTypeKind
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.PrivateForInline

/**
 * Returns all constructors of a given class, including generated ones. Resolve phase is not guaranteed.
 *
 * @param session given use-site session
 */
fun FirClass.constructors(session: FirSession): List<FirConstructorSymbol> {
    val result = mutableListOf<FirConstructorSymbol>()
    session.declaredMemberScope(this, memberRequiredPhase = null).processDeclaredConstructors { result += it }
    return result
}

/**
 * Returns all constructors of a given class, including generated ones. Resolve phase is not guaranteed.
 *
 * @param session given use-site session
 */
fun FirClassSymbol<*>.constructors(session: FirSession): List<FirConstructorSymbol> {
    val result = mutableListOf<FirConstructorSymbol>()
    session.declaredMemberScope(this, memberRequiredPhase = null).processDeclaredConstructors { result += it }
    return result
}

/**
 * Process all callables (including functions, enum entries and properties, excluding constructors) declared directly in a given class.
 *
 * This function processed only declared callables, including generated callables, but excluding constructors.
 * Inherited callables are not processed.
 *
 * @param session given use-site session
 * @param memberRequiredPhase the required phase for processed members, status phase is the default one
 */
fun FirClassSymbol<*>.processAllDeclaredCallables(
    session: FirSession,
    memberRequiredPhase: FirResolvePhase = FirResolvePhase.STATUS,
    processor: (FirCallableSymbol<*>) -> Unit
) {
    session.declaredMemberScope(this, memberRequiredPhase).processAllCallables(processor)
}

/**
 * Returns all properties and enum entries declared in a given class, including generated ones. Inherited properties are excluded.
 *
 * @param session given use-site session
 * @param memberRequiredPhase the required phase for properties, status phase is the default one
 */
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

/**
 * Returns all functions declared in a given class, including generated functions. Inherited functions are excluded.
 *
 * @param session given use-site session
 * @param memberRequiredPhase the required phase for functions, status phase is the default one
 */
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

/**
 * Process all classifiers (nested type aliases, objects and classes, including generated ones) declared directly in a given class.
 *
 * @param session given use-site session
 * @param memberRequiredPhase the required phase for processed members, status phase is the default one
 */
fun FirClassSymbol<*>.processAllClassifiers(
    session: FirSession,
    memberRequiredPhase: FirResolvePhase = FirResolvePhase.STATUS,
    processor: (FirClassifierSymbol<*>) -> Unit
) {
    session.declaredMemberScope(this, memberRequiredPhase).processAllClassifiers(processor)
}

/**
 * Returns all declarations declared in a given class, including generated ones. Inherited declarations are excluded.
 *
 * This function processes nested classes, nested objects, nested type aliases,
 * member functions, member properties, enum entries, constructors.
 * Anonymous initializers can't be processed this way.
 *
 * @param session given use-site session
 * @param memberRequiredPhase the required phase for declarations, status phase is the default one
 */
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

/**
 * Returns all declarations declared in a given class, including generated ones. Inherited declarations are excluded.
 *
 * This function processes nested classes, nested objects, nested type aliases,
 * member functions, member properties, enum entries, constructors.
 * Anonymous initializers can't be processed this way.
 *
 * @param session given use-site session
 * @param memberRequiredPhase the required phase for declarations, status phase is the default one
 */
@Suppress("unused")
fun FirClassSymbol<*>.processAllDeclarations(
    session: FirSession,
    memberRequiredPhase: FirResolvePhase = FirResolvePhase.STATUS,
    processor: (FirBasedSymbol<*>) -> Unit
) {
    fir.processAllDeclarations(session, memberRequiredPhase, processor)
}

/**
 * Returns the primary constructor of a given class, if any. Resolve phase is not guaranteed.
 *
 * @param session given use-site session
 */
fun FirClass.primaryConstructorIfAny(session: FirSession): FirConstructorSymbol? {
    return constructors(session).find(FirConstructorSymbol::isPrimary)
}

/**
 * Returns the primary constructor of a given class, if any. Resolve phase is not guaranteed.
 *
 * @param session given use-site session
 */
fun FirClassSymbol<*>.primaryConstructorIfAny(session: FirSession): FirConstructorSymbol? {
    return constructors(session).find(FirConstructorSymbol::isPrimary)
}

/**
 * Returns all enum entries declared in a given class, including generated ones. Resolve phase is not guaranteed.
 *
 * @param session given use-site session
 */
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

/**
 * Returns all enum entries declared in a given class, including generated ones. Resolve phase is not guaranteed.
 *
 * @param session given use-site session
 */
fun FirClassSymbol<*>.collectEnumEntries(session: FirSession): List<FirEnumEntrySymbol> {
    return fir.collectEnumEntries(session).map { it.symbol }
}

context(holder: SessionHolder)
fun FirEnumEntrySymbol.getComplementarySymbols(): List<FirEnumEntrySymbol>? = resolvedReturnType
    .toRegularClassSymbol()
    ?.collectEnumEntries(holder.session)
    ?.filter { it != this }

context(holder: SessionHolder)
fun FirRegularClassSymbol.getComplementarySymbols(): List<FirRegularClassSymbol>? {
    val superTypes = getSuperTypes(holder.session)
        .mapNotNullTo(mutableSetOf()) { it.toRegularClassSymbol() }

    return superTypes.flatMap { superType ->
        if (!superType.isSealed) return@flatMap emptyList()

        superType.fir.getSealedClassInheritors(holder.session)
            .mapNotNull { it.toSymbol() as? FirRegularClassSymbol }
            .filter { it != this@getComplementarySymbols && it !in superTypes }
    }
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

/**
 * Returns the FirClassLikeDeclaration that the
 * sequence of FirTypeAlias'es points to starting
 * with `this`. Or null if something goes wrong or we have anonymous object symbol.
 */
context(sessionHolder: SessionHolder)
fun FirClassLikeSymbol<*>.fullyExpandedClass(): FirRegularClassSymbol? {
    return fullyExpandedClass(useSiteSession = sessionHolder.session)
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
@ScopeFunctionRequiresPrewarm
fun MemberWithBaseScope<FirCallableSymbol<*>>.isTrivialIntersection(): Boolean {
    return baseScope
        .getDirectOverriddenMembersWithBaseScope(member)
        .nonSubsumed()
        .mapTo(mutableSetOf()) { it.member.unwrapSubstitutionOverrides() }.size == 1
}

@ScopeFunctionRequiresPrewarm
context(c: SessionAndScopeSessionHolder)
fun FirIntersectionCallableSymbol.getNonSubsumedOverriddenSymbols(): List<FirCallableSymbol<*>> {
    require(this is FirCallableSymbol<*>)

    val dispatchReceiverScope = dispatchReceiverScope()
    return MemberWithBaseScope(this, dispatchReceiverScope).getNonSubsumedOverriddenSymbols()
}

@ScopeFunctionRequiresPrewarm
fun MemberWithBaseScope<FirCallableSymbol<*>>.getNonSubsumedOverriddenSymbols(): List<FirCallableSymbol<*>> {
    return flattenIntersectionsRecursively()
        .nonSubsumed()
        .distinctBy { it.member.unwrapSubstitutionOverrides<FirCallableSymbol<*>>() }
        .map { it.member }
}

@ScopeFunctionRequiresPrewarm
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

context(c: SessionAndScopeSessionHolder)
fun FirCallableSymbol<*>.dispatchReceiverScope(): FirTypeScope {
    val dispatchReceiverType = requireNotNull(dispatchReceiverType)
    return dispatchReceiverType.scope(
        CallableCopyTypeCalculator.DoNothing,
        FirResolvePhase.STATUS
    ) ?: FirTypeScope.Empty
}

@ScopeFunctionRequiresPrewarm
fun MemberWithBaseScope<FirCallableSymbol<*>>.flattenIntersectionsRecursively(): List<MemberWithBaseScope<FirCallableSymbol<*>>> {
    if (member.unwrapSubstitutionOverrides<FirCallableSymbol<*>>().origin != FirDeclarationOrigin.IntersectionOverride) return listOf(this)

    return baseScope.getDirectOverriddenMembersWithBaseScope(member).flatMap { it.flattenIntersectionsRecursively() }
}

@ScopeFunctionRequiresPrewarm
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
@ScopeFunctionRequiresPrewarm
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

fun FirValueParameter.isInlinable(session: FirSession): Boolean {
    if (isNoinline) return false
    val fullyExpandedType = returnTypeRef.coneType.fullyExpandedType(session)
    return !fullyExpandedType.isMarkedNullable && fullyExpandedType.functionTypeKind(session)?.isInlineable == true
}