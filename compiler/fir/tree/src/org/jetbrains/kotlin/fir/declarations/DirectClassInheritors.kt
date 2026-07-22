/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.utils.SmartSet
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

private object DirectClassInheritorsKey : FirDeclarationDataKey()

private var FirRegularClass.directInheritorsAttr: Lazy<MutableSet<FirClassSymbol<*>>>? by FirDeclarationDataRegistry.data(DirectClassInheritorsKey)

private val FirRegularClassSymbol.directInheritorsAttr: Lazy<MutableSet<FirClassSymbol<*>>>? by FirDeclarationDataRegistry.symbolAccessor(DirectClassInheritorsKey)

fun FirRegularClass.addDirectInheritors(vararg inheritors: FirClassSymbol<*>) {
    directInheritorsAttr = lazyOf(
        value = directInheritorsAttr?.value?.apply { this += inheritors }
            ?: SmartSet.create<FirClassSymbol<*>>().apply { this += inheritors }
    )
}

fun FirRegularClass.addDirectInheritors(inheritors: Set<FirClassSymbol<*>>) {
    directInheritorsAttr = lazyOf(
        value = directInheritorsAttr?.value?.apply { this += inheritors }
            ?: SmartSet.create<FirClassSymbol<*>>().apply { this += inheritors }
    )
}

val FirRegularClass.directInheritors: Set<FirClassSymbol<*>> get() = directInheritorsAttr?.value ?: emptySet()

val FirRegularClassSymbol.directInheritors: Set<FirClassSymbol<*>> get() = directInheritorsAttr?.value ?: emptySet()

val FirRegularClassSymbol.allInheritors: Sequence<FirClassSymbol<*>>
    get() = traversal(this) { symbol ->
        symbol.directInheritors.forEach {
            emit(it)
            if (it is FirRegularClassSymbol) traverseFor(it)
        }
    }.distinct()
