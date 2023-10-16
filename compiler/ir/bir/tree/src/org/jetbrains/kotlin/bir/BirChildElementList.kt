/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

class BirChildElementList<E : BirElement?>(
    private val parent: BirElementBase,
) : AbstractMutableList<E>(), BirElementOrChildList {
    private var elementArray: Array<BirElementBase?> = EMPTY_ELEMENT_ARRAY
    override var size: Int = 0
        private set

    override fun get(index: Int): E {
        checkElementIndex(index, size)
        return elementArray[index] as E
    }

    override fun set(index: Int, element: E): E {
        checkElementIndex(index, size)
        element as BirElementBase?
        val old = elementArray[index]
        parent.replaceChild(old, element)
        elementArray[index] = element
        return element
    }

    override fun add(index: Int, element: E) {
        checkElementIndex(index, size + 1)
        element as BirElementBase?

        var elementArray = elementArray
        if (elementArray.size == size) {
            val newArray = arrayOfNulls<BirElementBase?>(if (elementArray.isEmpty()) 4 else elementArray.size * 2)
            elementArray.copyInto(newArray, 0, 0, index)
            elementArray.copyInto(newArray, index + 1, index, size)
            elementArray = newArray
            this.elementArray = elementArray
        }
        parent.initChild(element)
        elementArray[index] = element
        size++
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkElementIndex(index, size + 1)
        var elementArray = elementArray
        val newSize = size + elements.size
        if (elementArray.size <= newSize) {
            val newArray = arrayOfNulls<BirElementBase?>(
                if (elementArray.isEmpty()) newSize.coerceAtLeast(4)
                else newSize.coerceAtLeast(size * 2)
            )
            elementArray.copyInto(newArray, 0, 0, index)
            elementArray.copyInto(newArray, index + elements.size, index, size)
            elementArray = newArray
            this.elementArray = elementArray
        }

        var i = index
        for (element in elements) {
            elementArray[i++] = element as BirElementBase
            parent.initChild(element)
        }
        size = newSize

        return true
    }

    override fun removeAt(index: Int): E {
        checkElementIndex(index, size)
        val elementArray = elementArray
        val element = elementArray[index]
        parent.replaceChild(element, null)
        elementArray.copyInto(elementArray, index, index + 1, size)
        return element as E
    }

    override fun clear() {
        val elementArray = elementArray
        for (i in 0..<size) {
            val element = elementArray[i]
            elementArray[i] = null
            parent.replaceChild(element, null)
        }
        size = 0
    }

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        for (i in 0..<size) {
            val element = elementArray[i]
            element?.accept(data, visitor)
        }
    }

    companion object {
        private val EMPTY_ELEMENT_ARRAY = emptyArray<BirElementBase?>()

        private fun checkElementIndex(index: Int, size: Int) {
            if (index < 0 || index >= size) {
                throw IndexOutOfBoundsException("index: $index, size: $size")
            }
        }
    }
}