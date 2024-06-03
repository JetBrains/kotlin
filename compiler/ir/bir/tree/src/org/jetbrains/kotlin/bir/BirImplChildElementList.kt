/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import java.util.*

/**
 * A regular, mutable implementation of [BirChildElementList].
 * It generally mimics the [java.util.ArrayList] class, except
 * for additional logic required for handling structural changes
 * inside BIR tree, mainly tracking the parent <-> child relationship.
 *
 * ### Concurrent modification
 * This class follows the same principles as [java.util.ArrayList] -
 * it is not synchronized, and it must not be structurally changed
 * during iteration, except when using the methods on the iterator
 * itself. (A structural modification is any operation that adds or
 * deletes one or more elements, or explicitly resizes the backing
 * array; merely setting the value of an element is not a structural
 * modification.)
 *
 * Violations of these rules are detected on a fail-fast, best-effort
 * basis.
 */
class BirImplChildElementList<E : BirElement?>(
    override val parent: BirImplElementBase,
    id: Int,
    isNullable: Boolean,
) : BirChildElementList<E>(id, isNullable) {
    override val size: Int
        get() = _size

    override fun set(index: Int, element: E): E {
        val old = super.get(index) as BirElementBase?
        if (element !== old) {
            parent.childReplaced(old, element)
            setInternal(index, element as BirElementBase?, old)
        }
        @Suppress("UNCHECKED_CAST")
        return old as E
    }

    private fun setInternal(index: Int, element: BirElementBase?, old: BirElementBase?) {
        checkNewElement(element)

        when (val elementArray = elementArray) {
            is Array<*> -> {
                @Suppress("UNCHECKED_CAST")
                (elementArray as Array<BirElementBase?>)[index] = element
            }
            else -> {
                this.elementArray = element
            }
        }
        old?.resetContainingList()
        element?.setContainingList()
    }

    override fun add(element: E): Boolean {
        modCount++
        checkNewElement(element)
        element as BirElementBase?

        parent.childReplaced(null, element)

        val currentSize = _size
        val newSize = currentSize + 1
        var elementArray = elementArray
        if (elementArray is Array<*>) {
            @Suppress("UNCHECKED_CAST")
            elementArray as Array<BirElementBase?>

            if (elementArray.size <= newSize) {
                elementArray = elementArray.copyOf(getNewCapacity(newSize))
                this.elementArray = elementArray
            }
            elementArray[currentSize] = element
        } else {
            if (currentSize == 0) {
                this.elementArray = element
            } else {
                val newArray = arrayOfNulls<BirElementBase?>(getNewCapacity(newSize))
                if (currentSize == 1) {
                    newArray[0] = elementArray as BirElementBase
                }
                newArray[currentSize] = element
                elementArray = newArray
                this.elementArray = elementArray
            }
        }

        _size = newSize
        element?.setContainingList()

        return true
    }

    override fun add(index: Int, element: E) {
        modCount++
        val currentSize = _size
        val newSize = currentSize + 1
        checkElementIndex(index, newSize)
        checkNewElement(element)
        element as BirElementBase?

        parent.childReplaced(null, element)

        var elementArray = elementArray
        if (elementArray is Array<*>) {
            @Suppress("UNCHECKED_CAST")
            elementArray as Array<BirElementBase?>

            if (elementArray.size <= newSize) {
                val newArray = arrayOfNulls<BirElementBase?>(getNewCapacity(newSize))
                elementArray.copyInto(newArray, 0, 0, index)
                elementArray.copyInto(newArray, index + 1, index, currentSize)
                elementArray = newArray
                this.elementArray = elementArray
            } else {
                elementArray.copyInto(elementArray, index + 1, index, currentSize)
            }
            elementArray[index] = element
        } else {
            if (currentSize == 0 && index == 0) {
                this.elementArray = element
            } else {
                val newArray = arrayOfNulls<BirElementBase?>(getNewCapacity(newSize))
                if (currentSize == 1) {
                    newArray[if (index == 0) 1 else 0] = elementArray as BirElementBase
                }
                newArray[index] = element
                elementArray = newArray
                this.elementArray = elementArray
            }
        }

        _size = newSize
        element?.setContainingList()
    }

    override fun addAll(elements: Collection<E>): Boolean = addAll(_size, elements)

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        modCount++
        if (elements.isEmpty()) {
            return false
        }

        checkElementIndex(index, _size + 1)
        val newElementsSize = elements.size
        val currentSize = _size
        val newSize = currentSize + newElementsSize

        @Suppress("UNCHECKED_CAST")
        var elementArray = elementArray as? Array<BirElementBase?>
        if (elementArray != null) {
            if (elementArray.size <= newSize) {
                val newArray = arrayOfNulls<BirElementBase?>(getNewCapacity(newSize))
                elementArray.copyInto(newArray, 0, 0, index)
                elementArray.copyInto(newArray, index + newElementsSize, index, currentSize)
                elementArray = newArray
                this.elementArray = elementArray
            } else {
                elementArray.copyInto(elementArray, index + newElementsSize, index, currentSize)
            }
        } else if (!(newElementsSize == 1 && index == 0)) {
            elementArray = arrayOfNulls<BirElementBase?>(getNewCapacity(newSize))
            this.elementArray = elementArray
        }

        var insertIdx = index
        for (element in elements) {
            checkNewElement(element as BirElementBase?)

            if (newElementsSize == 1 && index == 0) {
                this.elementArray = element
            } else {
                elementArray!![insertIdx++] = element
            }
        }

        for (i in index..<index + newElementsSize) {
            val element = if (elementArray != null) {
                elementArray[i]
            } else {
                this.elementArray as BirElementBase
            }

            parent.childReplaced(null, element)
            element?.setContainingList()
        }

        _size = newSize

        return true
    }

    internal fun resetWithNulls(count: Int) {
        assert(isNullable) { "Cannot reset not-nullable list with nulls" }

        clear()
        modCount++

        if (count <= 1) {
            elementArray = null
        } else {
            val elementArray = elementArray
            if (elementArray is Array<*> && elementArray.size == count) {
                @Suppress("UNCHECKED_CAST")
                elementArray as Array<BirElementBase?>
                elementArray.fill(null)
            } else {

                // Nullable arrays are expected to be rarely resized,
                // so allocate at the exact size.
                this.elementArray = arrayOfNulls<BirElementBase?>(count)
            }
        }

        _size = count
    }

    override fun removeAt(index: Int): E {
        modCount++
        val element = removeAtInternal(index)
        parent.childReplaced(element, null)
        @Suppress("UNCHECKED_CAST")
        return element as E
    }

    private fun removeAtInternal(index: Int): BirElementBase? {
        val size = _size
        checkElementIndex(index, size)

        val element: BirElementBase?
        val elementArray = elementArray
        if (elementArray is Array<*>) {
            @Suppress("UNCHECKED_CAST")
            elementArray as Array<BirElementBase?>

            element = elementArray[index]
            elementArray.copyInto(elementArray, index, index + 1, size)
        } else {
            element = elementArray as BirElementBase?
            this.elementArray = null
        }
        _size = size - 1

        element?.resetContainingList()
        return element
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun remove(element: E): Boolean {
        modCount++
        val index = indexOf(element)
        if (index != -1) {
            removeAt(index)
            return true
        }
        return false
    }

    internal fun removeInternal(element: BirElementBase): Boolean {
        modCount++
        @Suppress("UNCHECKED_CAST")
        val index = indexOfInternal(element as E, false)
        if (index != -1) {
            removeAtInternal(index)
            return true
        }
        return false
    }

    fun replace(old: E, new: E): Boolean {
        if (new === old) {
            return true
        }

        val index = indexOf(old)
        if (index == -1) {
            return false
        }

        this[index] = new
        return true
    }

    internal fun replaceInternal(old: BirElementBase, new: BirElementBase?): Boolean {
        @Suppress("UNCHECKED_CAST")
        val index = indexOfInternal(old as E, false)
        if (index == -1) {
            return false
        }

        setInternal(index, new, super.get(index) as BirElementBase?)
        return true
    }

    override fun clear() {
        modCount++
        if (_size == 0) {
            return
        }

        val elementArray = elementArray
        if (elementArray is Array<*>) {
            @Suppress("UNCHECKED_CAST")
            elementArray as Array<BirElementBase?>

            for (i in 0..<_size) {
                val element = elementArray[i]
                parent.childReplaced(element, null)
                elementArray[i] = null
                element?.resetContainingList()
            }
        } else {
            val element = elementArray as BirElementBase?
            this.elementArray = null
            element?.resetContainingList()
        }

        _size = 0
    }

    fun ensureCapacity(capacity: Int) {
        val elementArray = elementArray
        if (capacity > 1 && capacity > getCurrentCapacity()) {
            modCount++
            val newArray = arrayOfNulls<BirElementBase?>(getNewCapacity(capacity))
            if (elementArray is Array<*>) {
                @Suppress("UNCHECKED_CAST")
                (elementArray as Array<BirElementBase?>).copyInto(newArray, endIndex = _size)
            } else {
                newArray[0] = elementArray as BirElementBase?
            }
            this.elementArray = newArray
        }
    }


    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        val size = size
        if (size == 0) return

        when (val elementArray = elementArray) {
            is Array<*> -> {
                @Suppress("UNCHECKED_CAST")
                elementArray as Array<BirElementBase?>
                for (i in 0..<size) {
                    val element = elementArray[i]
                    element?.accept(data, visitor)
                }
            }
            else -> {
                (elementArray as BirElementBase?)?.accept(data, visitor)
            }
        }
    }

    internal fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        val size = _size
        if (size == 0) return

        val scope = BirElementVisitorScopeLite(visitor)
        when (val elementArray = elementArray) {
            is Array<*> -> {
                @Suppress("UNCHECKED_CAST")
                elementArray as Array<BirElementBase?>
                for (i in 0..<size) {
                    val element = elementArray[i]
                    if (element != null) {
                        visitor.invoke(scope, element)
                    }
                }
            }
            else -> {
                val element = elementArray as BirElementBase?
                if (element != null) {
                    visitor.invoke(scope, element)
                }
            }
        }
    }

    override fun iterator(): MutableIterator<E> {
        return IteratorImpl<E>(this)
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
        private val list: BirImplChildElementList<E>,
    ) : MutableIterator<E> {
        private var expectedModCount = list.modCount

        @Suppress("UNCHECKED_CAST")
        private val elementArray = (list.elementArray as? Array<BirElementBase?>)
        private var index: Int = 0

        override fun hasNext(): Boolean {
            return index < list._size
        }

        override fun next(): E {
            checkForComodification()
            val i = index

            val elementArray = elementArray
            val next = if (elementArray != null) {
                elementArray[i]
            } else {
                list.elementArray as BirElementBase?
            }

            index = i + 1
            @Suppress("UNCHECKED_CAST")
            return next as E
        }

        override fun remove() {
            checkForComodification()
            list.removeAt(--index)
            expectedModCount = list.modCount
        }

        private fun checkForComodification() {
            if (list.modCount != expectedModCount) {
                throw ConcurrentModificationException()
            }
        }
    }
}

fun <E : BirElement> BirChildElementList<E?>.resetWithNulls(count: Int) {
    (this as BirImplChildElementList<*>).resetWithNulls(count)
}