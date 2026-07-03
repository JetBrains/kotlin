/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isEnumEntry
import org.jetbrains.kotlin.fir.nullableModuleData
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.getContainingSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe

sealed class TraversalOrder {

    abstract suspend fun <T> SequenceScope<T>.traverseNext(current: T, neighbours: (T) -> Sequence<T>)

    inline fun <T> traverse(
        start: T,
        visited: MutableSet<T> = mutableSetOf(),
        crossinline predicate: (T) -> Boolean = { true },
        crossinline neighbours: (T) -> Sequence<T>
    ): Sequence<T> =
        sequence {
            when (predicate(start) && visited.add(start)) {
                true -> traverseNext(start) { next -> neighbours(next).filter { predicate(it) && visited.add(it) } }
                false -> {}
            }
        }

    object PreOrder : TraversalOrder() {
        override suspend fun <T> SequenceScope<T>.traverseNext(current: T, neighbours: (T) -> Sequence<T>) {
            yield(current)
            neighbours(current).forEach {
                traverseNext(it, neighbours)
            }
        }
    }

    object PostOrder : TraversalOrder() {
        override suspend fun <T> SequenceScope<T>.traverseNext(current: T, neighbours: (T) -> Sequence<T>) {
            neighbours(current).forEach {
                traverseNext(it, neighbours)
            }
            yield(current)
        }
    }
}

operator fun <E, M : MutableCollection<E>> M.plus(other: Iterable<E>): M = apply {
    other.forEach { add(it) }
}

context(sessionHolder: SessionHolder)
fun FirBasedSymbol<*>.inSameModule(): Boolean = sessionHolder.session.nullableModuleData?.let { it == moduleData } ?: false

context(sessionHolder: SessionHolder)
fun FirDeclaration.inSameModule(): Boolean = symbol.inSameModule()

fun FirClassSymbol<*>.collectEnumEntries(): List<FirEnumEntrySymbol> = collectEnumEntries(moduleData.session)

context(sessionHolder: SessionHolder)
fun FirAnonymousObjectSymbol.findCorrespondingEnumEntry(): Pair<FirRegularClassSymbol, FirEnumEntrySymbol>? = when {
    isEnumEntry -> resolvedSuperTypes.asSequence()
        .mapNotNull { it.fullyExpandedType().toRegularClassSymbol() }
        .find { it.isEnumClass }
        ?.let { enumClass ->
            lateinit var enumEntry: FirEnumEntrySymbol
            enumClass.declaredMemberScope(sessionHolder.session, memberRequiredPhase = FirResolvePhase.STATUS).processAllProperties {
                if (it is FirEnumEntrySymbol && it.initializerObjectSymbol == this) {
                    enumEntry = it
                }
            }
            enumClass to enumEntry
        }
    else -> null
}

context(sessionHolder: SessionHolder)
tailrec fun FirClassLikeSymbol<*>.fullyExpandClass(): FirClassSymbol<*>? {
    return when (this) {
        is FirRegularClassSymbol -> this
        is FirAnonymousObjectSymbol -> this
        is FirTypeAliasSymbol -> resolvedExpandedTypeRef.coneTypeSafe<ConeClassLikeType>()?.toSymbol()?.fullyExpandClass()
    }
}

context(sessionHolder: SessionHolder)
val FirCallableSymbol<*>.containingFileSymbol: FirFileSymbol? get() = getContainingSymbol(sessionHolder.session) as? FirFileSymbol

val FirClassSymbol<*>.isInitializedBySupertypes: Boolean
    get() = !classKind.isInterface || classKind.isInterface && declarationSymbols.any {
        it is FirPropertySymbol && it.hasCustomAccessors || it is FirFunctionSymbol<*> && it.hasBody
    }

val FirPropertyAccessorSymbol.hasCustomImplementation: Boolean get() = !isDefault && hasBody

val FirPropertySymbol.hasCustomAccessors: Boolean
    get() = (getterSymbol?.hasCustomImplementation ?: false) || (setterSymbol?.hasCustomImplementation ?: false)

val FirBasedSymbol<*>.isLibraryDeclaration: Boolean
    get() = origin == FirDeclarationOrigin.Library
            || origin == FirDeclarationOrigin.Java.Library
            || moduleData.session.kind == FirSession.Kind.Library

val FirClassSymbol<*>.isPrivate: Boolean
    get() = when (effectiveVisibility) {
        is EffectiveVisibility.PrivateInClass -> true
        is EffectiveVisibility.PrivateInFile -> true
        is EffectiveVisibility.Local -> true
        else -> false
    }

val FirCallableSymbol<*>.isPrivate: Boolean
    get() = when (effectiveVisibility) {
        is EffectiveVisibility.PrivateInClass -> true
        is EffectiveVisibility.PrivateInFile -> true
        is EffectiveVisibility.Local -> true
        else -> false
    }

val <C : FirClass> FirClassSymbol<C>.beginInitializationIndex: BeginInstanceInitializationIndex<C>
    get() = BeginInstanceInitializationIndex(this)

val <C : FirClass> FirClassSymbol<C>.endInitializationIndex: EndInstanceInitializationIndex<C>
    get() = EndInstanceInitializationIndex(this)

infix operator fun <T> List<T>.plus(element: T?): List<T> = toMutableList().apply { element?.let(::add) }

infix operator fun <K, V> Map<K, V>.plus(entry: Pair<K, V>?): Map<K, V> = toMutableMap().apply {
    entry?.let { put(it.first, it.second) }
}

data class AccessPath(
    val symbol: FirCallableSymbol<*>,
    val next: List<AccessPath> = emptyList()
)
