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

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrVarargElement
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.SmartList

class IrVarargImpl(
    startOffset: Int,
    endOffset: Int,
    type: KotlinType,
    override val varargElementType: KotlinType
) : IrVararg, IrExpressionBase(startOffset, endOffset, type) {
    constructor(
        startOffset: Int, endOffset: Int, type: KotlinType, varargElementType: KotlinType,
        elements: List<IrVarargElement>
    ) : this(startOffset, endOffset, type, varargElementType) {
        this.elements.addAll(elements)
    }

    override val elements: MutableList<IrVarargElement> = SmartList()

    fun addElement(varargElement: IrVarargElement) {
        elements.add(varargElement)
    }

    override fun putElement(i: Int, element: IrVarargElement) {
        elements[i] = element
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitVararg(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        elements.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        elements.forEachIndexed { i, irVarargElement ->
            elements[i] = irVarargElement.transform(transformer, data) as IrVarargElement
        }
    }
}