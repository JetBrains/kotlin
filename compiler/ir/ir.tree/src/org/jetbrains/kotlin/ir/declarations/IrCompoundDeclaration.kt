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

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.CHILD_DECLARATION_INDEX
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.DETACHED_INDEX
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import java.util.*

interface IrDeclarationOwner : IrElement {
    fun getChildDeclaration(index: Int): IrMemberDeclaration?
    fun replaceChildDeclaration(oldChild: IrMemberDeclaration, newChild: IrMemberDeclaration)
}

interface IrDeclarationOwner1 : IrDeclarationOwner {
    val childDeclaration: IrMemberDeclaration
}

interface IrDeclarationOwnerN : IrDeclarationOwner {
    val childrenCount: Int
    fun addChildDeclaration(child: IrMemberDeclaration)
    fun removeChildDeclaration(child: IrMemberDeclaration)
    fun removeAllChildDeclarations()

    fun <D> acceptChildDeclarations(visitor: IrElementVisitor<Unit, D>, data: D)
}

interface IrCompoundDeclaration : IrDeclaration, IrDeclarationOwnerN

interface IrMemberDeclaration : IrDeclaration {
    override var parent: IrDeclarationOwner?

    fun setTreeLocation(parent: IrDeclarationOwner?, index: Int)
}

abstract class IrDeclarationOwnerNBase(
        startOffset: Int,
        endOffset: Int
) : IrElementBase(startOffset, endOffset), IrDeclarationOwnerN {
    protected val childDeclarations: MutableList<IrMemberDeclaration> = ArrayList()

    override val childrenCount: Int
        get() = childDeclarations.size

    override fun getChildDeclaration(index: Int): IrMemberDeclaration? =
            childDeclarations.getOrNull(index)

    override fun addChildDeclaration(child: IrMemberDeclaration) {
        child.setTreeLocation(this, childDeclarations.size)
        childDeclarations.add(child)
    }

    override fun removeChildDeclaration(child: IrMemberDeclaration) {
        validateChild(child)
        childDeclarations.removeAt(child.indexInParent)
        for (i in child.indexInParent..childDeclarations.size - 1) {
            childDeclarations[i].setTreeLocation(this, i)
        }
        child.detach()
    }

    override fun removeAllChildDeclarations() {
        childDeclarations.forEach { it.detach() }
        childDeclarations.clear()
    }

    override fun replaceChildDeclaration(oldChild: IrMemberDeclaration, newChild: IrMemberDeclaration) {
        validateChild(oldChild)
        childDeclarations[oldChild.indexInParent] = newChild
        newChild.setTreeLocation(this, oldChild.indexInParent)
        oldChild.detach()
    }

    override fun <D> acceptChildDeclarations(visitor: IrElementVisitor<Unit, D>, data: D) {
        childDeclarations.forEach { it.accept(visitor, data) }
    }
}

// TODO synchronization?
abstract class IrCompoundDeclarationBase(
        startOffset: Int,
        endOffset: Int,
        override val originKind: IrDeclarationOriginKind
) : IrDeclarationOwnerNBase(startOffset, endOffset), IrCompoundDeclaration {
    override var indexInParent: Int = IrDeclaration.DETACHED_INDEX

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        acceptChildDeclarations(visitor, data)
    }
}

abstract class IrCompoundMemberDeclarationBase(
        startOffset: Int,
        endOffset: Int,
        originKind: IrDeclarationOriginKind
) : IrCompoundDeclarationBase(startOffset, endOffset, originKind), IrMemberDeclaration {
    private var parentImpl: IrDeclarationOwner? = null

    override var parent: IrDeclarationOwner?
        get() = parentImpl!!
        set(newParent) {
            parentImpl = newParent
        }

    override fun setTreeLocation(parent: IrDeclarationOwner?, index: Int) {
        this.parentImpl = parent
        this.indexInParent = index
    }
}

abstract class IrMemberDeclarationBase(
        startOffset: Int,
        endOffset: Int,
        originKind: IrDeclarationOriginKind
) : IrDeclarationBase(startOffset, endOffset, originKind), IrMemberDeclaration {
    private var parentImpl: IrDeclarationOwner? = null

    override var parent: IrDeclarationOwner?
        get() = parentImpl!!
        set(newParent) {
            parentImpl = newParent
        }

    override fun setTreeLocation(parent: IrDeclarationOwner?, index: Int) {
        this.parentImpl = parent
        this.indexInParent = index
    }
}

fun IrMemberDeclaration.detach() {
    setTreeLocation(null, IrDeclaration.DETACHED_INDEX)
}

fun IrDeclarationOwner.validateChild(child: IrMemberDeclaration) {
    assert(child.parent == this && getChildDeclaration(child.indexInParent) == child) { "Invalid child: $child" }
}
