/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.util.asReversedFrozen

abstract class Stack<T> {
    abstract val size: Int
    abstract fun top(): T
    abstract fun pop(): T
    abstract fun push(value: T)
    abstract fun reset()

    /**
     * returns all elements of the stack in order of retrieval
     */
    abstract fun all(): List<T>
}

fun <T> stackOf(vararg values: T): Stack<T> = StackImpl(*values)
val Stack<*>.isEmpty: Boolean get() = size == 0
val Stack<*>.isNotEmpty: Boolean get() = size != 0
fun <T> Stack<T>.topOrNull(): T? = if (size == 0) null else top()
fun <T> Stack<T>.popOrNull(): T? = if (size == 0) null else pop()

private class StackImpl<T>(vararg values: T) : Stack<T>() {
    private val stack = mutableListOf(*values)

    override fun top(): T = stack[stack.size - 1]
    override fun pop(): T = stack.removeAt(stack.size - 1)

    override fun push(value: T) {
        stack.add(value)
    }

    override val size: Int get() = stack.size
    override fun reset() {
        stack.clear()
    }

    override fun all(): List<T> = stack.asReversedFrozen()
}

class NodeStorage<T : FirElement, N : CFGNode<T>> : Stack<N>(){
    private val stack: Stack<N> = StackImpl()
    private val map: MutableMap<T, N> = mutableMapOf()

    override val size: Int get() = stack.size

    override fun top(): N = stack.top()

    override fun pop(): N = stack.pop().also {
        map.remove(it.fir)
    }

    override fun push(value: N) {
        stack.push(value)
        map[value.fir] = value
    }

    operator fun get(key: T): N? {
        return map[key]
    }

    override fun reset() {
        stack.reset()
        map.clear()
    }

    override fun all(): List<N> = stack.all()
}

class SymbolBasedNodeStorage<T, N : CFGNode<T>> : Stack<N>() where T : FirElement {
    private val stack: Stack<N> = StackImpl()
    private val map: MutableMap<FirBasedSymbol<*>, N> = mutableMapOf()

    override val size: Int get() = stack.size

    override fun top(): N = stack.top()

    override fun pop(): N = stack.pop().also {
        map.remove((it.fir as FirDeclaration).symbol)
    }

    override fun push(value: N) {
        stack.push(value)
        map[(value.fir as FirDeclaration).symbol] = value
    }

    operator fun get(key: FirBasedSymbol<*>): N? {
        return map[key]
    }

    override fun reset() {
        stack.reset()
        map.clear()
    }

    override fun all(): List<N> = stack.all()
}
