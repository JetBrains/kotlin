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

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType


interface IrCallableReference : IrDeclarationReference {
    override val descriptor: CallableDescriptor

    // TODO closure
}

class IrCallableReferenceImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        descriptor: CallableDescriptor
) : IrDeclarationReferenceBase<CallableDescriptor>(startOffset, endOffset, type, descriptor), IrCallableReference {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitCallableReference(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // TODO
    }

    override fun getChild(slot: Int): IrElement? {
        // TODO
        return null
    }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        // TODO
    }
}