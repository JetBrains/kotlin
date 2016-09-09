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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationKind
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrErrorDeclaration
import org.jetbrains.kotlin.ir.throwNoSuchSlot
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrErrorDeclarationImpl(
        startOffset: Int,
        endOffset: Int,
        override val descriptor: DeclarationDescriptor
) : IrDeclarationBase(startOffset, endOffset, IrDeclarationOrigin.DEFINED), IrErrorDeclaration {
    override val declarationKind: IrDeclarationKind get() = IrDeclarationKind.DUMMY

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitErrorDeclaration(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // no children
    }

    override fun getChild(slot: Int): IrElement? = null

    override fun replaceChild(slot: Int, newChild: IrElement) = throwNoSuchSlot(slot)
}