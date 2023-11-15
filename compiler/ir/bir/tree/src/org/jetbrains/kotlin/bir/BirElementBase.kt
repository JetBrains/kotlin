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

import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

abstract class BirElementBase : BirElementParent(), BirElement {
    /**
     * Root reference may be stale.
     * To actualize it for all elements in a forest, call [BirForest.realizeTreeMovements]
     */
    internal var root: BirForest? = null
    internal var _parent: BirElementParent? = null
    private var flags: Byte = 0
    internal var indexSlot: UByte = 0u
    private var backReferences: Any? = null // null | BirElementBase | Array<BirElementBase?>
    internal var dynamicProperties: Array<Any?>? = null

    abstract override val parent: BirElementBase?

    internal abstract fun setParentWithInvalidation(new: BirElementParent?)

    val attachedToTree
        get() = root != null

    fun getContainingForest(): BirForest? {
        root?.realizeTreeMovements()
        return root
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
        root?.realizeTreeMovements()
        other.root?.realizeTreeMovements()
        if (root !== other.root) {
            return false
        }

        var n = other
        while (true) {
            n = n.parent ?: break
            if (n === this) return true
        }

        return false
    }

    internal fun findRootFromAncestors(): BirForest? {
        var n = _parent
        while (true) {
            when (n) {
                null -> break
                is BirElementBase -> n = n._parent
                is BirForest -> return n
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
        val size = token.manager.getInitialDynamicPropertyArraySize(javaClass)
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


    internal fun registerBackReference(backReference: BirElementBase) {
        val RESIZE_GRADUALITY = 4
        var elementsOrSingle = backReferences
        when (elementsOrSingle) {
            null -> {
                backReferences = backReference
            }
            is BirElementBase -> {
                if (elementsOrSingle === backReference) {
                    return
                }

                val elements = arrayOfNulls<BirElementBase>(RESIZE_GRADUALITY)
                elements[0] = elementsOrSingle
                elements[1] = backReference
                backReferences = elements
            }
            else -> {
                @Suppress("UNCHECKED_CAST")
                elementsOrSingle as Array<BirElementBase?>

                var newIndex = 0
                while (newIndex < elementsOrSingle.size) {
                    val e = elementsOrSingle[newIndex]
                    if (e == null) {
                        break
                    } else if (e === backReference) {
                        return
                    }
                    newIndex++
                }

                if (newIndex == elementsOrSingle.size) {
                    elementsOrSingle = elementsOrSingle.copyOf(elementsOrSingle.size + RESIZE_GRADUALITY)
                    backReferences = elementsOrSingle
                }
                elementsOrSingle[newIndex] = backReference
            }
        }
    }

    final override fun <E : BirElement> getBackReferences(key: BirElementBackReferencesKey<E>): List<E> {
        root?.flushElementsWithInvalidatedIndexBuffer()
        require(attachedToTree) { "Element must be attached to tree" }

        val array: Array<BirElementBase?>
        when (val elementsOrSingle = backReferences) {
            null -> return emptyList()
            is BirElementBase -> array = arrayOf(elementsOrSingle)
            else -> {
                @Suppress("UNCHECKED_CAST")
                array = elementsOrSingle as Array<BirElementBase?>
            }
        }

        val results = ArrayList<BirElementBase>(array.size)
        val backReferenceRecorder = BirForest.BackReferenceRecorder()

        var j = 0
        for (i in array.indices) {
            val backRef = array[i] ?: break

            with(backReferenceRecorder) {
                key.recorder.recordBackReferences(backRef)
            }

            val recordedRef = backReferenceRecorder.recordedRef
            backReferenceRecorder.recordedRef = null
            if (recordedRef != null && recordedRef.attachedToTree) {
                if (recordedRef === this) {
                    results += backRef
                }

                if (i != j) {
                    array[j] = backRef
                }
                j++
            }
        }

        return results as List<E>
    }


    companion object {
        const val FLAG_IN_INVALIDATE_INDEX_BUFFER: Byte = (1 shl 0).toByte()
        const val FLAG_IS_IN_MOVED_ELEMENTS_BUFFER: Byte = (1 shl 1).toByte()

        const val CONTAINING_LIST_ID_BITS = 3
    }
}