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

import org.jetbrains.kotlin.ir.SourceLocation
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import java.util.*

interface IrCompoundDeclaration : IrDeclaration {
    fun getChildDeclaration(index: Int): IrMemberDeclaration?
    fun addChildDeclaration(child: IrMemberDeclaration)
    fun replaceChildDeclaration(oldChild: IrMemberDeclaration, newChild: IrMemberDeclaration)
    fun removeAllChildDeclarations()

    // TODO This can be an expensive operation / prohibited for some children.
    fun removeChildDeclaration(child: IrMemberDeclaration)

    fun <D> acceptChildDeclarations(visitor: IrElementVisitor<Unit, D>, data: D)
}

interface IrMemberDeclaration : IrDeclaration {
    fun setTreeLocation(parent: IrCompoundDeclaration?, index: Int)
}

// TODO synchronization?
abstract class IrCompoundDeclarationBase(
        sourceLocation: SourceLocation,
        kind: IrDeclarationKind
) : IrDeclarationBase(sourceLocation, kind), IrCompoundDeclaration {
    protected val childDeclarations: MutableList<IrMemberDeclaration> = ArrayList()

    override fun getChildDeclaration(index: Int): IrMemberDeclaration? =
            childDeclarations.getOrNull(index)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        acceptChildDeclarations(visitor, data)
    }

    override fun <D> acceptChildDeclarations(visitor: IrElementVisitor<Unit, D>, data: D) {
        childDeclarations.forEach { it.accept(visitor, data) }
    }

    override fun addChildDeclaration(child: IrMemberDeclaration) {
        child.setTreeLocation(this, childDeclarations.size)
        childDeclarations.add(child)
    }

    override fun removeChildDeclaration(child: IrMemberDeclaration) {
        validateChild(child)
        childDeclarations.removeAt(child.index)
        for (i in child.index ..childDeclarations.size - 1) {
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
        childDeclarations[oldChild.index] = newChild
        newChild.setTreeLocation(this, oldChild.index)
        oldChild.detach()
    }
}

fun IrMemberDeclaration.detach() {
    setTreeLocation(null, IrDeclaration.DETACHED_INDEX)
}

fun IrCompoundDeclaration.validateChild(child: IrMemberDeclaration) {
    assert(child.parent == this && getChildDeclaration(child.index) == child) { "Invalid child: $child" }
}
