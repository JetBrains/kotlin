/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.bir.util.ancestors
import org.jetbrains.kotlin.bir.util.countAllElementsInTree
import java.lang.AutoCloseable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class BirForest : BirElementParent() {
    private val possiblyRootElements = mutableListOf<BirElementBase>()

    private val elementIndexSlots = arrayOfNulls<ElementsIndexSlot>(256)
    private var elementIndexSlotCount = 0
    private val registeredIndexers = mutableListOf<BirElementGeneralIndexerKey>()
    private var elementClassifier: BirElementIndexClassifier? = null
    private var currentElementsIndexSlotIterator: ElementsIndexSlotIterator<*>? = null
    private var currentIndexSlot = 0
    private var bufferedElementWithInvalidatedIndex: BirElementBase? = null
    internal var mutableElementCurrentlyBeingClassified: BirImplElementBase? = null
        private set

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
        val oldParent = element._parent
        if (oldParent != null) {
            element as BirImplElementBase
            element.replacedWithInternal(null)
            element.setParentWithInvalidation(this)
            if (oldParent is BirElementBase) {
                (oldParent as BirImplElementBase).invalidate()
            }

            elementMoved(element, oldParent)
        } else {
            element.setParentWithInvalidation(this)
            elementAttached(element)
        }

        possiblyRootElements += element
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

    internal fun elementMoved(element: BirElementBase, oldParent: BirElementParent) {
        // Currently there is nothing to do here. So... yay :)
    }

    private fun markElementDirtyInSubtreeShuffleTransaction(element: BirElementBase) {
        if (!element.hasFlag(BirElementBase.FLAG_MARKED_DIRTY_IN_SUBTREE_SHUFFLE_TRANSACTION)) {
            element.setFlag(BirElementBase.FLAG_MARKED_DIRTY_IN_SUBTREE_SHUFFLE_TRANSACTION, true)
            dirtyElementsInsideSubtreeShuffleTransaction += element
        }
    }

    private fun getActualRootElements(): List<BirElementBase> {
        possiblyRootElements.retainAll { it._parent === this }
        return possiblyRootElements
    }

    fun countAllElements(): Int {
        var count = 0
        val roots = getActualRootElements()
        for (root in roots) {
            count += root.countAllElementsInTree()
        }
        return count
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

        val backReferenceRecorder = BackReferenceRecorder()

        assert(mutableElementCurrentlyBeingClassified == null)
        if (element is BirImplElementBase) {
            mutableElementCurrentlyBeingClassified = element
        }
        val i = classifier.classify(element, currentIndexSlot + 1, backReferenceRecorder)
        mutableElementCurrentlyBeingClassified = null

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

        val recordedRef = backReferenceRecorder.recordedRef
        if (recordedRef != null && recordedRef.root === this) {
            recordedRef.registerBackReference(element)
        }
    }

    internal class BackReferenceRecorder() : BirElementBackReferenceRecorderScope {
        var recordedRef: BirElementBase? = null

        override fun recordReference(forwardRef: BirElement?) {
            if (forwardRef == null) return

            if (recordedRef == null) {
                recordedRef = forwardRef as BirElementBase
            } else {
                if (recordedRef !== forwardRef)
                    TODO("multiple forward refs for element")
            }
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

    internal val isInsideElementClassification: Boolean
        get() = mutableElementCurrentlyBeingClassified != null

    internal fun elementIndexInvalidated(element: BirElementBase) {
        if (element !== bufferedElementWithInvalidatedIndex) {
            flushElementsWithInvalidatedIndexBuffer()
            bufferedElementWithInvalidatedIndex = element
        }
    }

    private fun flushElementsWithInvalidatedIndexBuffer() {
        bufferedElementWithInvalidatedIndex?.let {
            addElementToIndex(it)
            (it as? BirImplElementBase)?.invalidateDependentElements()
            bufferedElementWithInvalidatedIndex = null
        }
    }

    fun registerElementIndexingKey(key: BirElementsIndexKey<*>) {
        registeredIndexers += key
    }

    fun registerElementBackReferencesKey(key: BirElementBackReferencesKey<*>) {
        registeredIndexers += key
    }

    fun applyNewRegisteredIndices() {
        require(!isInsideSubtreeShuffleTransaction)

        if (registeredIndexers.size != elementIndexSlotCount) {
            val indexers = registeredIndexers.mapIndexed { i, indexer ->
                val index = i + 1
                when (indexer) {
                    is BirElementsIndexKey<*> -> {
                        indexer.index = index
                        val slot = ElementsIndexSlot(index, indexer.condition, indexer.elementClass)
                        elementIndexSlots[index] = slot
                        BirElementIndexClassifierFunctionGenerator.Indexer(
                            BirElementGeneralIndexer.Kind.IndexMatcher,
                            indexer.condition,
                            indexer.elementClass,
                            index
                        )
                    }
                    is BirElementBackReferencesKey<*> -> {
                        BirElementIndexClassifierFunctionGenerator.Indexer(
                            BirElementGeneralIndexer.Kind.BackReferenceRecorder,
                            indexer.recorder,
                            indexer.elementClass,
                            index
                        )
                    }
                }
            }

            elementClassifier = BirElementIndexClassifierFunctionGenerator.createClassifierFunction(indexers)
            elementIndexSlotCount = registeredIndexers.size
        }
    }

    fun reindexAllElements() {
        require(!isInsideSubtreeShuffleTransaction)

        val roots = getActualRootElements()
        for (root in roots) {
            root.acceptLite { element ->
                addElementToIndex(element)
                element.walkIntoChildren()
            }
        }
    }

    fun <E : BirElement> getElementsWithIndex(key: BirElementsIndexKey<E>): Iterator<E> {
        require(!isInsideSubtreeShuffleTransaction)

        flushElementsWithInvalidatedIndexBuffer()

        currentElementsIndexSlotIterator?.let { iterator ->
            cancelElementsIndexSlotIterator(iterator)
        }

        while (elementIndexSlots[++currentIndexSlot] == null) {
            // nothing
        }

        val cacheSlotIndex = key.index
        require(cacheSlotIndex == currentIndexSlot)
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
        val condition: BirElementIndexMatcher?,
        val elementClass: Class<*>,
    ) {
        var array = emptyArray<BirElementBase?>()
            private set
        var size = 0

        fun add(element: BirElementBase) {
            var array = array
            val size = size

            if (array.isEmpty()) {
                array = acquireNewArray(size)
                this.array = array
            } else if (size == array.size) {
                array = array.copyOf(size * 2)
                this.array = array
            }

            array[size] = element
            this.size = size + 1
        }

        private fun acquireNewArray(size: Int): Array<BirElementBase?> {
            for (i in 1..<currentIndexSlot) {
                val slot = elementIndexSlots[i] ?: continue
                if (slot.array.size > size) {
                    // Steal a nice, preallocated and nulled-out array from some previous slot.
                    // It won't use it anyway.
                    val array = slot.array
                    slot.array = emptyArray<BirElementBase?>()
                    return array
                }
            }

            return arrayOfNulls(8)
        }
    }

    private inner class ElementsIndexSlotIterator<E : BirElement>(
        private val slot: ElementsIndexSlot,
    ) : Iterator<E>, AutoCloseable {
        private var canceled = false
        var mainListIdx = 0
            private set
        private val slotIndex = slot.index
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

            // An operation after last computeNext might have invalidated
            // some element which we are about to yield here, so check for that.
            flushElementsWithInvalidatedIndexBuffer()

            while (true) {
                val idx = mainListIdx
                var element: BirElementBase? = null
                while (idx < slot.size) {
                    element = array[idx]!!
                    if (element.indexSlot.toInt() == slotIndex) {
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
                    // Element classification stops at the first successful match.
                    // Now that the element has matched this particular index, we always
                    // have to check whether it will also match some proceeding one.
                    elementIndexInvalidated(element)

                    mainListIdx++
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
            canceled = true
        }
    }
}