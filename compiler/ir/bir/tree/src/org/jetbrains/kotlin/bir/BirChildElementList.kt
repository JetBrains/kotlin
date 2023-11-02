/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

abstract class BirChildElementList<E : BirElement?>(
    id: Int,
    isNullable: Boolean,
) : AbstractList<E>(), MutableList<E>, BirElementOrChildList {
    internal abstract val parent: BirElementBase
    protected var elementArray: Array<BirElementBase?> = EMPTY_ELEMENT_ARRAY
    private var compressedData: Int = (if (isNullable) 1 shl SIZE_BITS else 0) or (id shl (SIZE_BITS + FLAG_BITS))

    protected var _size: Int
        get() = compressedData and SIZE_MASK
        set(value) {
            compressedData = value or (compressedData and SIZE_MASK.inv())
        }

    internal val id: Int
        get() = compressedData shr (SIZE_BITS + FLAG_BITS)

    internal val isNullable
        get() = compressedData and (1 shl SIZE_BITS) != 0


    override fun get(index: Int): E {
        checkElementIndex(index, _size)
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

        for (index in 0..<_size) {
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

        for (index in _size - 1 downTo 0) {
            if (this[index] === element) {
                return index
            }
        }
        return -1
    }



    fun ensureCapacity(capacity: Int) {
        if (elementArray.size <= capacity) {
            val newArray = arrayOfNulls<BirElementBase?>(getNewCapacity(capacity))
            elementArray.copyInto(newArray, endIndex = _size)
            elementArray = newArray
        }
    }

    protected fun getNewCapacity(minimumCapacity: Int) = maxOf(minimumCapacity, elementArray.size * 2, 4)
    protected fun checkNewElement(new: BirElement?) {
        if (new == null && !isNullable) {
            throw IllegalArgumentException("Trying to add null element to a not-nullable list")
        }
    }

    protected fun BirElementBase.setContainingList() {
        containingListId = id.toByte()
    }

    protected fun BirElementBase.resetContainingList() {
        containingListId = 0
    }

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        val size = size
        if (size == 0) return

        val elementArray = elementArray
        for (i in 0..<size) {
            val element = elementArray[i]
            element?.accept(data, visitor)
        }
    }

    internal fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        val size = _size
        if (size == 0) return

        val elementArray = elementArray
        val scope = BirElementVisitorScopeLite(visitor)
        for (i in 0..<size) {
            val element = elementArray[i]
            if (element != null) {
                visitor.invoke(scope, element)
            }
        }
    }

    abstract override fun iterator(): MutableIterator<E>
    abstract override fun listIterator(): MutableListIterator<E>
    abstract override fun listIterator(index: Int): MutableListIterator<E>
    abstract override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>

    companion object {
        @JvmStatic
        protected val EMPTY_ELEMENT_ARRAY = emptyArray<BirElementBase?>()

        private const val SIZE_BITS = 28
        private const val FLAG_BITS = 1
        private const val ID_BITS = 3

        private const val SIZE_MASK: Int = (-1 ushr (32 - SIZE_BITS))

        @JvmStatic
        protected fun checkElementIndex(index: Int, size: Int) {
            if (index < 0 || index >= size) {
                throw IndexOutOfBoundsException("index: $index, _size: $size")
            }
        }
    }
}