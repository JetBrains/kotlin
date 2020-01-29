/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

abstract class Stack<T> {
    abstract val size: Int
    abstract fun top(): T
    abstract fun pop(): T
    abstract fun push(value: T)
}

fun <T> stackOf(vararg values: T): Stack<T> = StackImpl(*values)
val Stack<*>.isEmpty: Boolean get() = size == 0
val Stack<*>.isNotEmpty: Boolean get() = size != 0
fun <T> Stack<T>.topOrNull(): T? = if (size == 0) null else top()

private class StackImpl<T>(vararg values: T) : Stack<T>() {
    private val stack = mutableListOf(*values)

    override fun top(): T = stack[stack.size - 1]
    override fun pop(): T = stack.removeAt(stack.size - 1)

    override fun push(value: T) {
        stack.add(value)
    }

    override val size: Int get() = stack.size
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
}

class SymbolBasedNodeStorage<T, N : CFGNode<T>> : Stack<N>() where T : FirElement {
    private val stack: Stack<N> = StackImpl()
    private val map: MutableMap<FirBasedSymbol<*>, N> = mutableMapOf()

    override val size: Int get() = stack.size

    override fun top(): N = stack.top()

    override fun pop(): N = stack.pop().also {
        map.remove((it.fir as FirSymbolOwner<*>).symbol)
    }

    override fun push(value: N) {
        stack.push(value)
        map[(value.fir as FirSymbolOwner<*>).symbol] = value
    }

    operator fun get(key: FirBasedSymbol<*>): N? {
        return map[key]
    }
}