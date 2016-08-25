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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import java.util.*

interface IrClassElement : IrElement

interface IrClass : IrDeclaration {
    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.CLASS

    override val descriptor: ClassDescriptor

    val children: List<IrClassElement>
}

class IrClassImpl(
        startOffset: Int,
        endOffset: Int,
        originKind: IrDeclarationOriginKind,
        override val descriptor: ClassDescriptor
) : IrDeclarationBase(startOffset, endOffset, originKind), IrClass {
    override val children: MutableList<IrClassElement> = ArrayList()

    fun addElement(child: IrClassElement) {
        child.assertDetached()
        child.setTreeLocation(this, children.size)
        children.add(child)
    }

    override fun getChild(slot: Int): IrElement? =
            children.getOrNull(slot)

    override fun replaceChild(slot: Int, newChild: IrElement) {
        newChild.assertDetached()
        children.getOrNull(slot)?.detach() ?: throwNoSuchSlot(slot)
        children[slot] = newChild.assertCast()
        newChild.setTreeLocation(this, slot)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitClass(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        children.forEach { it.accept(visitor, data) }
    }

}
