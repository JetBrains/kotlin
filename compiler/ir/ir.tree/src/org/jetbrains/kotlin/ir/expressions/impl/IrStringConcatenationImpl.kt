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

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class IrStringConcatenationImpl(startOffset: Int, endOffset: Int, type: KotlinType) :
    IrExpressionBase(startOffset, endOffset, type), IrStringConcatenation {
    constructor(startOffset: Int, endOffset: Int, type: KotlinType, arguments: Collection<IrExpression>) : this(
        startOffset,
        endOffset,
        type
    ) {
        this.arguments.addAll(arguments)
    }

    override val arguments: MutableList<IrExpression> = ArrayList()

    override fun addArgument(argument: IrExpression) {
        arguments.add(argument)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitStringConcatenation(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        arguments.forEachIndexed { i, irExpression ->
            arguments[i] = irExpression.transform(transformer, data)
        }
    }
}