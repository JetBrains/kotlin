/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import java.lang.AutoCloseable

class BirForest {
    private val possiblyRootElements = mutableListOf<BirElementBase>()

    private val elementIndexSlots = arrayOfNulls<ElementsIndexSlot>(256)
    private var elementIndexSlotCount = 0
    private var registeredElementIndexSlotCount = 0
    private var elementClassifier: BirElementIndexClassifier? = null
    private var currentElementsIndexSlotIterator: ElementsIndexSlotIterator<*>? = null
    private var currentIndexSlot = 0
    private var bufferedElementWithInvalidatedIndex: BirElementBase? = null


    internal fun elementAttached(element: BirElementBase) {
        element.accept {
            attachElement(it as BirElementBase)
            it.walkIntoChildren()
        }
    }

    private fun attachElement(element: BirElementBase) {
        element.root = this
        element.updateLevel()
    }

    internal fun rootElementAttached(element: BirElementBase) {
        elementAttached(element)
        possiblyRootElements += element
    }

    internal fun elementDetached(element: BirElementBase) {
        element.accept {
            detachElement(it as BirElementBase)
            it.walkIntoChildren()
        }
    }

    private fun detachElement(element: BirElementBase) {
        element.root = null
        element.updateLevel()
    }


    private fun addElementToIndex(element: BirElementBase) {
        val classifier = elementClassifier ?: return
        val i = classifier.classify(element, currentIndexSlot + 1)
        if (i != 0) {
            if (element.indexSlot.toInt() != i) {
                removeElementFromIndex(element)
                val targetSlot = elementIndexSlots[i]!!
                targetSlot.add(element)
                element.indexSlot = i.toUByte()
            }
        } else {
            removeElementFromIndex(element)
        }
    }

    private fun removeElementFromIndex(element: BirElementBase) {
        element.indexSlot = 0u

        // Don't eagerly remove element from index slot, as it is too slow.
        //  But, when detaching a bigger subtree, maybe we can, instead of finding and
        //  removing each element individually, rather scan the list for detached elements.
        //  Maybe also formalize and leverage the invariant that sub-elements must appear later
        //  than their ancestor (so start scanning from the index of the root one).
    }

    internal fun elementIndexInvalidated(element: BirElementBase) {
        if (element !== bufferedElementWithInvalidatedIndex) {
            flushElementsWithInvalidatedIndexBuffer()
            bufferedElementWithInvalidatedIndex = element
        }
    }

    private fun flushElementsWithInvalidatedIndexBuffer() {
        bufferedElementWithInvalidatedIndex?.let {
            addElementToIndex(it)
        }
    }

    fun registerElementIndexingKey(key: BirElementsIndexKey<*>) {
        val i = ++registeredElementIndexSlotCount
        val slot = ElementsIndexSlot(i, key.condition, key.elementClass)
        elementIndexSlots[i] = slot
        key.index = i
    }

    fun applyNewRegisteredIndices() {
        if (registeredElementIndexSlotCount != elementIndexSlotCount) {
            elementIndexSlotCount = registeredElementIndexSlotCount

            val matchers = List(elementIndexSlotCount) {
                val slot = elementIndexSlots[it + 1]!!
                BirElementIndexClassifierFunctionGenerator.Matcher(slot.condition, slot.elementClass, it + 1)
            }
            elementClassifier = BirElementIndexClassifierFunctionGenerator.createClassifierFunction(matchers)
        }
    }

    fun reindexAllElements() {
        possiblyRootElements.retainAll { it.root == this && it.parent == null }
        for (root in possiblyRootElements) {
            root.accept { element ->
                addElementToIndex(element as BirElementBase)
                element.walkIntoChildren()
            }
        }
    }

    fun <E : BirElement> getElementsWithIndex(key: BirElementsIndexKey<E>): Iterator<E> {
        val cacheSlotIndex = key.index
        require(cacheSlotIndex == currentIndexSlot + 1)

        flushElementsWithInvalidatedIndexBuffer()

        currentElementsIndexSlotIterator?.let { iterator ->
            cancelElementsIndexSlotIterator(iterator)
        }

        currentIndexSlot++

        val slot = elementIndexSlots[cacheSlotIndex]!!

        val iter = ElementsIndexSlotIterator<E>(slot)
        currentElementsIndexSlotIterator = iter
        return iter
    }

    private fun cancelElementsIndexSlotIterator(iterator: ElementsIndexSlotIterator<*>) {
        iterator.close()
        currentElementsIndexSlotIterator = null
    }


    private inner class ElementsIndexSlot(
        val index: Int,
        val condition: BirElementIndexMatcher,
        val elementClass: Class<*>,
    ) {
        var array = emptyArray<BirElementBase?>()
            private set
        var size = 0
        var currentIterator: ElementsIndexSlotIterator<*>? = null

        fun add(element: BirElementBase) {
            var array = array
            val size = size

            if (array.isEmpty()) {
                for (i in 1..<currentIndexSlot) {
                    val slot = elementIndexSlots[i]!!
                    if (slot.array.size > size) {
                        // Steal a nice, preallocated and nulled-out array from some previous slot.
                        // It won't use it anyway.
                        array = slot.array
                        slot.array = emptyArray<BirElementBase?>()
                        break
                    }
                }

                if (array.isEmpty()) {
                    array = arrayOfNulls(8)
                }

                this.array = array
            } else if (size == array.size) {
                array = array.copyOf(size * 2)
                this.array = array
            }

            array[size] = element
            this.size = size + 1
        }
    }

    private inner class ElementsIndexSlotIterator<E : BirElement>(
        private val slot: ElementsIndexSlot,
    ) : Iterator<E>, AutoCloseable {
        private var canceled = false
        var mainListIdx = 0
            private set
        private var next: BirElementBase? = null

        override fun hasNext(): Boolean {
            if (next != null) return true
            val n = computeNext()
            next = n
            return n != null
        }

        override fun next(): E {
            val n = next
                ?: computeNext()
                ?: throw NoSuchElementException()
            next = null
            @Suppress("UNCHECKED_CAST")
            return n as E
        }

        private fun computeNext(): BirElementBase? {
            require(!canceled) { "Iterator was cancelled" }
            val array = slot.array

            while (true) {
                val idx = mainListIdx
                var element: BirElementBase? = null
                while (idx < slot.size) {
                    element = array[idx]!!
                    if (element.indexSlot.toInt() == slot.index) {
                        deregisterElement(array, idx)
                        break
                    } else {
                        val lastIdx = slot.size - 1
                        if (idx < lastIdx) {
                            array[idx] = array[lastIdx]
                        }
                        array[lastIdx] = null

                        slot.size--
                        element = null
                    }
                }

                if (element != null) {
                    mainListIdx++
                    return element
                } else {
                    mainListIdx = 0
                    slot.size = 0
                    return null
                }
            }
        }

        private fun deregisterElement(array: Array<BirElementBase?>, index: Int) {
            val last = array[index]!!
            array[index] = null
            last.indexSlot = 0u
            addElementToIndex(last)
        }

        override fun close() {
            for (i in mainListIdx..<slot.size) {
                deregisterElement(slot.array, i)
            }

            slot.size = 0
            canceled = true
        }
    }
}