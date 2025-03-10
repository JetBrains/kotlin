/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

open class IrChildElementList<E : IrElement?>(
    protected val parent: IrElementBase,
) : AbstractMutableList<E>() {
    private val backingList = ArrayList<E>()

    override val size: Int
        get() = backingList.size

    override fun get(index: Int): E = backingList[index]

    override fun contains(element: E): Boolean {
        element as IrElementBase?
        if (element?.structuralParent !== parent) return false
        return backingList.contains(element)
    }

    override fun set(index: Int, element: E): E {
        val old = backingList.set(index, element)
        parent.childReplaced(old, element)
        return old
    }

    override fun add(element: E): Boolean {
        parent.childReplaced(null, element)
        return backingList.add(element)
    }

    override fun add(index: Int, element: E) {
        parent.childReplaced(null, element)
        backingList.add(index, element)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        for (element in elements) {
            parent.childReplaced(null, element)
        }
        return backingList.addAll(elements)
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        for (element in elements) {
            parent.childReplaced(null, element)
        }
        return backingList.addAll(index, elements)
    }

    override fun remove(element: E): Boolean {
        val removed = backingList.remove(element)
        if (removed) {
            parent.childReplaced(element, null)
        }
        return removed
    }

    override fun removeAt(index: Int): E {
        val removed = backingList.removeAt(index)
        parent.childReplaced(removed, null)
        return removed
    }

    override fun clear() {
        for (element in this) {
            parent.childReplaced(element, null)
        }
        backingList.clear()
    }
}