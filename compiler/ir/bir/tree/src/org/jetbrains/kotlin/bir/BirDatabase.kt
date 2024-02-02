/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.bir.util.ForwardReferenceRecorder

/**
 * A collection of [BirElement] trees.
 *
 * After adding an element to [BirDatabase], it enables usages of additional features,
 * such as fast retrieval of elements matching a given, predefined condition (indexing).
 *
 * ### Adding and removing elements from [BirDatabase]
 * [BirElement] may be added to [BirDatabase] either as a root of a new tree, by calling [attachRootElement],
 * or by attaching it as a child property to some other element, already present in the database.
 * The whole subtree represented by an element being added, i.e., it and all its child elements, recursively,
 * will be implicitly added to the database as well. However, in the case of lazy [BirElement]s only
 * already initialized child elements are added, so that the operation does not trigger any creation of new elements.
 *
 * The same way, [BirElement] may be removed from the database either by calling [BirElement.remove],
 * or by nulling-out property / removing from a list of child elements of its current parent element.
 *
 * Any given [BirElement] instance may be present in at most one [BirDatabase], and it may not move between databases.
 * This means that if an [BirElement] is attached to some [BirDatabase], even after being detached later on,
 * it may not be attached to any but the same [BirDatabase] instance.
 *
 * This limitation may be relaxed in future, however it should not be a problem w.r.t. the .
 *
 * ### Intended usage
 * It is expected that a [BirDatabase] instance is created for a whole Koltin module, or a group of modules,
 * e.g., one instance for a module being compiled, and another one for all the library modules it depends on.
 * However, it may also be used to support other use cases, such as a seperate [BirDatabase] for a script,
 * or a code fragment in a debugger.
 *
 * ### Thread safety
 * This class is _not_ thread-safe.
 */
class BirDatabase : BirElementParent() {
    private val possiblyRootElements = mutableListOf<BirElementBase>()

    private val elementIndexSlots: Array<ElementIndexSlot?>
    var indexElements = true

    var includeEntireSubtreeWhenAttachingElement = true
    var attachExternalReferencedElementTreeToOtherDatabase: ((BirElementBase) -> BirDatabase?)? = null

    private var lastQueryIndex: UByte = 0u

    private val movedElementBuffer = arrayOfNulls<BirElementBase>(64)
    private var movedElementBufferSize = 0

    init {
        val indexableElementClasses = BirMetadata.allElements.filter { it.hasImplementation }
        elementIndexSlots = arrayOfNulls<ElementIndexSlot>(indexableElementClasses.maxOf { it.id } + 1)
        for (elementClass in indexableElementClasses) {
            elementIndexSlots[elementClass.id] = ElementIndexSlot()
        }
    }

    internal fun elementAttached(element: BirElementBase) {
        element.acceptLite {
            when (it._containingDatabase) {
                null -> {
                    // The element is likely new, and therefore likely to
                    // stay attached. Realize the attachment operation eagerly.
                    attachElement(it)

                    // todo: Probably the easier solution would be to always attach a subtree,
                    //  but index only a single element.
                    if (includeEntireSubtreeWhenAttachingElement) {
                        it.walkIntoChildren()
                    }
                }
                this@BirDatabase -> addToMovedElementsBuffer(it)
                else -> handleElementFromOtherDatabase()
            }
        }
    }

    private fun attachElement(element: BirElementBase) {
        element._containingDatabase = this
        indexElement(element)
    }

    /**
     * Adds a tree of elements (an [element] and all its child elements, recursively)
     * to this database.
     * If [element] has a parent element, it is first detached from it (see [BirElement.remove]).
     *
     * @param element The root of an element tree to attach.
     */
    fun attachRootElement(element: BirElementBase) {
        element.moveElementToNewParent(this, this)
        possiblyRootElements += element
    }

    internal fun elementDetached(element: BirElementBase) {
        when (element._containingDatabase) {
            null -> {
                // This element has not been attached, or its detachment
                //  has already been realized, so it should be (TODO: is)
                //  safe to ignore it here.
            }
            this -> addToMovedElementsBuffer(element)
            else -> handleElementFromOtherDatabase()
        }
    }

    private fun detachElement(element: BirElementBase) {
        element._containingDatabase = null
        element.setFlag(BirElementBase.FLAG_HAS_VALID_FORWARD_REFERENCES_INDEX, false)
    }

    internal fun elementMoved(element: BirElementBase, oldParent: BirElementParent) {
        if (element._containingDatabase != null && element._containingDatabase !== this) {
            handleElementFromOtherDatabase()
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


    /**
     * Makes sure the internal state of [BirElementBase] and the indices are up-to-date,
     * after some elements are attached, detached, or moved within the database.
     */
    internal fun realizeTreeMovements() {
        val buffer = movedElementBuffer
        for (i in 0..<movedElementBufferSize) {
            val element = buffer[i]!!
            buffer[i] = null
            element.setFlag(BirElementBase.FLAG_IS_IN_MOVED_ELEMENTS_BUFFER, false)

            val actualDatabase = element.findActualContainingDatabaseAfterMove()
            val previousDatabase = element._containingDatabase

            if (actualDatabase === this) {
                if (previousDatabase !== this) {
                    // The element was not attached, but now it is.

                    element.acceptLite {
                        when (it._containingDatabase) {
                            null -> {
                                attachElement(it)
                                it.walkIntoChildren()
                            }
                            this@BirDatabase -> {}
                            else -> handleElementFromOtherDatabase()
                        }
                    }
                }
            } else {
                if (previousDatabase === this) {
                    // The element was attached, but now it isn't.

                    val parent = element._parent
                    if (parent is BirDatabase) {
                        // The element was a root element in this database.
                        if (parent === this) {
                            // Only impl elements are allowed to be roots
                            element as BirImplElementBase
                            element._parent = null
                        } else {
                            handleElementFromOtherDatabase()
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

    private fun BirElementBase.findActualContainingDatabaseAfterMove(): BirDatabase? {
        var ancestor = _parent
        while (true) {
            when (ancestor) {
                null -> break
                is BirElementBase -> {
                    val db = ancestor._containingDatabase
                    if (db != null) {
                        if (db === this@BirDatabase) {
                            return db
                        } else {
                            handleElementFromOtherDatabase()
                        }
                    }

                    ancestor = ancestor._parent
                }
                is BirDatabase -> return ancestor
            }
        }

        return null
    }

    private fun collectCurrentRootElements(): List<BirElementBase> {
        possiblyRootElements.retainAll { it._parent === this }
        return possiblyRootElements
    }

    fun getRootElements(): List<BirElement> =
        ArrayList<BirElement>(collectCurrentRootElements())

    private fun handleElementFromOtherDatabase(): Nothing {
        // Once an element is attached to some database, trying to
        //  attach it to some other database instance is not supported,
        //  even after being removed from the former.
        //  This limitation can probably be removed in future by adding proper
        //  realization and handling of such a move, but right now
        //  this case is not anticipated to occur in the compilation flow.
        TODO("Handle element possibly coming from different database")
    }


    /**
     * Updates the indices the given element belongs to, based on its current state.
     * If an element is already contained in a correct index, this function does nothing.
     *
     * It also updates the listing of references between elements, used for tracking back-references.
     *
     * This function should be called for all [BirElement]s added to the database, or changed later on,
     * in such a way that could affect any of the former.
     *
     * @param element The element to be indexed.
     * @param updateBackReferences Whether to update the list of other elements' back references,
     * based on forward references of this element. Otherwise, just update the index.
     */
    private fun indexElement(element: BirElementBase) {
        if (element._containingDatabase !== this) return
        if (!indexElements) return

        if (!element.hasFlag(BirElementBase.FLAG_IS_IN_CLASS_INDEX)) {
            val indexSlot = element.elementClassId.toInt()
            val targetSlot = elementIndexSlots[indexSlot]
            targetSlot?.add(element)

            element.setFlag(BirElementBase.FLAG_IS_IN_CLASS_INDEX, true)
        }

        if (!element.hasFlag(BirElementBase.FLAG_HAS_VALID_FORWARD_REFERENCES_INDEX)) {
            indexForwardReferencesOfElement(element)

            element.setFlag(BirElementBase.FLAG_HAS_VALID_FORWARD_REFERENCES_INDEX, true)
        }
    }

    internal fun indexForwardReferencesOfElement(element: BirElementBase) {
        val forwardReferenceRecorder = ForwardReferenceRecorder()
        element.getForwardReferences(forwardReferenceRecorder)

        val forwardReference = forwardReferenceRecorder.recordedRef
        if (forwardReference != null) {
            val referenceDatabase = forwardReference._containingDatabase
            if (referenceDatabase !== this) {
                maybeAttachReferencedElementToOtherDatabase(forwardReference)
            }

            forwardReference.registerBackReference(element)
        }
    }

    private fun maybeAttachReferencedElementToOtherDatabase(element: BirElementBase) {
        var externalDatabase: BirDatabase? = null
        var rootElement: BirElementBase? = element
        run {
            var ancestor = element._parent
            while (ancestor != null) {
                when (ancestor) {
                    is BirElementBase -> {
                        val db = ancestor._containingDatabase
                        if (db === this) {
                            return
                        } else if (db != null) {
                            externalDatabase = db
                            rootElement = null
                            break
                        }

                        rootElement = ancestor
                        ancestor = ancestor._parent
                    }
                    is BirDatabase -> {
                        externalDatabase = ancestor
                    }
                }
            }
        }

        if (externalDatabase == null && rootElement != null) {
            externalDatabase = attachExternalReferencedElementTreeToOtherDatabase?.invoke(rootElement)
            require(externalDatabase !== this) { "Cannot add referenced element to the same database." }
        }

        if (externalDatabase != null) {
            var ancestor: BirElementParent? = element
            while (ancestor is BirElementBase && ancestor._containingDatabase == null) {
                if (ancestor === rootElement) {
                    externalDatabase.attachRootElement(ancestor)
                } else {
                    externalDatabase.elementAttached(ancestor)
                }
                ancestor = ancestor._parent
            }
        }
    }

    /**
     * Updates the indices of all elements stored in this [BirDatabase].
     *
     * It is only useful after registering new indices and calling [activateNewRegisteredIndices],
     * otherwise all indices should already be up-to-date.
     */
    fun reindexAllElements() {
        indexElements = true
        realizeTreeMovements()

        val roots = getRootElements()
        for (root in roots) {
            root.acceptLite { element ->
                indexElement(element)
                element.walkIntoChildren()
            }
        }
    }

    /**
     * Returns an unordered and live sequence of unique elements in this database, which match a given index key.
     *
     * The returned sequence is live, meaning it reflects the changes made to the database during the iteration,
     * up until the last element in the sequence is reached (i.e., once [Iterator.hasNext] returns false, it won't return true again).
     *
     * The index keys have to be provided to this function in the same order they were registered.
     * This function cannot be called twice with the same key.
     */
    fun <E : BirElement> getElementsWithIndex(elementType: BirElementType<E>): ElementIndexIterator<E> {
        val allTopLevelClasses = when (elementType) {
            is BirElementClass<*> -> setOf(elementType)
            is BirElementUnionType<*> -> elementType.possibleClasses
        }

        val slots = allTopLevelClasses
            .flatMap { it.descendantClassesAndSelf }
            .filter { it.hasImplementation }
            .map { elementIndexSlots[it.id]!! }
            .toTypedArray()

        val queryIndex = ++lastQueryIndex
        return ElementIndexIteratorImpl<E>(slots, queryIndex)
    }

    private inner class ElementIndexSlot {
        var array = arrayOfNulls<BirElementBase?>(4)
            private set
        var size = 0

        fun add(element: BirElementBase) {
            var array = array
            val size = size

            if (size == array.size) {
                array = growArray(array)
                this.array = array
            }

            array[size] = element
            this.size = size + 1
        }

        private fun growArray(array: Array<BirElementBase?>): Array<BirElementBase?> {
            val newSize = maxOf(500, array.size * 2)
            return array.copyOf(newSize)
        }
    }

    abstract class ElementIndexIterator<E : BirElement> : Sequence<E>, Iterator<E> {
        protected var canceled = false

        abstract override fun next(): E

        override fun iterator(): ElementIndexIterator<E> {
            require(!canceled) { "Iterator was cancelled" }
            return this
        }
    }

    private inner class ElementIndexIteratorImpl<E : BirElement>(
        private val slots: Array<ElementIndexSlot>,
        private val queryIndex: UByte,
    ) : ElementIndexIterator<E>() {
        private var slotIndex = 0
        private var currentSlot = slots[0]
        private var elementIndexInSlot = 0
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
            val queryIndex = queryIndex

            var array = currentSlot.array
            while (true) {
                var element: BirElementBase? = null
                while (true) {
                    val idx = elementIndexInSlot
                    if (idx == currentSlot.size) {
                        if (++slotIndex < slots.size) {
                            currentSlot = slots[slotIndex]
                            array = currentSlot.array
                            elementIndexInSlot = 0
                            continue
                        } else {
                            break
                        }
                    }

                    element = array[idx]!!

                    // We have to check whether this element sill matches the given index,
                    //  because elements are not removed eagerly.
                    if (element.getContainingDatabase() === this@BirDatabase) {
                        // We have to check if this element has not been returned before,
                        //  as the the sequence is guaranteed to yield each one only once.
                        //  An element may be encountered twice in the buffer in the case
                        //  it was yielded, then removed, and then added to the index again.
                        if (element.lastReturnedInQueryIndex != queryIndex) {
                            break
                        }
                    } else {
                        val lastIdx = currentSlot.size - 1
                        if (idx < lastIdx) {
                            array[idx] = array[lastIdx]
                        }
                        array[lastIdx] = null

                        element.setFlag(BirElementBase.FLAG_IS_IN_CLASS_INDEX, false)

                        currentSlot.size--
                        element = null
                    }
                }

                if (element != null) {
                    element.lastReturnedInQueryIndex = queryIndex
                    elementIndexInSlot++
                    return element
                } else {
                    elementIndexInSlot = 0
                    canceled = true
                    return null
                }
            }
        }
    }
}