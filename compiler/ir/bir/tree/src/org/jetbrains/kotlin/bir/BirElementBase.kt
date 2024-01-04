/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.bir.symbols.BirSymbol
import org.jetbrains.kotlin.bir.symbols.ownerIfBound
import org.jetbrains.kotlin.bir.util.ForwardReferenceRecorder
import org.jetbrains.kotlin.bir.util.SmallFixedPointFraction
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

@Suppress("EqualsOrHashCode")
abstract class BirElementBase(elementClass: BirElementClass<*>) : BirElementParent(), BirElement {
    /**
     * Database reference may be stale.
     * To actualize it for all elements in a database, call [BirDatabase.realizeTreeMovements]
     */
    internal var _containingDatabase: BirDatabase? = null
    internal var _parent: BirElementParent? = null

    internal val elementClassId = elementClass.id.toUByte()
    private var flags: Byte = 0

    internal var indexSlot: UByte = 0u
    internal var lastReturnedInQueryOfIndexSlot: UByte = 0u

    // Contains both back references and dependent elements
    protected var relatedElements: Any? = null // null | BirElementBase | Array<BirElementBase?>
        private set
    private var relatedElementsFullness = SmallFixedPointFraction.ZERO

    // Array of form [key, value, key, value, ...]
    internal var dynamicProperties: Array<Any?>? = null


    internal val elementClass
        get() = BirMetadata.allElementsById.getOrNull(elementClassId.toInt())


    abstract override val parent: BirElementBase?

    internal abstract fun setParentWithInvalidation(new: BirElementParent?)

    fun getContainingDatabase(): BirDatabase? {
        // perf: it should be possible to realize movements only for this element
        _containingDatabase?.realizeTreeMovements()
        return _containingDatabase
    }


    internal fun hasFlag(flag: Byte): Boolean =
        (flags and flag).toInt() != 0

    internal fun setFlag(flag: Byte, value: Boolean) {
        flags = if (value) flags or flag else flags and flag.inv()
    }

    internal var containingListId: Int
        get() = flags.toInt() shr (8 - CONTAINING_LIST_ID_BITS)
        set(value) {
            flags = (flags and (-1 ushr (32 - 8 + CONTAINING_LIST_ID_BITS)).toByte()) or (value shl (8 - CONTAINING_LIST_ID_BITS)).toByte()
        }


    fun isAncestorOf(other: BirElementBase): Boolean {
        if (getContainingDatabase() !== other.getContainingDatabase()) {
            return false
        }

        var n = other
        while (true) {
            n = n.parent ?: break
            if (n === this) return true
        }

        return false
    }

    internal fun findDatabaseFromAncestors(): BirDatabase? {
        var n = _parent
        while (true) {
            when (n) {
                null -> break
                is BirElementBase -> n = n._parent
                is BirDatabase -> return n
            }
        }

        return null
    }


    internal open fun getChildrenListById(id: Int): BirChildElementList<*> {
        throwChildrenListWithIdNotFound(id)
    }

    protected fun throwChildrenListWithIdNotFound(id: Int): Nothing {
        throw IllegalStateException("The element $this does not have a children list with id $id")
    }

    internal fun getContainingList(): BirChildElementList<*>? {
        val containingListId = containingListId
        return if (containingListId == 0) null
        else (parent as? BirElementBase)?.getChildrenListById(containingListId)
    }


    internal open fun <T> getDynamicProperty(token: BirElementDynamicPropertyToken<*, T>): T? {
        val arrayMap = dynamicProperties ?: return null
        val keyIndex = findDynamicPropertyIndex(arrayMap, token.key)
        if (keyIndex < 0) return null
        @Suppress("UNCHECKED_CAST")
        return arrayMap[keyIndex + 1] as T
    }

    internal open fun <T> setDynamicProperty(token: BirElementDynamicPropertyToken<*, T>, value: T?): Boolean {
        val arrayMap = dynamicProperties
        if (arrayMap == null) {
            if (value == null) {
                // optimization: next read will return null if the array is null, so no need to initialize it
                return false
            } else {
                initializeDynamicProperties(token, value)
                return true
            }
        } else {
            val foundIndex = findDynamicPropertyIndex(arrayMap, token.key)
            if (foundIndex >= 0) {
                val valueIndex = foundIndex + 1
                val old = arrayMap[valueIndex]
                if (old != value) {
                    arrayMap[valueIndex] = value
                    return true
                } else {
                    return false
                }
            } else {
                val entryIndex = -(foundIndex + 1)
                arrayMap[entryIndex] = token.key
                arrayMap[entryIndex + 1] = value
                return value != null
            }
        }
    }

    protected fun <T> initializeDynamicProperties(token: BirElementDynamicPropertyToken<*, T>, value: T?) {
        val size = token.manager.getInitialDynamicPropertyArraySize(elementClassId.toInt())
        require(size != 0) { "This element is not supposed to store any dynamic properties" }

        val arrayMap = arrayOfNulls<Any?>(size * 2)
        arrayMap[0] = token.key
        arrayMap[1] = value
        this.dynamicProperties = arrayMap
    }

    protected fun findDynamicPropertyIndex(arrayMap: Array<Any?>, propertyKey: BirElementDynamicPropertyKey<*, *>): Int {
        var i = 0
        while (i < arrayMap.size) {
            val key = arrayMap[i]
            if (key == null) return -i - 1
            if (key === propertyKey) return i
            i += 2
        }
        return -i - 1
    }


    protected fun addRelatedElement(relatedElement: BirElementBase, isBackReference: Boolean) {
        val hasBeenStoredInArrayFlag = if (isBackReference) FLAG_HAS_BEEN_STORED_IN_BACK_REFERENCES_ARRAY
        else FLAG_HAS_BEEN_STORED_IN_DEPENDENT_ELEMENTS_ARRAY

        var elementsOrSingle = relatedElements
        when (elementsOrSingle) {
            null -> {
                relatedElements = relatedElement
                relatedElementsFullness = SmallFixedPointFraction.ZERO
            }
            is BirElementBase -> {
                if (elementsOrSingle === relatedElement) {
                    return
                }

                // 2 elements in array is a very common case.
                val elements = arrayOfNulls<BirElementBase>(2)
                elements[0] = elementsOrSingle
                elements[1] = relatedElement
                relatedElements = elements

                val newSize = 2
                relatedElementsFullness = SmallFixedPointFraction(newSize, elements.size)

                elementsOrSingle.setFlag(hasBeenStoredInArrayFlag, true)
                relatedElement.setFlag(hasBeenStoredInArrayFlag, true)
            }
            else -> {
                @Suppress("UNCHECKED_CAST")
                elementsOrSingle as Array<BirElementBase?>

                var currentCount = 0
                if (relatedElement.hasFlag(FLAG_HAS_BEEN_STORED_IN_BACK_REFERENCES_ARRAY or FLAG_HAS_BEEN_STORED_IN_DEPENDENT_ELEMENTS_ARRAY)) {
                    while (currentCount < elementsOrSingle.size) {
                        val element = elementsOrSingle[currentCount]
                        if (element == null) {
                            break
                        } else if (element === relatedElement) {
                            relatedElement.setFlag(hasBeenStoredInArrayFlag, true)
                            return
                        }
                        currentCount++
                    }
                } else {
                    // Optimization: this element certainly isn't in the array. Just find a free spot.
                    currentCount = findRelatedElementsArrayCount(elementsOrSingle)
                }

                if (currentCount == elementsOrSingle.size) {
                    // This formula gives a nice progression: 2, 3, 4, 6, 9, 13...
                    val newArraySize = elementsOrSingle.size * 3 / 2

                    elementsOrSingle = elementsOrSingle.copyOf(newArraySize)
                    relatedElements = elementsOrSingle
                }
                elementsOrSingle[currentCount] = relatedElement

                currentCount++
                relatedElementsFullness = SmallFixedPointFraction(currentCount, elementsOrSingle.size)

                relatedElement.setFlag(hasBeenStoredInArrayFlag, true)
            }
        }
    }

    protected fun removeRelatedElementFromArray(index: Int) {
        @Suppress("UNCHECKED_CAST")
        val array = relatedElements as Array<BirElementBase?>
        val count = findRelatedElementsArrayCount(array)
        require(index < count)

        val lastIndex = count - 1
        if (index != lastIndex) {
            array[index] = array[lastIndex]
        }
        array[lastIndex] = null

        relatedElementsFullness = SmallFixedPointFraction(lastIndex, array.size)
    }

    private fun findRelatedElementsArrayCount(array: Array<BirElementBase?>): Int {
        val minSize = relatedElementsFullness * array.size
        if (minSize == array.size) {
            return minSize
        }

        for (i in minSize..<array.size) {
            if (array[i] == null) {
                return i
            }
        }
        error("Should not reach here")
    }

    internal fun registerBackReference(backReference: BirElementBase) {
        addRelatedElement(backReference, true)
    }

    internal fun <R : BirElement> getBackReferences(key: BirElementBackReferencesKey<*, R>): List<BirElementBase> {
        _containingDatabase?.flushInvalidatedElementBuffer()
        require(_containingDatabase != null) { "Element must be attached to some BirDatabase" }

        val array: Array<BirElementBase?>
        var storageIsArray = false
        when (val elementsOrSingle = relatedElements) {
            null -> return emptyList<BirElementBase>()
            is BirElementBase -> array = arrayOf(elementsOrSingle)
            else -> {
                @Suppress("UNCHECKED_CAST")
                array = elementsOrSingle as Array<BirElementBase?>
                storageIsArray = true
            }
        }

        val results = ArrayList<BirElementBase>(array.size)

        for (i in array.indices) {
            val backRef = array[i] ?: break

            var isValidBackRef = false
            if (!(storageIsArray && !backRef.hasFlag(FLAG_HAS_BEEN_STORED_IN_BACK_REFERENCES_ARRAY))) {
                val forwardReferenceRecorder = ForwardReferenceRecorder()
                with(forwardReferenceRecorder) {
                    key.recorder.recordBackReferences(backRef)
                }

                val recordedRef = forwardReferenceRecorder.recordedRef
                forwardReferenceRecorder.reset()

                if (recordedRef === this) {
                    backRef._containingDatabase?.realizeTreeMovements()
                    if (backRef._containingDatabase != null) {
                        isValidBackRef = true
                    }
                }
            }

            if (isValidBackRef) {
                results += backRef
            } else if (storageIsArray && !backRef.hasFlag(FLAG_HAS_BEEN_STORED_IN_DEPENDENT_ELEMENTS_ARRAY)) {
                // This element is certainly not a dependent element, so it is safe to remove.
                removeRelatedElementFromArray(i)
            }
        }

        return results
    }


    final override fun equals(other: Any?): Boolean {
        return other === this ||
                (other is BirSymbol && other.ownerIfBound === this)
    }


    companion object {
        internal const val FLAG_INVALIDATED: Byte = (1 shl 0).toByte()
        internal const val FLAG_IS_IN_MOVED_ELEMENTS_BUFFER: Byte = (1 shl 1).toByte()
        internal const val FLAG_HAS_BEEN_STORED_IN_BACK_REFERENCES_ARRAY: Byte = (1 shl 2).toByte()
        internal const val FLAG_HAS_BEEN_STORED_IN_DEPENDENT_ELEMENTS_ARRAY: Byte = (1 shl 3).toByte()

        private const val CONTAINING_LIST_ID_BITS = 3
    }
}