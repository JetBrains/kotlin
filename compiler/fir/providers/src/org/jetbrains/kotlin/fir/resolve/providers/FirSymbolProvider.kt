/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.getSymbolByLookupTag
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirSyntheticFunctionInterfaceProviderBase.Companion.mayBeSyntheticFunctionClassName
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@RequiresOptIn
annotation class FirSymbolProviderInternals

abstract class FirSymbolProvider(val session: FirSession) : FirSessionComponent {
    abstract fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>?

    @OptIn(FirSymbolProviderInternals::class)
    open fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        return buildList { getTopLevelCallableSymbolsTo(this, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    abstract fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name)

    @OptIn(FirSymbolProviderInternals::class)
    open fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<FirNamedFunctionSymbol> {
        return buildList { getTopLevelFunctionSymbolsTo(this, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    abstract fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name)

    @OptIn(FirSymbolProviderInternals::class)
    open fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<FirPropertySymbol> {
        return buildList { getTopLevelPropertySymbolsTo(this, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    abstract fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name)

    abstract fun getPackage(fqName: FqName): FqName? // TODO: Replace to symbol sometime

    /**
     * All the three "compute*" functions below have the following common contract:
     * - They return null in case necessary name set is too hard/impossible to compute.
     * - They might return a strict superset of the name set, i.e. the resulting set might contain some names that do not belong to the provider.
     * - It might be non-cheap to compute them on each query, thus their result should be cached properly.
     *
     * @returns full package names that contain some top-level callables
     */
    abstract fun computePackageSetWithTopLevelCallables(): Set<String>?

    /**
     * @returns top-level classifier names that belong to `packageFqName` or null if it's complicated to compute the set
     *
     * All usages must take into account that the result might not include kotlin.FunctionN
     * (and others for which org.jetbrains.kotlin.builtins.functions.FunctionClassKind.Companion.byClassNamePrefix not-null)
     */
    abstract fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>?

    /**
     * @returns top-level callable names that belong to `packageFqName` or null if it's complicated to compute the set
     */
    abstract fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>?
}

/**
 * Works almost as regular flatMap, but returns a set and returns null if any lambda call returned null
 */
inline fun <T, R> Iterable<T>.flatMapToNullableSet(transform: (T) -> Iterable<R>?): Set<R>? =
    flatMapTo(mutableSetOf()) { transform(it) ?: return null }

private fun FirSymbolProvider.getClassDeclaredMemberScope(classId: ClassId): FirScope? {
    val classSymbol = getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: return null
    return session.declaredMemberScope(classSymbol.fir)
}

fun FirSymbolProvider.getClassDeclaredConstructors(classId: ClassId): List<FirConstructorSymbol> {
    val classMemberScope = getClassDeclaredMemberScope(classId)
    return classMemberScope?.getDeclaredConstructors().orEmpty()
}

fun FirSymbolProvider.getClassDeclaredFunctionSymbols(classId: ClassId, name: Name): List<FirNamedFunctionSymbol> {
    val classMemberScope = getClassDeclaredMemberScope(classId)
    return classMemberScope?.getFunctions(name).orEmpty()
}

fun FirSymbolProvider.getClassDeclaredPropertySymbols(classId: ClassId, name: Name): List<FirVariableSymbol<*>> {
    val classMemberScope = getClassDeclaredMemberScope(classId)
    return classMemberScope?.getProperties(name).orEmpty()
}

inline fun <reified T : FirBasedSymbol<*>> FirSymbolProvider.getSymbolByTypeRef(typeRef: FirTypeRef): T? {
    val lookupTag = (typeRef.coneTypeSafe<ConeSimpleKotlinType>()?.fullyExpandedType(session) as? ConeLookupTagBasedType)?.lookupTag
        ?: return null
    return getSymbolByLookupTag(lookupTag) as? T
}

fun FirSymbolProvider.getRegularClassSymbolByClassId(classId: ClassId): FirRegularClassSymbol? {
    return getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol
}

/**
 * Whether [classId] may be contained in the set of known classifier names.
 *
 * If it's certain that [classId] cannot be a function class (for example when [classId] is known to come from a package without
 * function classes), [mayBeFunctionClass] can be set to `false`. This avoids a hash map access in
 * [org.jetbrains.kotlin.builtins.functions.FunctionTypeKindExtractor.getFunctionalClassKindWithArity].
 */
fun Set<String>.mayHaveTopLevelClassifier(
    classId: ClassId,
    session: FirSession,
    mayBeFunctionClass: Boolean = true,
): Boolean {
    if (mayBeFunctionClass && isNameForFunctionClass(classId, session)) return true

    if (classId.outerClassId == null) {
        if (!mayHaveTopLevelClassifier(classId.shortClassName)) return false
    } else {
        if (!mayHaveTopLevelClassifier(classId.outermostClassId.shortClassName)) return false
    }

    return true
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Set<String>.mayHaveTopLevelClassifier(shortClassName: Name): Boolean =
    shortClassName.asString() in this || shortClassName.isSpecial

@OptIn(FirSymbolProviderInternals::class)
private fun isNameForFunctionClass(classId: ClassId, session: FirSession): Boolean {
    if (!classId.mayBeSyntheticFunctionClassName()) return false
    return session.functionTypeService.getKindByClassNamePrefix(classId.packageFqName, classId.shortClassName.asString()) != null
}

fun ClassId.toSymbol(session: FirSession): FirClassifierSymbol<*>? {
    return session.symbolProvider.getClassLikeSymbolByClassId(this)
}

val FirSession.symbolProvider: FirSymbolProvider by FirSession.sessionComponentAccessor()

const val DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY: String = "org.jetbrains.kotlin.fir.resolve.providers.FirDependenciesSymbolProvider"

val FirSession.dependenciesSymbolProvider: FirSymbolProvider by FirSession.sessionComponentAccessor(
    DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY
)
