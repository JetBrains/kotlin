/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import kotlin.experimental.or

abstract class BirImplElementBase(elementClass: BirElementClass<*>) : BirElementBase(elementClass) {
    // bits reservation: 0 - parent, 1-n - child element lists, n-14 - all other properties, 15 - dynamic properties
    private var observedPropertiesBitSet: Short = 0

    final override val parent: BirElementBase?
        get() {
            recordPropertyRead(PARENT_PROPERTY_ID)
            return _parent as? BirElementBase
        }

    internal fun getParentRecordingRead(): BirElementParent? {
        recordPropertyRead(PARENT_PROPERTY_ID)
        return _parent
    }

    final override fun setParentWithInvalidation(new: BirElementParent?) {
        if (_parent !== new) {
            _parent = new
            invalidate(PARENT_PROPERTY_ID)
        }
    }


    protected fun initChild(new: BirElement?) {
        childReplaced(null, new)
    }

    internal fun childReplaced(old: BirElement?, new: BirElement?) {
        if (old != null) {
            old as BirImplElementBase

            old.setParentWithInvalidation(null)
            _containingDatabase?.elementDetached(old)
        }

        if (new != null) {
            new as BirElementBase

            val oldParent = (new as? BirImplElementBase)?._parent
            if (oldParent != null) {
                val propertyId = new.replacedWithInternal(null)
                new.setParentWithInvalidation(this)
                if (oldParent is BirImplElementBase) {
                    oldParent.invalidate(propertyId)
                }

                _containingDatabase?.elementMoved(new, oldParent)
            } else {
                new.setParentWithInvalidation(this)
                _containingDatabase?.elementAttached(new)
            }
        }
    }


    internal open fun replaceChildProperty(old: BirElement, new: BirElement?): Int {
        throwChildForReplacementNotFound(old)
    }

    protected fun throwChildForReplacementNotFound(old: BirElement): Nothing {
        throw IllegalStateException("The child property $old not found in its parent $this")
    }


    override fun replaceWith(new: BirElement?) {
        if (this === new) return

        val parent = _parent
        require(parent != null) { "Element must have a parent" }

        val propertyId = replacedWithInternal(new as BirImplElementBase?)
        if (parent is BirImplElementBase) {
            parent.childReplaced(this, new)
            parent.invalidate(propertyId)
        }
    }

    internal fun replacedWithInternal(new: BirImplElementBase?): Int {
        val parent = _parent
        if (parent is BirImplElementBase) {
            val containingList = getContainingList()
            if (containingList != null) {
                containingList as BirImplChildElementList<*>

                val found = if (new == null && !containingList.isNullable) {
                    containingList.removeInternal(this)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    containingList as BirChildElementList<BirImplElementBase?>
                    containingList.replaceInternal(this, new)
                }

                if (!found) {
                    containingList.parent.throwChildForReplacementNotFound(this)
                }

                return containingList.id
            } else {
                return parent.replaceChildProperty(this, new)
            }
        }
        return -1
    }

    protected fun throwChildElementRemoved(propertyName: String): Nothing {
        throw IllegalStateException("The child property $propertyName has been removed from this element $this")
    }


    final override fun <T> getDynamicProperty(token: BirElementDynamicPropertyToken<*, T>): T? {
        recordPropertyRead(DYNAMIC_PROPERTY_ID)
        return super.getDynamicProperty(token)
    }

    final override fun <T> setDynamicProperty(token: BirElementDynamicPropertyToken<*, T>, value: T?): Boolean {
        val changed = super.setDynamicProperty(token, value)
        if (changed) {
            invalidate(DYNAMIC_PROPERTY_ID)
        }
        return changed
    }

    internal fun <T> getOrPutDynamicProperty(token: BirElementDynamicPropertyToken<*, T>, compute: () -> T): T {
        val arrayMap = dynamicProperties
        if (arrayMap == null) {
            val value = compute()
            initializeDynamicProperties(token, value)
            invalidate(DYNAMIC_PROPERTY_ID)
            return value
        }

        val foundIndex = findDynamicPropertyIndex(arrayMap, token.key)
        if (foundIndex >= 0) {
            @Suppress("UNCHECKED_CAST")
            return arrayMap[foundIndex + 1] as T
        } else {
            val value = compute()
            val entryIndex = -(foundIndex + 1)
            arrayMap[entryIndex] = token.key
            arrayMap[entryIndex + 1] = value
            invalidate(DYNAMIC_PROPERTY_ID)
            return value
        }
    }

    // todo: fine-grained control of which data to copy
    internal fun copyDynamicProperties(from: BirElementBase) {
        invalidate(DYNAMIC_PROPERTY_ID)
        dynamicProperties = from.dynamicProperties?.copyOf()
    }


    internal fun invalidate() {
        _containingDatabase?.invalidateElement(this)
    }

    internal fun invalidate(propertyId: Int) {
        if ((observedPropertiesBitSet.toInt() and (1 shl propertyId)) != 0) {
            invalidate()
        }
    }

    internal fun recordPropertyRead(propertyId: Int) {
        val database = _containingDatabase ?: return
        val classifiedElement = database.mutableElementCurrentlyBeingClassified ?: return
        if (classifiedElement === this) {
            observedPropertiesBitSet = observedPropertiesBitSet or (1 shl propertyId).toShort()
        } else {
            registerDependentElement(classifiedElement)
        }
    }

    private fun registerDependentElement(dependentElement: BirImplElementBase) {
        addRelatedElement(dependentElement, false)
    }

    internal fun indexInvalidatedDependentElements() {
        val database = _containingDatabase ?: return

        val array: Array<BirElementBase?>
        var storageIsArray = false
        when (val elementsOrSingle = relatedElements) {
            null -> return
            is BirElementBase -> array = arrayOf(elementsOrSingle)
            else -> {
                @Suppress("UNCHECKED_CAST")
                array = elementsOrSingle as Array<BirElementBase?>
                storageIsArray = true
            }
        }

        for (i in array.indices) {
            val element = array[i] ?: break

            if (!element.hasFlag(FLAG_HAS_BEEN_REGISTERED_AS_DEPENDENT_ELEMENT)) {
                // This element is certainly not a back reference, so is safe to delete.
                removeRelatedElement(i)
            }

            database.indexElement(element, true)
        }
    }

    companion object {
        private const val PARENT_PROPERTY_ID = 0
        private const val DYNAMIC_PROPERTY_ID = 15
    }
}