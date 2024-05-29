/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

abstract class BirImplElementBase : BirElementBase() {
    final override val parent: BirElementBase?
        get() {
            recordPropertyRead()
            return super.parent
        }

    internal fun setParentWithInvalidation(new: BirElementParent?) {
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
            new.moveElementToNewParent(this, _containingDatabase)
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
}