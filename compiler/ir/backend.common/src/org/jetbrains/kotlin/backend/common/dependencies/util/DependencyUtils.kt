/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies.util

import org.jetbrains.kotlin.backend.common.dependencies.BeginInstanceInitializationIndex
import org.jetbrains.kotlin.backend.common.dependencies.EndInstanceInitializationIndex
import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.util.fileOrNull

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

fun IrClassSymbol.collectEnumEntries(): List<IrEnumEntrySymbol> {
    if (!isBound) return emptyList()
    if (!owner.hasEnumEntries) return emptyList()
    return owner.declarations.asSequence().filterIsInstance<IrEnumEntry>().map { it.symbol }.toList()
}

val <D : IrDeclaration> IrBindableSymbol<*, D>.containingFileSymbol: IrFileSymbol? get() = owner.fileOrNull?.symbol

val IrClassSymbol.isInitializedBySupertypes: Boolean
    get() = owner.let {
        !it.kind.isInterface || it.kind.isInterface && it.declarations.any { decl ->
            decl is IrProperty && decl.hasCustomAccessors || decl is IrFunction && decl.body != null
        }
    }

val IrSimpleFunction.hasCustomImplementation: Boolean get() = origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR

val IrProperty.hasCustomAccessors: Boolean get() = (getter?.hasCustomImplementation ?: false) || (setter?.hasCustomImplementation ?: false)

val <D : IrDeclarationWithVisibility> IrBindableSymbol<*, D>.isPrivate: Boolean get() = owner.isEffectivelyPrivate()

operator fun <D : IrDeclaration> IrModuleFragment.contains(symbol: IrBindableSymbol<*, D>): Boolean =
    symbol.owner.fileOrNull?.let { it in this } ?: false

operator fun IrModuleFragment.contains(decl: IrDeclaration): Boolean =
    decl.fileOrNull?.let { it in this } ?: false

operator fun IrModuleFragment.contains(file: IrFile): Boolean = file in files

val IrClassSymbol.beginInitializationIndex: BeginInstanceInitializationIndex
    get() = BeginInstanceInitializationIndex(this)

val IrClassSymbol.endInitializationIndex: EndInstanceInitializationIndex
    get() = EndInstanceInitializationIndex(this)

infix operator fun <T> List<T>.plus(element: T?): List<T> = toMutableList().apply { element?.let(::add) }

infix operator fun <K, V> Map<K, V>.plus(entry: Pair<K, V>?): Map<K, V> = toMutableMap().apply {
    entry?.let { put(it.first, it.second) }
}
