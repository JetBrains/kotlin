/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

abstract class BirImplElementBase(elementClass: BirElementClass<*>) : BirElementBase(elementClass) {
    final override val parent: BirElementBase?
        get() {
            recordPropertyRead()
            return _parent as? BirElementBase
        }

    internal fun getParentRecordingRead(): BirElementParent? {
        recordPropertyRead()
        return _parent
    }

    final override fun setParentWithInvalidation(new: BirElementParent?) {
        if (_parent !== new) {
            _parent = new
            invalidate()
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
                new.replacedWithInternal(null)
                new.setParentWithInvalidation(this)
                if (oldParent is BirImplElementBase) {
                    oldParent.invalidate()
                }

                _containingDatabase?.elementMoved(new, oldParent)
            } else {
                new.setParentWithInvalidation(this)
                _containingDatabase?.elementAttached(new)
            }
        }
    }


    internal open fun replaceChildProperty(old: BirElement, new: BirElement?) {
        throwChildForReplacementNotFound(old)
    }

    protected fun throwChildForReplacementNotFound(old: BirElement): Nothing {
        throw IllegalStateException("The child property $old not found in its parent $this")
    }


    override fun replaceWith(new: BirElement?) {
        if (this === new) return

        val parent = _parent
        require(parent != null) { "Element must have a parent" }

        replacedWithInternal(new as BirImplElementBase?)
        if (parent is BirImplElementBase) {
            parent.childReplaced(this, new)
            parent.invalidate()
        }
    }

    internal fun replacedWithInternal(new: BirImplElementBase?) {
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
            } else {
                parent.replaceChildProperty(this, new)
            }
        }
    }

    protected fun throwChildElementRemoved(propertyName: String): Nothing {
        throw IllegalStateException("The child property $propertyName has been removed from this element $this")
    }


    final override fun <T> getDynamicProperty(token: BirDynamicPropertyAccessToken<*, T>): T? {
        recordPropertyRead()
        return super.getDynamicProperty(token)
    }

    final override fun <T> setDynamicProperty(token: BirDynamicPropertyAccessToken<*, T>, value: T?): Boolean {
        val changed = super.setDynamicProperty(token, value)
        if (changed) {
            invalidate()
        }
        return changed
    }

    internal fun <T> getOrPutDynamicProperty(token: BirDynamicPropertyAccessToken<*, T>, compute: () -> T): T {
        token.requireValid()

        val arrayMap = dynamicProperties
        if (arrayMap == null) {
            val value = compute()
            initializeDynamicProperties(token, value)
            invalidate()
            return value
        }

        val foundIndex = findDynamicPropertyIndex(arrayMap, token.key)
        if (foundIndex >= 0) {
            @Suppress("UNCHECKED_CAST")
            return arrayMap[foundIndex + 1] as T
        } else {
            val value = compute()
            val entryIndex = -(foundIndex + 1)
            addDynamicProperty(arrayMap, entryIndex, token.key, value)
            invalidate()
            return value
        }
    }

    // todo: fine-grained control of which data to copy
    internal fun copyDynamicProperties(from: BirElementBase) {
        invalidate()
        dynamicProperties = from.dynamicProperties?.copyOf()
    }


    internal fun invalidate() {
        _containingDatabase?.invalidateElement(this)
    }

    internal fun recordPropertyRead() {
        val database = _containingDatabase ?: return
        val classifiedElement = database.mutableElementCurrentlyBeingClassified ?: return
        if (classifiedElement !== this) {
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
}