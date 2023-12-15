/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

class BirImplChildElementList<E : BirElement?>(
    override val parent: BirImplElementBase,
    id: Int,
    isNullable: Boolean,
) : BirChildElementList<E>(id, isNullable) {

    override val size: Int
        get() {
            recordRead()
            return _size
        }

    override fun get(index: Int): E {
        recordRead()
        return super.get(index)
    }

    override fun contains(element: E): Boolean {
        recordRead()
        return super.contains(element)
    }

    override fun indexOf(element: E): Int {
        recordRead()
        return super.indexOf(element)
    }

    override fun lastIndexOf(element: E): Int {
        recordRead()
        return super.lastIndexOf(element)
    }


    override fun set(index: Int, element: E): E {
        val old = super.get(index) as BirElementBase?
        if (element !== old) {
            parent.childReplaced(old, element)
            setInternal(index, element as BirElementBase?, old)
            invalidate()
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
        invalidate()

        return true
    }

    override fun add(index: Int, element: E) {
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
        invalidate()
    }

    override fun addAll(elements: Collection<E>): Boolean = addAll(_size, elements)

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        if (elements.isEmpty()) {
            return false
        }

        checkElementIndex(index, _size + 1)
        val newElementsSize = elements.size
        val currentSize = _size
        val newSize = currentSize + newElementsSize
        var elementArray = elementArray
        if (elementArray is Array<*>) {
            @Suppress("UNCHECKED_CAST")
            elementArray as Array<BirElementBase?>
            if (elementArray.size <= newSize) {
                val newArray = arrayOfNulls<BirElementBase?>(getNewCapacity(newSize))
                elementArray.copyInto(newArray, 0, 0, index)
                elementArray.copyInto(newArray, index + newElementsSize, index, currentSize)
                elementArray = newArray
                this.elementArray = elementArray
            } else {
                elementArray.copyInto(elementArray, index + newElementsSize, index, currentSize)
            }
        } else {
            if (newElementsSize == 1 && index == 0) {
                this.elementArray = elements.iterator().next()
            } else {
                val newArray = arrayOfNulls<BirElementBase?>(getNewCapacity(newSize))
                if (currentSize == 1) {
                    newArray[if (index == 0) newElementsSize else 0] = elementArray as BirElementBase
                }
                elementArray = newArray
                this.elementArray = elementArray
            }
        }

        if (elementArray is Array<*>) {
            @Suppress("UNCHECKED_CAST")
            elementArray as Array<BirElementBase?>

            var i = index
            for (element in elements) {
                checkNewElement(element)

                parent.childReplaced(null, element)

                elementArray[i++] = element as BirElementBase?
                element?.setContainingList()
            }
        }

        _size = newSize
        invalidate()

        return true
    }

    internal fun resetWithNulls(count: Int) {
        assert(isNullable) { "Cannot reset not-nullable list with nulls" }

        clear()

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
        invalidate()
    }

    override fun removeAt(index: Int): E {
        val element = removeAtInternal(index)
        parent.childReplaced(element, null)
        invalidate()
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
        val index = indexOf(element)
        if (index != -1) {
            removeAt(index)
            return true
        }
        return false
    }

    internal fun removeInternal(element: BirElementBase): Boolean {
        @Suppress("UNCHECKED_CAST")
        val index = indexOf(element as E)
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
        val index = indexOf(old as E)
        if (index == -1) {
            return false
        }

        setInternal(index, new, super.get(index) as BirElementBase?)
        return true
    }

    override fun clear() {
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
        invalidate()
    }

    fun ensureCapacity(capacity: Int) {
        val elementArray = elementArray
        if (capacity > 1 && capacity > getCurrentCapacity()) {
            val newArray = arrayOfNulls<BirElementBase?>(getNewCapacity(capacity))
            if (elementArray is Array<*>) {
                (elementArray as Array<BirElementBase?>).copyInto(newArray, endIndex = _size)
            } else {
                newArray[0] = elementArray as BirElementBase?
            }
            this.elementArray = newArray
        }
    }


    private fun recordRead() {
        parent.recordPropertyRead(id)
    }

    private fun invalidate() {
        parent.invalidate(id)
    }


    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
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
        recordRead()
        return IteratorImpl<E>(this)
    }

    override fun listIterator(): MutableListIterator<E> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): MutableListIterator<E> {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        TODO("Not yet implemented")
    }


    private class IteratorImpl<E : BirElement?>(
        private val list: BirImplChildElementList<E>,
    ) : MutableIterator<E> {
        private var index: Int = 0

        override fun hasNext(): Boolean {
            return index < list._size
        }

        override fun next(): E {
            val i = index

            @Suppress("UNCHECKED_CAST")
            val next = when (val elementArray = list.elementArray) {
                is Array<*> -> elementArray[i] as E
                else -> elementArray as E
            }
            index = i + 1
            return next
        }

        override fun remove() {
            list.removeAt(--index)
        }
    }
}

fun <E : BirElement> BirChildElementList<E?>.resetWithNulls(count: Int) {
    (this as BirImplChildElementList<*>).resetWithNulls(count)
}