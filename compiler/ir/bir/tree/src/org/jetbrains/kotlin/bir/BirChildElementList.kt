/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

class BirChildElementList<E : BirElement?>(
    internal val parent: BirElementBase,
    id: Int,
) : AbstractMutableList<E>(), BirElementOrChildList {
    private var elementArray: Array<BirElementBase?> = EMPTY_ELEMENT_ARRAY
    private var sizeAndId: Int = id shl (32 - ID_BITS)

    override var size: Int
        get() = sizeAndId and SIZE_MASK
        private set(value) {
            sizeAndId = value or (sizeAndId and ID_MASK)
        }

    internal val id: Int
        get() = sizeAndId.toInt() shr (32 - ID_BITS)

    override fun get(index: Int): E {
        checkElementIndex(index, size)
        @Suppress("UNCHECKED_CAST")
        return elementArray[index] as E
    }

    override fun contains(element: E): Boolean {
        return element != null && element.parent === parent && (element as BirElementBase).containingListId.toInt() == id
    }

    override fun indexOf(element: E): Int {
        if (element != null && element !in this) {
            return -1
        }

        for (index in 0..<size) {
            if (this[index] === element) {
                return index
            }
        }
        return -1
    }

    override fun lastIndexOf(element: E): Int {
        if (element != null && element !in this) {
            return -1
        }

        for (index in size - 1 downTo 0) {
            if (this[index] === element) {
                return index
            }
        }
        return -1
    }

    override fun set(index: Int, element: E): E {
        checkElementIndex(index, size)
        element as BirElementBase?
        val old = elementArray[index]
        if (element !== old) {
            replaceChild(old, element)
            element?.containingListId = id.toByte()
            elementArray[index] = element
            invalidate()
        }
        return element
    }

    override fun add(index: Int, element: E) {
        checkElementIndex(index, size + 1)
        element as BirElementBase?

        var elementArray = elementArray
        val newSize = size + 1
        if (elementArray.size == newSize) {
            val newArray = arrayOfNulls<BirElementBase?>(getNewCapacity(newSize))
            elementArray.copyInto(newArray, 0, 0, index)
            elementArray.copyInto(newArray, index + 1, index, size)
            elementArray = newArray
            this.elementArray = elementArray
        }
        addChild(element)
        elementArray[index] = element
        size = newSize
        invalidate()
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        if (elements.isEmpty()) {
            return false
        }

        checkElementIndex(index, size + 1)
        var elementArray = elementArray
        val newSize = size + elements.size
        if (elementArray.size <= newSize) {
            val newArray = arrayOfNulls<BirElementBase?>(getNewCapacity(newSize))
            elementArray.copyInto(newArray, 0, 0, index)
            elementArray.copyInto(newArray, index + elements.size, index, size)
            elementArray = newArray
            this.elementArray = elementArray
        }

        var i = index
        for (element in elements) {
            elementArray[i++] = element as BirElementBase?
            addChild(element)
        }
        size = newSize
        invalidate()

        return true
    }

    fun ensureCapacity(capacity: Int) {
        if (elementArray.size <= capacity) {
            val newArray = arrayOfNulls<BirElementBase?>(getNewCapacity(capacity))
            elementArray.copyInto(newArray, endIndex = size)
            elementArray = newArray
        }
    }

    private fun getNewCapacity(minimumCapacity: Int) = maxOf(minimumCapacity, elementArray.size * 2, 4)

    override fun removeAt(index: Int): E {
        checkElementIndex(index, size)
        val elementArray = elementArray
        val element = elementArray[index]
        replaceChild(element, null)
        elementArray.copyInto(elementArray, index, index + 1, size)
        invalidate()
        @Suppress("UNCHECKED_CAST")
        return element as E
    }

    override fun remove(element: E): Boolean {
        val index = indexOf(element)
        if (index != -1) {
            removeAt(index)
            return true
        }
        return false
    }

    fun replace(old: E, new: E): Boolean {
        if (new === old) {
            return true
        }

        val index = indexOf(old)
        if (index != -1) {
            this[index] = new
            return true
        }
        return false
    }

    override fun clear() {
        if (size == 0) {
            return
        }

        val elementArray = elementArray
        for (i in 0..<size) {
            val element = elementArray[i]
            elementArray[i] = null
            replaceChild(element, null)
        }
        size = 0
        invalidate()
    }

    private fun addChild(new: BirElementBase?) {
        parent.replaceChild(null, new)
        new?.containingListId = id.toByte()
    }

    private fun replaceChild(old: BirElementBase?, new: BirElementBase?) {
        parent.replaceChild(old, new)
        old?.containingListId = 0
        new?.containingListId = id.toByte()
    }

    private fun invalidate() {
        parent.invalidate()
    }

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        for (i in 0..<size) {
            val element = elementArray[i]
            element?.accept(data, visitor)
        }
    }

    companion object {
        private val EMPTY_ELEMENT_ARRAY = emptyArray<BirElementBase?>()
        private const val ID_BITS = 3
        private const val SIZE_MASK: Int = (-1 ushr (32 + ID_BITS))
        private const val ID_MASK: Int = (-1 shl (32 - ID_BITS))

        private fun checkElementIndex(index: Int, size: Int) {
            if (index < 0 || index >= size) {
                throw IndexOutOfBoundsException("index: $index, size: $size")
            }
        }
    }
}