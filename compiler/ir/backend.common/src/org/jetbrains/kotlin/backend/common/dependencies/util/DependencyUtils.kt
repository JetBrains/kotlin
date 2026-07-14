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
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.isSetter
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume

@RestrictsSuspension
interface TraversalScope<T, R> {

    suspend fun emit(value: R)

    suspend fun traverseFor(element: T)
}

fun <T, R> traversal(initial: T, block: suspend TraversalScope<T, R>.(T) -> Unit): Sequence<R> = object : Sequence<R> {
    override fun iterator(): Iterator<R> = TraversalIterator(block, initial)
}

private fun <R, T, V> (suspend R.(T) -> V).lowerArity(argument: T): suspend R.() -> V = { this@lowerArity(argument) }

/**
 * This iterator tries to marry Sequences and DeepRecursiveFunctions in a coroutine-friendly way.
 * It is used to generate a sequence of values such that the generation is given by a function that can be called recursively.
 * It is called TraversalIterator since it allows natural implementations of graph traversals such as DFS.
 * Most of the code is adapted from [SequenceBuilderIterator] class.
 */
private class TraversalIterator<T, R>(
    private val block: suspend TraversalScope<T, R>.(T) -> Unit,
    initial: T
) : TraversalScope<T, R>, Iterator<R>, Continuation<Unit> {

    private enum class TraversalState {
        NotReady,
        Ready,
        Done,
        Failed,
    }

    private var state = TraversalState.NotReady
    private var nextValue: R? = null
    var nextStep: Continuation<Unit>? =
        block.lowerArity(initial).createCoroutineUnintercepted(receiver = this, completion = this)

    override fun hasNext(): Boolean {
        while (true) {
            when (state) {
                TraversalState.NotReady -> {}
                TraversalState.Done -> return false
                TraversalState.Ready -> return true
                else -> throw exceptionalState()
            }

            state = TraversalState.Failed
            val step = nextStep!!
            nextStep = null
            step.resume(Unit)
        }
    }

    override fun next(): R {
        when (state) {
            TraversalState.NotReady if hasNext() -> return next()
            TraversalState.NotReady -> throw NoSuchElementException()
            TraversalState.Ready -> {
                state = TraversalState.NotReady
                val result = nextValue!!
                nextValue = null
                return result
            }
            else -> throw exceptionalState()
        }
    }

    private fun exceptionalState(): Throwable = when (state) {
        TraversalState.Done -> NoSuchElementException()
        TraversalState.Failed -> IllegalStateException("Iterator has failed. ($this)")
        else -> IllegalStateException("Unexpected state of the iterator: $state")
    }


    override suspend fun emit(value: R) {
        nextValue = value
        state = TraversalState.Ready
        return suspendCoroutineUninterceptedOrReturn { c ->
            nextStep = c
            COROUTINE_SUSPENDED
        }
    }

    override suspend fun traverseFor(element: T) {
        return suspendCoroutineUninterceptedOrReturn { c ->
            // Treat the iterator as if it has been initialized again, so the recursive traversal simply becomes reinitialization with a new initial value
            // No need to clean the nextValue, as the nextStep resumption will only happen after its consumption (next() switches the state to NotReady)
            state = TraversalState.NotReady
            nextStep = block.lowerArity(element).createCoroutineUnintercepted(receiver = this, completion = c)
            COROUTINE_SUSPENDED
        }
    }

    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow()
        state = TraversalState.Done
    }

    override val context: CoroutineContext
        get() = EmptyCoroutineContext
}

sealed class TraversalOrder {

    abstract suspend fun <T> TraversalScope<T, T>.traverseNext(current: T, neighbours: (T) -> Sequence<T>)

    inline fun <T> traverse(
        start: T,
        visited: MutableSet<T> = mutableSetOf(),
        crossinline predicate: (T) -> Boolean = { true },
        noinline neighbours: (T) -> Sequence<T>
    ): Sequence<T> = traversal(start) {
        when (predicate(it) && visited.add(it)) {
            true -> traverseNext(it, neighbours)
            false -> {}
        }
    }

    object PreOrder : TraversalOrder() {
        override suspend fun <T> TraversalScope<T, T>.traverseNext(current: T, neighbours: (T) -> Sequence<T>) {
            emit(current)
            neighbours(current).forEach { traverseFor(it) }
        }
    }

    object PostOrder : TraversalOrder() {
        override suspend fun <T> TraversalScope<T, T>.traverseNext(current: T, neighbours: (T) -> Sequence<T>) {
            neighbours(current).forEach { traverseFor(it) }
            emit(current)
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

val IrSimpleFunction.isCustomAccessor: Boolean get() = (isGetter || isSetter) && origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR

val IrProperty.hasCustomAccessors: Boolean get() = (getter?.isCustomAccessor ?: false) || (setter?.isCustomAccessor ?: false)

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
