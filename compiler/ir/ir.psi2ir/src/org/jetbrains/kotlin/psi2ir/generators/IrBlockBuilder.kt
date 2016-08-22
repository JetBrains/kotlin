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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.SmartList

class IrBlockBuilder(val startOffset: Int, val endOffset: Int, val irOperator: IrOperator, val generator: IrBodyGenerator) {
    private val statements = SmartList<IrStatement>()
    private var resultType: KotlinType? = null
    private var hasResult = false
    val scope: Scope get() = generator.scope

    fun <T : IrStatement?> add(irStatement: T): T {
        if (irStatement != null) {
            statements.add(irStatement)
        }
        return irStatement
    }

    fun <T : IrExpression> result(irExpression: T): T {
        resultType = irExpression.type
        hasResult = true
        return irExpression
    }

    fun build() =
            if (statements.size == 1)
                statements[0]
            else
                IrBlockImpl(startOffset, endOffset, resultType, hasResult, irOperator).apply {
                    statements.forEach { addStatement(it) }
                }
}

fun IrBodyGenerator.block(ktElement: KtElement, irOperator: IrOperator, body: IrBlockBuilder.() -> Unit) =
        IrBlockBuilder(ktElement.startOffset, ktElement.endOffset, irOperator, this).apply(body).build()

fun IrBodyGenerator.block(irExpression: IrExpression, irOperator: IrOperator, body: IrBlockBuilder.() -> Unit) =
        IrBlockBuilder(irExpression.startOffset, irExpression.endOffset, irOperator, this).apply(body).build()