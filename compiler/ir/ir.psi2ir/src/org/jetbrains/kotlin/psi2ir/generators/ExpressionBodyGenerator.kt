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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturnImpl
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class ExpressionBodyGenerator(val scopeOwner: CallableDescriptor, override val context: GeneratorContext): BodyGenerator {
    override val scope = Scope(scopeOwner)

    fun generateFunctionBody(ktBody: KtExpression): IrExpression {
        resetInternalContext()

        val irBodyExpression = createStatementGenerator().generateExpression(ktBody)

        val irBodyExpressionAsBlock =
                if (ktBody is KtBlockExpression)
                    irBodyExpression
                else IrBlockImpl(ktBody.startOffset, ktBody.endOffset, null, false).apply {
                    addStatement(IrReturnImpl(ktBody.startOffset, ktBody.endOffset, scopeOwner, irBodyExpression))
                }

        postprocessFunctionBody()

        return irBodyExpressionAsBlock
    }

    private fun resetInternalContext() {
    }

    private fun postprocessFunctionBody() {
    }

    fun generatePropertyInitializerBody(ktInitializer: KtExpression): IrExpression =
            createStatementGenerator().generateExpression(ktInitializer)

    private fun createStatementGenerator() =
            StatementGenerator(context, scopeOwner, this, scope)
}

