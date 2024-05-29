/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import java.util.*
import kotlin.collections.AbstractList

abstract class BirChildElementList<E : BirElement?>(
    id: Int,
    internal val isNullable: Boolean,
) : AbstractList<E>(), MutableList<E>, BirElementOrChildList {
    protected var elementArray: Any? = null // Array<BirElementBase?> = EMPTY_ELEMENT_ARRAY
    protected var _size: Int = 0

    internal abstract val parent: BirElementBase
    internal val id: Byte = id.toByte()

    protected var modCount = 0

    init {
        require(id in 1..BirElementBase.MAX_CONTAINING_LIST_ID)
    }


    override fun get(index: Int): E {
        checkElementIndex(index, _size)
        @Suppress("UNCHECKED_CAST")
        return when (val elementArray = elementArray) {
            is Array<*> -> (elementArray as Array<BirElementBase?>)[index] as E
            else -> elementArray as E
        }
    }

    override fun contains(element: E): Boolean {
        return element != null && element.parent === parent && (element as BirElementBase).containingListId == id.toInt()
    }

    override fun indexOf(element: E): Int {
        if (element != null && element !in this) {
            return -1
        }

        return indexOfInternal(element, false)
    }

    override fun lastIndexOf(element: E): Int {
        if (element != null && element !in this) {
            return -1
        }

        return indexOfInternal(element, true)
    }

    protected fun indexOfInternal(element: E, searchBackward: Boolean): Int {
        val size = size
        when (val elementArray = elementArray) {
            is Array<*> -> {
                @Suppress("UNCHECKED_CAST")
                elementArray as Array<BirElementBase?>

                if (searchBackward) {
                    for (index in size - 1 downTo 0) {
                        if (elementArray[index] === element) {
                            return index
                        }
                    }
                } else {
                    for (index in 0..<size) {
                        if (elementArray[index] === element) {
                            return index
                        }
                    }
                }
            }
            else -> {
                if (elementArray === element) {
                    return 0
                }
            }
        }

        return -1
    }


    protected fun getNewCapacity(minimumCapacity: Int) =
        maxOf(minimumCapacity, getCurrentCapacity() * 2, 2)

    protected fun getCurrentCapacity(): Int = when (val elementArray = elementArray) {
        is Array<*> -> elementArray.size
        else -> 1
    }

    protected fun checkNewElement(new: BirElement?) {
        if (new == null && !isNullable) {
            throw IllegalArgumentException("Trying to add null element to a not-nullable list")
        }
    }

    protected fun BirElementBase.setContainingList() {
        containingListId = id.toInt()
    }

    protected fun BirElementBase.resetContainingList() {
        containingListId = 0
    }

    abstract override fun iterator(): MutableIterator<E>
    abstract override fun listIterator(): MutableListIterator<E>
    abstract override fun listIterator(index: Int): MutableListIterator<E>
    abstract override fun spliterator(): Spliterator<E>
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