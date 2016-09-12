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

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import java.util.*

class IrClassImpl(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        override val descriptor: ClassDescriptor
) : IrDeclarationBase(startOffset, endOffset, origin), IrClass {
    constructor(
            startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, descriptor: ClassDescriptor,
            members: List<IrDeclaration>
    ) : this(startOffset, endOffset, origin, descriptor) {
        addAll(members)
    }

    override val declarations: MutableList<IrDeclaration> = ArrayList()

    fun addMember(member: IrDeclaration) {
        member.setTreeLocation(this, declarations.size)
        declarations.add(member)
    }

    fun addAll(members: List<IrDeclaration>) {
        val originalSize = declarations.size
        declarations.addAll(members)
        members.forEachIndexed { i, irDeclaration -> irDeclaration.setTreeLocation(this, originalSize + i) }
    }

    override fun toBuilder(): IrDeclarationContainer.Builder =
            IrClassBuilderImpl(this)

    override fun getChild(slot: Int): IrElement? =
            declarations.getOrNull(slot)


    override fun replaceChild(slot: Int, newChild: IrElement) {
        declarations.getOrNull(slot)?.detach() ?: throwNoSuchSlot(slot)
        declarations[slot] = newChild.assertCast()
        newChild.setTreeLocation(this, slot)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitClass(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }
}

class IrClassBuilderImpl(
        override val startOffset: Int,
        override val endOffset: Int,
        override var origin: IrDeclarationOrigin,
        override val descriptor: ClassDescriptor,
        override val declarations: MutableList<IrDeclaration>
) : IrClass.Builder {
    constructor(irClass: IrClass) : this(irClass.startOffset, irClass.endOffset, irClass.origin, irClass.descriptor,
                                         irClass.declarations.toMutableList())

    override fun build(): IrClass =
            IrClassImpl(startOffset, endOffset, origin, descriptor, declarations)
}