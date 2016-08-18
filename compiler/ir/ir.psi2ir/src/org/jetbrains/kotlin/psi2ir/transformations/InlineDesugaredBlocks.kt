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

package org.jetbrains.kotlin.psi2ir.transformations

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.detach
import org.jetbrains.kotlin.ir.expressions.IrBlockExpression
import org.jetbrains.kotlin.ir.expressions.IrBlockExpressionImpl
import org.jetbrains.kotlin.ir.replaceWith
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

fun inlineDesugaredBlocks(element: IrElement) {
    element.accept(InlineDesugaredBlocks(), null)
}

class InlineDesugaredBlocks : IrElementVisitor<Unit, Nothing?> {
    override fun visitElement(element: IrElement, data: Nothing?) {
        element.acceptChildren(this, data)
    }

    override fun visitBlockExpression(expression: IrBlockExpression, data: Nothing?) {
        val transformedBlock = IrBlockExpressionImpl(
                expression.startOffset, expression.endOffset, expression.type,
                expression.hasResult, expression.operator
        )
        for (statement in expression.statements) {
            statement.accept(this, data)
            if (statement is IrBlockExpression && statement.operator != null) {
                statement.statements.forEach {
                    transformedBlock.addStatement(it.detach())
                }
            }
            else {
                transformedBlock.addStatement(statement.detach())
            }
        }
        expression.replaceWith(transformedBlock)
    }
}
