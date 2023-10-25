/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.bir.util.ancestors
import java.lang.AutoCloseable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class BirForest : BirElementParent() {
    private val possiblyRootElements = mutableListOf<BirElementBase>()

    private val elementIndexSlots = arrayOfNulls<ElementsIndexSlot>(256)
    private var elementIndexSlotCount = 0
    private var registeredElementIndexSlotCount = 0
    private var elementClassifier: BirElementIndexClassifier? = null
    private var currentElementsIndexSlotIterator: ElementsIndexSlotIterator<*>? = null
    private var currentIndexSlot = 0
    private var bufferedElementWithInvalidatedIndex: BirElementBase? = null
    private var elementCurrentlyBeingClassified: BirElementBase? = null

    private var isInsideSubtreeShuffleTransaction = false
    private val dirtyElementsInsideSubtreeShuffleTransaction = mutableListOf<BirElementBase>()

    internal fun elementAttached(element: BirElementBase) {
        if (!isInsideSubtreeShuffleTransaction) {
            element.acceptLite {
                if (it.root !== this@BirForest) {
                    attachElement(it)
                    it.walkIntoChildren()
                }
            }
        } else {
            markElementDirtyInSubtreeShuffleTransaction(element)
        }
    }

    private fun attachElement(element: BirElementBase) {
        element.root = this
        addElementToIndex(element)
    }

    fun attachRootElement(element: BirElementBase) {
        require(element._parent == null)
        require(element.root == null || element.root === this)

        possiblyRootElements += element
        element.setParentWithInvalidation(this)
        elementAttached(element)
    }

    internal fun elementDetached(element: BirElementBase) {
        if (!isInsideSubtreeShuffleTransaction) {
            if (element._parent === this) {
                element.setParentWithInvalidation(null)
            } else {
                assert(element._parent !is BirForest)
            }

            element.acceptLite {
                detachElement(it)
                it.walkIntoChildren()
            }
        } else {
            markElementDirtyInSubtreeShuffleTransaction(element)
        }
    }

    private fun detachElement(element: BirElementBase) {
        element.root = null
        removeElementFromIndex(element)
    }

    private fun markElementDirtyInSubtreeShuffleTransaction(element: BirElementBase) {
        if (!element.hasFlag(BirElementBase.FLAG_MARKED_DIRTY_IN_SUBTREE_SHUFFLE_TRANSACTION)) {
            element.setFlag(BirElementBase.FLAG_MARKED_DIRTY_IN_SUBTREE_SHUFFLE_TRANSACTION, true)
            dirtyElementsInsideSubtreeShuffleTransaction += element
        }
    }

    /**
     * Allows to efficiently move around big BIR subtrees within the forest.
     */
    @OptIn(ExperimentalContracts::class)
    fun subtreeShuffleTransaction(block: () -> Unit) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        require(!isInsideSubtreeShuffleTransaction)
        isInsideSubtreeShuffleTransaction = true
        block()
        isInsideSubtreeShuffleTransaction = false

        for (element in dirtyElementsInsideSubtreeShuffleTransaction) {
            element.setFlag(BirElementBase.FLAG_MARKED_DIRTY_IN_SUBTREE_SHUFFLE_TRANSACTION, false)

            val realRoot = element.findRealRootFromAncestors()
            val previousRoot = element.root
            if (realRoot === this) {
                if (previousRoot !== this) {
                    elementAttached(element)
                }
            } else {
                if (previousRoot === this) {
                    elementDetached(element)
                }
            }
        }
        dirtyElementsInsideSubtreeShuffleTransaction.clear()
    }

    private fun BirElementBase.findRealRootFromAncestors(): BirForest? {
        return ancestors(false).firstNotNullOfOrNull { it.root }
            ?: _parent as? BirForest
    }


    private fun addElementToIndex(element: BirElementBase) {
        val classifier = elementClassifier ?: return
        if (element.root !== this) return

        assert(elementCurrentlyBeingClassified == null)
        elementCurrentlyBeingClassified = element
        val i = classifier.classify(element, currentIndexSlot + 1)
        elementCurrentlyBeingClassified = null

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
            it.invalidateDependentElements()
        }
    }

    internal fun recordElementPropertyRead(element: BirElementBase) {
        if (elementCurrentlyBeingClassified != null && element.root === this) {
            element.registerDependentElement(elementCurrentlyBeingClassified!!)
        }
    }


    fun registerElementIndexingKey(key: BirElementsIndexKey<*>) {
        val i = ++registeredElementIndexSlotCount
        val slot = ElementsIndexSlot(i, key.condition, key.elementClass)
        elementIndexSlots[i] = slot
        key.index = i
    }

    fun applyNewRegisteredIndices() {
        require(!isInsideSubtreeShuffleTransaction)

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
        require(!isInsideSubtreeShuffleTransaction)

        possiblyRootElements.retainAll { it._parent === this }

        for (root in possiblyRootElements) {
            root.acceptLite { element ->
                addElementToIndex(element)
                element.walkIntoChildren()
            }
        }
    }

    fun <E : BirElement> getElementsWithIndex(key: BirElementsIndexKey<E>): Iterator<E> {
        require(!isInsideSubtreeShuffleTransaction)

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
        private var prev: BirElementBase? = null

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
                prev?.let {
                    addElementToIndex(it)
                }

                val idx = mainListIdx
                var element: BirElementBase? = null
                while (idx < slot.size) {
                    element = array[idx]!!
                    if (element.indexSlot.toInt() == slot.index) {
                        array[idx] = null
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
                    prev = element
                    return element
                } else {
                    mainListIdx = 0
                    slot.size = 0
                    return null
                }
            }
        }

        override fun close() {
            val array = slot.array
            for (i in maxOf(0, mainListIdx - 1)..<slot.size) {
                val element = array[i]!!
                array[i] = null
                addElementToIndex(element)
            }

            slot.size = 0
            next = null
            prev = null
            canceled = true
        }
    }
}