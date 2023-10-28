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
     * It may get out-of-sync only inside [BirForest.subtreeShuffleTransaction]
     */
    internal var root: BirForest? = null
    internal var _parent: BirElementParent? = null
    internal var containingListId: Byte = 0
    private var flags: Byte = 0
    internal var indexSlot: UByte = 0u
    private var dependentIndexedElements: Any? = null // null | BirElementBase | Array<BirElementBase?>
    private var backReferences: Any? = null // null | BirElementBase | Array<BirElementBase?>
    private var dynamicProperties: Array<Any?>? = null

    final override val parent: BirElementBase?
        get() {
            recordPropertyRead()
            return _parent as? BirElementBase
        }

    internal fun getParentRecordingRead(): BirElementParent? {
        recordPropertyRead()
        return _parent
    }

    internal fun setParentWithInvalidation(new: BirElementParent?) {
        if (_parent !== new) {
            _parent = new
            invalidate()
        }
    }

    val attachedToTree
        get() = root != null


    internal fun hasFlag(flag: Byte): Boolean =
        (flags and flag).toInt() != 0

    internal fun setFlag(flag: Byte, value: Boolean) {
        flags = if (value) flags or flag else flags and flag.inv()
    }


    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {}
    internal open fun acceptChildrenLite(visitor: BirElementVisitorLite) {}

    fun isAncestorOf(other: BirElementBase): Boolean {
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

    protected fun throwChildElementRemoved(propertyName: String): Nothing {
        throw IllegalStateException("The child property $propertyName has been removed from this element $this")
    }


    protected fun initChild(new: BirElement?) {
        childReplaced(null, new)
    }

    internal fun childReplaced(old: BirElement?, new: BirElement?) {
        if (old != null) {
            old as BirElementBase

            old.setParentWithInvalidation(null)
            root?.elementDetached(old)
        }

        if (new != null) {
            new as BirElementBase

            val oldParent = new._parent
            if (oldParent != null) {
                new.replacedWithInternal(null)
                new.setParentWithInvalidation(this)
                if (oldParent is BirElementBase) {
                    oldParent.invalidate()
                }

                root?.elementMoved(new, oldParent)
            } else {
                new.setParentWithInvalidation(this)
                root?.elementAttached(new)
            }
        }
    }


    internal open fun replaceChildProperty(old: BirElement, new: BirElement?) {
        throwChildForReplacementNotFound(old)
    }

    protected fun throwChildForReplacementNotFound(old: BirElement): Nothing {
        throw IllegalStateException("The child property $old not found in its parent $this")
    }

    internal open fun getChildrenListById(id: Int): BirChildElementList<*> {
        throwChildrenListWithIdNotFound(id)
    }

    protected fun throwChildrenListWithIdNotFound(id: Int): Nothing {
        throw IllegalStateException("The element $this does not have a children list with id $id")
    }


    override fun replaceWith(new: BirElement?) {
        if (this === new) return

        val parent = replacedWithInternal(new as BirElementBase?)
        if (parent is BirElementBase) {
            parent.childReplaced(this, new)
            parent.invalidate()
        }
    }

    internal fun replacedWithInternal(new: BirElementBase?): BirElementParent? {
        val parent = _parent
        if (parent is BirElementBase) {
            val list = getContainingList()
            if (list != null) {
                val found = if (new == null && !list.isNullable) {
                    list.removeInternal(this)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    list as BirChildElementList<BirElementBase?>
                    list.replaceInternal(this, new as BirElementBase?)
                }

                if (!found) {
                    list.parent.throwChildForReplacementNotFound(this)
                }
            } else {
                parent.replaceChildProperty(this, new)
            }
        }
        return parent
    }

    internal fun getContainingList(): BirChildElementList<*>? {
        val containingListId = containingListId.toInt()
        return if (containingListId == 0) null
        else (parent as? BirElementBase)?.getChildrenListById(containingListId)
    }


    internal fun <T> getDynamicProperty(token: BirElementDynamicPropertyToken<*, T>): T? {
        recordPropertyRead()
        val arrayMap = dynamicProperties ?: return null
        val keyIndex = findDynamicPropertyIndex(arrayMap, token)
        if (keyIndex < 0) return null
        @Suppress("UNCHECKED_CAST")
        return arrayMap[keyIndex + 1] as T
    }

    internal fun <T> setDynamicProperty(token: BirElementDynamicPropertyToken<*, T>, value: T?) {
        val arrayMap = dynamicProperties
        if (arrayMap == null) {
            if (value == null) {
                // optimization: next read will return null if the array is null, so no need to initialize it
                return
            }

            initializeDynamicProperties(token, value)
        } else {
            val keyIndex = findDynamicPropertyIndex(arrayMap, token)
            if (keyIndex >= 0) {
                val valueIndex = keyIndex + 1
                val old = arrayMap[valueIndex]
                if (old != value) {
                    arrayMap[valueIndex] = value
                    invalidate()
                }
            } else {
                val valueIndex = -keyIndex + 1
                arrayMap[valueIndex] = value
                invalidate()
            }
        }
    }

    internal fun <T> getOrPutDynamicProperty(token: BirElementDynamicPropertyToken<*, T>, compute: () -> T): T {
        recordPropertyRead()

        val arrayMap = dynamicProperties
        if (arrayMap == null) {
            val value = compute()
            initializeDynamicProperties(token, value)
            return value
        }

        val keyIndex = findDynamicPropertyIndex(arrayMap, token)
        if (keyIndex >= 0) {
            @Suppress("UNCHECKED_CAST")
            return arrayMap[keyIndex + 1] as T
        } else {
            val value = compute()
            val valueIndex = -keyIndex + 1
            arrayMap[valueIndex] = value
            invalidate()
            return value
        }
    }

    private fun <T> initializeDynamicProperties(token: BirElementDynamicPropertyToken<*, T>, value: T?) {
        val size = token.manager.getInitialDynamicPropertyArraySize(javaClass)
        require(size != 0) { "This element is not supposed to store any dynamic properties" }

        val arrayMap = arrayOfNulls<Any?>(size * 2)
        arrayMap[0] = token
        arrayMap[1] = value
        this.dynamicProperties = arrayMap

        invalidate()
    }

    private fun findDynamicPropertyIndex(arrayMap: Array<Any?>, token: BirElementDynamicPropertyToken<*, *>): Int {
        for (i in arrayMap.indices step 2) {
            val key = arrayMap[i] ?: return -i
            if (key === token) return i
        }
        return -1
    }

    // todo: fine-grained control of which data to copy
    internal fun copyDynamicProperties(from: BirElementBase) {
        dynamicProperties = from.dynamicProperties?.copyOf()
    }


    internal fun invalidate() {
        root?.elementIndexInvalidated(this)
    }

    internal fun recordPropertyRead() {
        root?.recordElementPropertyRead(this)
    }

    internal fun registerDependentElement(dependentElement: BirElementBase) {
        if (dependentElement === this) {
            return
        }

        val RESIZE_GRADUALITY = 4
        var elementsOrSingle = dependentIndexedElements
        when (elementsOrSingle) {
            null -> {
                dependentIndexedElements = dependentElement
            }
            is BirElementBase -> {
                if (elementsOrSingle === dependentElement) {
                    return
                }

                val elements = arrayOfNulls<BirElementBase>(RESIZE_GRADUALITY)
                elements[0] = elementsOrSingle
                elements[1] = dependentElement
                dependentIndexedElements = elements
            }
            else -> {
                @Suppress("UNCHECKED_CAST")
                elementsOrSingle as Array<BirElementBase?>

                var newIndex = 0
                while (newIndex < elementsOrSingle.size) {
                    val e = elementsOrSingle[newIndex]
                    if (e == null) {
                        break
                    } else if (e === dependentElement) {
                        return
                    }
                    newIndex++
                }

                if (newIndex == elementsOrSingle.size) {
                    elementsOrSingle = elementsOrSingle.copyOf(elementsOrSingle.size + RESIZE_GRADUALITY)
                    dependentIndexedElements = elementsOrSingle
                }
                elementsOrSingle[newIndex] = dependentElement
            }
        }
    }

    internal fun invalidateDependentElements() {
        when (val elementsOrSingle = dependentIndexedElements) {
            null -> {}
            is BirElementBase -> {
                dependentIndexedElements = null
                elementsOrSingle.invalidate()
            }
            else -> {
                @Suppress("UNCHECKED_CAST")
                var array = elementsOrSingle as Array<BirElementBase?>
                var arraySize = array.size
                var i = 0
                while (i < arraySize) {
                    val e = array[i] ?: break
                    val arrayIsFull = array[arraySize - 1] != null

                    array[i] = null
                    e.invalidate()

                    if (arrayIsFull && array !== dependentIndexedElements) {
                        @Suppress("UNCHECKED_CAST")
                        array = dependentIndexedElements as Array<BirElementBase?>
                        arraySize = array.size
                    }

                    i++
                }
            }
        }
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

    fun <E : BirElement> getBackReferences(key: BirElementBackReferencesKey<E>): List<E> {
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
        val root = root

        var j = 0
        for (i in array.indices) {
            val backRef = array[i] ?: break

            with(backReferenceRecorder) {
                key.recorder.recordBackReferences(backRef)
            }

            val recordedRef = backReferenceRecorder.recordedRef
            backReferenceRecorder.recordedRef = null
            if (recordedRef != null && recordedRef.root === root) {
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


    fun unsafeDispose() {
        acceptChildrenLite {
            it.setParentWithInvalidation(null)
            //childDetached(it)
        }
        // todo: mark as disposed
    }

    companion object {
        const val FLAG_MARKED_DIRTY_IN_SUBTREE_SHUFFLE_TRANSACTION: Byte = (1 shl 0).toByte()
    }
}