/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.ir.visitors.IrVisitor
import java.util.Spliterator
import java.util.function.Predicate
import java.util.function.UnaryOperator

open class IrChildElementList<E : IrElement?>(
    protected val parent: IrElementBase,
) : ArrayList<E>() {
    override fun contains(o: E): Boolean {
        o as IrElementBase?
        if (o?.structuralParent !== parent) return false
        return super.contains(o)
    }

    override fun set(index: Int, element: E): E {
        val old = super.set(index, element)
        parent.childReplaced(old, element)
        return old
    }

    override fun add(e: E): Boolean {
        parent.childReplaced(null, e)
        return super.add(e)
    }

    override fun add(index: Int, element: E) {
        parent.childReplaced(null, element)
        super.add(index, element)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        for (element in elements) {
            parent.childReplaced(null, element)
        }
        return super.addAll(elements)
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        for (element in elements) {
            parent.childReplaced(null, element)
        }
        return super.addAll(index, elements)
    }

    override fun remove(o: E): Boolean {
        val removed = super.remove(o)
        if (removed) {
            parent.childReplaced(o, null)
        }
        return removed
    }

    override fun removeAt(index: Int): E {
        val removed = super.removeAt(index)
        parent.childReplaced(removed, null)
        return removed
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        for (i in fromIndex ..< toIndex) {
            parent.childReplaced(this[i], null)
        }
        super.removeRange(fromIndex, toIndex)
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        var removedAny = false
        for (i in lastIndex downTo 0) {
            val element = this[i]
            if (element in elements) {
                removeAt(i)
                removedAny = true
            }
        }
        return removedAny
    }

    override fun retainAll(c: Collection<E>): Boolean {
        var removedAny = false
        for (i in lastIndex downTo 0) {
            val element = this[i]
            if (element !in c) {
                removeAt(i)
                removedAny = true
            }
        }
        return removedAny
    }

    override fun replaceAll(operator: UnaryOperator<E>) {
        super.replaceAll { old ->
            val new = operator.apply(old)
            parent.childReplaced(old, new)
            new
        }
    }

    override fun removeIf(filter: Predicate<in E>): Boolean {
        return super.removeIf {
            val remove = filter.test(it)
            if (remove) {
                parent.childReplaced(it, null)
            }
            remove
        }
    }

    override fun clear() {
        for (element in this) {
            parent.childReplaced(element, null)
        }
        super.clear()
    }

    override fun clone(): Any = TODO()
}