/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.bir.util.countAllElementsInTree
import java.lang.AutoCloseable

class BirForest : BirElementParent() {
    private val possiblyRootElements = mutableListOf<BirElementBase>()

    private val elementIndexSlots = arrayOfNulls<ElementsIndexSlot>(256)
    private var elementIndexSlotCount = 0
    private val registeredIndexers = mutableListOf<BirElementGeneralIndexerKey>()
    private val indexerIndexes = mutableMapOf<BirElementGeneralIndexerKey, Int>()
    private var elementClassifier: BirElementIndexClassifier? = null
    private var currentElementsIndexSlotIterator: ElementsIndexSlotIterator<*>? = null
    private var currentIndexSlot = 0
    internal var mutableElementCurrentlyBeingClassified: BirImplElementBase? = null
        private set

    private val elementsWithInvalidatedIndexBuffer = arrayOfNulls<BirElementBase>(64)
    private var elementsWithInvalidatedIndexBufferSize = 0

    private val movedElementBuffer = arrayOfNulls<BirElementBase>(64)
    private var movedElementBufferSize = 0

    internal fun elementAttached(element: BirElementBase) {
        element.acceptLite {
            when (it.root) {
                null -> {
                    // The element is likely new, and therefore likely to
                    // stay attached. Realize the attachment operation eagerly.
                    attachElement(it)
                    it.walkIntoChildren()
                }
                this@BirForest -> addToMovedElementsBuffer(it)
                else -> handleElementFromOtherForest()
            }
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
            val propertyId = element.replacedWithInternal(null)
            element.setParentWithInvalidation(this)
            (oldParent as? BirImplElementBase)?.invalidate(propertyId)

            elementMoved(element, oldParent)
        } else {
            element.setParentWithInvalidation(this)
            elementAttached(element)
        }

        possiblyRootElements += element
    }

    internal fun elementDetached(element: BirElementBase) {
        when (element.root) {
            null -> {
                // This element has not been attached, or its detachment
                //  has already been realized, so it should be (TODO: is)
                //  safe to ignore it here.
            }
            this -> addToMovedElementsBuffer(element)
            else -> handleElementFromOtherForest()
        }
    }

    private fun detachElement(element: BirElementBase) {
        element.root = null
        removeElementFromIndex(element)
    }

    internal fun elementMoved(element: BirElementBase, oldParent: BirElementParent) {
        if (element.root != null && element.root !== this) {
            handleElementFromOtherForest()
        }

        addToMovedElementsBuffer(element)
    }

    private fun addToMovedElementsBuffer(element: BirElementBase) {
        if (!element.hasFlag(BirElementBase.FLAG_IS_IN_MOVED_ELEMENTS_BUFFER)) {
            var size = movedElementBufferSize
            val buffer = movedElementBuffer
            if (size == buffer.size) {
                realizeTreeMovements()
                size = movedElementBufferSize
            }

            buffer[size] = element
            movedElementBufferSize = size + 1
            element.setFlag(BirElementBase.FLAG_IS_IN_MOVED_ELEMENTS_BUFFER, true)
        }
    }

    internal fun realizeTreeMovements() {
        val buffer = movedElementBuffer
        for (i in 0..<movedElementBufferSize) {
            val element = buffer[i]!!
            buffer[i] = null
            element.setFlag(BirElementBase.FLAG_IS_IN_MOVED_ELEMENTS_BUFFER, false)

            // perf: it may be possible to find out the actual forest reference faster
            //  than traversing up to the root each time.
            val actualRoot = element.findRootFromAncestors()
            val previousRoot = element.root

            if (actualRoot === this) {
                if (previousRoot !== this) {
                    // The element was not attached, but now it is.

                    element.acceptLite {
                        when (it.root) {
                            null -> {
                                attachElement(it)
                                it.walkIntoChildren()
                            }
                            this@BirForest -> {}
                            else -> handleElementFromOtherForest()
                        }
                    }
                }
            } else {
                if (previousRoot === this) {
                    // The element was attached, but now it isn't.

                    val parent = element._parent
                    if (parent is BirForest) {
                        // The element has been a root element in this forest.
                        if (parent === this) {
                            element.setParentWithInvalidation(null)
                        } else {
                            handleElementFromOtherForest()
                        }
                    }

                    element.acceptLite {
                        detachElement(it)
                        it.walkIntoChildren()
                    }
                }
            }
        }
        movedElementBufferSize = 0
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

    private fun handleElementFromOtherForest(): Nothing {
        // Once an element is attached to some forest, trying to
        //  attach it to some other forest instance is not supported,
        //  even after being removed from the former.
        //  This limitation can probably be removed in future by adding proper
        //  realization and handling of such a move, but right now
        //  this case is not anticipated to occur in the compilation flow.
        TODO("Handle element possibly coming from different forest")
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
        recordedRef?.registerBackReference(element)
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
        if (!element.hasFlag(BirElementBase.FLAG_IN_INVALIDATE_INDEX_BUFFER)) {
            var size = elementsWithInvalidatedIndexBufferSize
            val buffer = elementsWithInvalidatedIndexBuffer
            if (size == buffer.size) {
                flushElementsWithInvalidatedIndexBuffer()
                size = elementsWithInvalidatedIndexBufferSize
            }

            buffer[size] = element
            elementsWithInvalidatedIndexBufferSize = size + 1
            element.setFlag(BirElementBase.FLAG_IN_INVALIDATE_INDEX_BUFFER, true)
        }
    }

    internal fun flushElementsWithInvalidatedIndexBuffer() {
        realizeTreeMovements()

        val buffer = elementsWithInvalidatedIndexBuffer
        for (i in 0..<elementsWithInvalidatedIndexBufferSize) {
            val element = buffer[i]!!
            if (element.hasFlag(BirElementBase.FLAG_IN_INVALIDATE_INDEX_BUFFER)) {
                invalidateElement(element)
            }
            buffer[i] = null
        }
        elementsWithInvalidatedIndexBufferSize = 0
    }

    internal fun invalidateElement(element: BirElementBase) {
        addElementToIndex(element)
        (element as? BirImplElementBase)?.invalidateDependentElements()
        element.setFlag(BirElementBase.FLAG_IN_INVALIDATE_INDEX_BUFFER, false)
    }

    fun registerElementIndexingKey(key: BirElementsIndexKey<*>) {
        registeredIndexers += key
    }

    fun registerElementBackReferencesKey(key: BirElementBackReferencesKey<*, *>) {
        registeredIndexers += key
    }

    fun applyNewRegisteredIndices() {
        if (registeredIndexers.size != elementIndexSlotCount) {
            val indexers = registeredIndexers.mapIndexed { i, indexerKey ->
                val index = i + 1
                when (indexerKey) {
                    is BirElementsIndexKey<*> -> {
                        indexerIndexes[indexerKey] = index
                        val slot = ElementsIndexSlot(index, indexerKey.condition, indexerKey.elementClass)
                        elementIndexSlots[index] = slot
                        BirElementIndexClassifierFunctionGenerator.Indexer(
                            BirElementGeneralIndexer.Kind.IndexMatcher,
                            indexerKey.condition,
                            indexerKey.elementClass,
                            index
                        )
                    }
                    is BirElementBackReferencesKey<*, *> -> {
                        BirElementIndexClassifierFunctionGenerator.Indexer(
                            BirElementGeneralIndexer.Kind.BackReferenceRecorder,
                            indexerKey.recorder,
                            indexerKey.elementClass,
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
        realizeTreeMovements()

        val roots = getActualRootElements()
        for (root in roots) {
            root.acceptLite { element ->
                addElementToIndex(element)
                element.walkIntoChildren()
            }
        }
    }

    fun <E : BirElement> getElementsWithIndex(key: BirElementsIndexKey<E>): Sequence<E> {
        flushElementsWithInvalidatedIndexBuffer()

        currentElementsIndexSlotIterator?.let { iterator ->
            cancelElementsIndexSlotIterator(iterator)
        }

        while (elementIndexSlots[++currentIndexSlot] == null) {
            // nothing
        }

        val cacheSlotIndex = indexerIndexes.getValue(key)
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
    ) : Iterator<E>, Sequence<E>, AutoCloseable {
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
                    canceled = true
                    return null
                }
            }
        }

        override fun close() {
            val array = slot.array
            for (i in maxOf(0, mainListIdx - 1)..<slot.size) {
                val element = array[i]!!
                array[i] = null
                elementIndexInvalidated(element)
            }

            slot.size = 0
            next = null
            canceled = true
        }

        override fun iterator(): Iterator<E> {
            require(!canceled) { "Iterator was cancelled" }
            return this
        }
    }
}