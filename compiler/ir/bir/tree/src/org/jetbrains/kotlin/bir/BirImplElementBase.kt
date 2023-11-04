/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

abstract class BirImplElementBase : BirElementBase() {
    private var dependentIndexedElements: Any? = null // null | BirImplElementBase | Array<BirImplElementBase?>

    final override val parent: BirImplElementBase?
        get() {
            recordPropertyRead()
            return _parent as? BirImplElementBase
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
            root?.elementDetached(old)
        }

        if (new != null) {
            new as BirImplElementBase

            val oldParent = new._parent
            if (oldParent != null) {
                new.replacedWithInternal(null)
                new.setParentWithInvalidation(this)
                if (oldParent is BirImplElementBase) {
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


    override fun replaceWith(new: BirElement?) {
        if (this === new) return

        val parent = replacedWithInternal(new as BirImplElementBase?)
        if (parent is BirImplElementBase) {
            parent.childReplaced(this, new)
            parent.invalidate()
        }
    }

    internal fun replacedWithInternal(new: BirImplElementBase?): BirElementParent? {
        val parent = _parent
        if (parent is BirImplElementBase) {
            val list = getContainingList() as BirImplChildElementList<*>?
            if (list != null) {
                val found = if (new == null && !list.isNullable) {
                    list.removeInternal(this)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    list as BirChildElementList<BirImplElementBase?>
                    list.replaceInternal(this, new as BirImplElementBase?)
                }

                if (!found) {
                    (list.parent as BirImplElementBase).throwChildForReplacementNotFound(this)
                }
            } else {
                parent.replaceChildProperty(this, new)
            }
        }
        return parent
    }

    protected fun throwChildElementRemoved(propertyName: String): Nothing {
        throw IllegalStateException("The child property $propertyName has been removed from this element $this")
    }


    final override fun <T> getDynamicProperty(token: BirElementDynamicPropertyToken<*, T>): T? {
        recordPropertyRead()
        return super.getDynamicProperty(token)
    }

    final override fun <T> setDynamicProperty(token: BirElementDynamicPropertyToken<*, T>, value: T?): Boolean {
        val changed = super.setDynamicProperty(token, value)
        if (changed) {
            invalidate()
        }
        return changed
    }

    internal fun <T> getOrPutDynamicProperty(token: BirElementDynamicPropertyToken<*, T>, compute: () -> T): T {
        // todo: why asserts do run?
        //assert(root?.isInsideElementClassification != true)

        val arrayMap = dynamicProperties
        if (arrayMap == null) {
            val value = compute()
            initializeDynamicProperties(token, value)
            invalidate()
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

    // todo: fine-grained control of which data to copy
    internal fun copyDynamicProperties(from: BirElementBase) {
        invalidate()
        dynamicProperties = from.dynamicProperties?.copyOf()
    }


    internal fun invalidate() {
        root?.elementIndexInvalidated(this)
    }

    internal fun recordPropertyRead() {
        root?.recordElementPropertyRead(this)
    }


    internal fun registerDependentElement(dependentElement: BirImplElementBase) {
        if (dependentElement === this) {
            return
        }

        val RESIZE_GRADUALITY = 4
        var elementsOrSingle = dependentIndexedElements
        when (elementsOrSingle) {
            null -> {
                dependentIndexedElements = dependentElement
            }
            is BirImplElementBase -> {
                if (elementsOrSingle === dependentElement) {
                    return
                }

                val elements = arrayOfNulls<BirImplElementBase>(RESIZE_GRADUALITY)
                elements[0] = elementsOrSingle
                elements[1] = dependentElement
                dependentIndexedElements = elements
            }
            else -> {
                @Suppress("UNCHECKED_CAST")
                elementsOrSingle as Array<BirImplElementBase?>

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
            is BirImplElementBase -> {
                dependentIndexedElements = null
                elementsOrSingle.invalidate()
            }
            else -> {
                @Suppress("UNCHECKED_CAST")
                var array = elementsOrSingle as Array<BirImplElementBase?>
                var arraySize = array.size
                var i = 0
                while (i < arraySize) {
                    val e = array[i] ?: break
                    val arrayIsFull = array[arraySize - 1] != null

                    array[i] = null
                    e.invalidate()

                    if (arrayIsFull && array !== dependentIndexedElements) {
                        @Suppress("UNCHECKED_CAST")
                        array = dependentIndexedElements as Array<BirImplElementBase?>
                        arraySize = array.size
                    }

                    i++
                }
            }
        }
    }


    fun unsafeDispose() {
        acceptChildrenLite {
            (it as BirImplElementBase).setParentWithInvalidation(null)
            //childDetached(it)
        }
        // todo: mark as disposed
    }
}