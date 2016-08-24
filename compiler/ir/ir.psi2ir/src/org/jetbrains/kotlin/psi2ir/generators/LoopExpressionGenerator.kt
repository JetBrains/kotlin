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

import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.KtWhileExpressionBase
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class LoopExpressionGenerator(val statementGenerator: StatementGenerator) {
    fun generateWhileExpression(expression: KtWhileExpression): IrExpression =
            generateConditionalLoop(expression, IrWhileLoopImpl(expression.startOffset, expression.endOffset, IrOperator.WHILE_LOOP))

    fun generateDoWhileExpression(expression: KtDoWhileExpression): IrExpression =
            generateConditionalLoop(expression, IrDoWhileLoopImpl(expression.startOffset, expression.endOffset, IrOperator.DO_WHILE_LOOP))

    private fun generateConditionalLoop(expression: KtWhileExpressionBase, irLoop: IrLoop): IrLoop {
        statementGenerator.expressionBodyGenerator.putLoop(expression, irLoop)
        irLoop.condition = statementGenerator.generateExpression(expression.condition!!)
        irLoop.body = statementGenerator.generateExpression(expression.body!!)
        return irLoop
    }
}