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
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
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
        declarations.add(member)
    }

    fun addAll(members: List<IrDeclaration>) {
        declarations.addAll(members)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitClass(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        declarations.forEachIndexed { i, irDeclaration ->
            declarations[i] = irDeclaration.transform(transformer, data) as IrDeclaration
        }
    }
}
