/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.lazy

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.ir.IrElement
import java.util.*
import kotlin.concurrent.Volatile

class BirLazyChildElementList<E : BirElement?>(
    override val parent: BirLazyElementBase,
    id: Int,
    isNullable: Boolean,
    retrieveUpstreamList: BirLazyElementBase.() -> List<IrElement>,
) : BirChildElementList<E>(id, isNullable) {
    @Volatile
    private var upstreamList: List<IrElement>? = null
    private var retrieveUpstreamList: (BirLazyElementBase.() -> List<IrElement>)? = retrieveUpstreamList

    private val converter
        get() = parent.converter

    override val size: Int
        get() {
            queryUpstreamList()
            return _size
        }

    override fun get(index: Int): E {
        val upstreamList = queryUpstreamList()
        checkElementIndex(index, _size)

        // todo: more fine-grained locking
        synchronized(this) {
            @Suppress("UNCHECKED_CAST")
            var elementArray = elementArray as Array<BirElementBase?>?
            var element = if (!elementArray.isNullOrEmpty()) elementArray[index] else null
            if (element == null) {
                if (elementArray.isNullOrEmpty()) {
                    elementArray = arrayOfNulls<BirElementBase>(_size)
                    this.elementArray = elementArray
                }

                val originalElement = upstreamList[index]
                element = convertElement(originalElement)
                elementArray[index] = element
            }

            @Suppress("UNCHECKED_CAST")
            return element as E
        }
    }

    private fun convertElement(originalElement: IrElement): BirElementBase {
        val element = converter.remapElement<BirElementBase>(originalElement)
        element.setContainingList()
        parent.initChild(element)
        return element
    }

    private fun queryUpstreamList(): List<IrElement> {
        return upstreamList ?: synchronized(this) {
            upstreamList?.let {
                return it
            }

            retrieveUpstreamList!!(parent).also {
                retrieveUpstreamList = null
                upstreamList = it
                _size = it.size
            }
        }
    }


    override fun set(index: Int, element: E): E {
        BirLazyElementBase.mutationNotSupported()
    }

    override fun add(element: E): Boolean {
        BirLazyElementBase.mutationNotSupported()
    }

    override fun add(index: Int, element: E) {
        BirLazyElementBase.mutationNotSupported()
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        BirLazyElementBase.mutationNotSupported()
    }

    override fun addAll(elements: Collection<E>): Boolean {
        BirLazyElementBase.mutationNotSupported()
    }

    override fun removeAt(index: Int): E {
        BirLazyElementBase.mutationNotSupported()
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        BirLazyElementBase.mutationNotSupported()
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        BirLazyElementBase.mutationNotSupported()
    }

    override fun remove(element: E): Boolean {
        BirLazyElementBase.mutationNotSupported()
    }

    override fun clear() {
        BirLazyElementBase.mutationNotSupported()
    }


    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        val size = size
        if (size == 0) return

        for (i in 0..<size) {
            val element = this[i]
            element?.accept(data, visitor)
        }
    }

    override fun iterator(): MutableIterator<E> {
        queryUpstreamList()
        return IteratorImpl(this)
    }

    override fun listIterator(): MutableListIterator<E> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): MutableListIterator<E> {
        TODO("Not yet implemented")
    }

    override fun spliterator(): Spliterator<E> {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        TODO("Not yet implemented")
    }

    private class IteratorImpl<E : BirElement?>(
        private val list: BirLazyChildElementList<E>,
    ) : MutableIterator<E> {
        private var index: Int = 0

        override fun hasNext(): Boolean {
            return index < list._size
        }

        override fun next(): E {
            val i = index
            val next = list[i]
            index = i + 1
            return next
        }

        override fun remove() {
            BirLazyElementBase.mutationNotSupported()
        }
    }
}