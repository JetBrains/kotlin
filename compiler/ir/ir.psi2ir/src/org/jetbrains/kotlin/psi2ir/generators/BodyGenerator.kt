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
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.util.*

class BodyGenerator(val scopeOwner: CallableDescriptor, override val context: GeneratorContext): GeneratorWithScope {
    override val scope = Scope(scopeOwner)
    private val loopTable = HashMap<KtExpression, IrLoop>()

    fun generateFunctionBody(ktBody: KtExpression): IrBody {
        resetInternalContext()

        val statementGenerator = createStatementGenerator()

        val irBlockBody = IrBlockBodyImpl(ktBody.startOffset, ktBody.endOffset)
        if (ktBody is KtBlockExpression) {
            for (ktStatement in ktBody.statements) {
                irBlockBody.addStatement(statementGenerator.generateStatement(ktStatement))
            }
        }
        else {
            val irBodyExpression = statementGenerator.generateExpression(ktBody)
            val irReturn = IrReturnImpl(ktBody.startOffset, ktBody.endOffset, scopeOwner, irBodyExpression)
            irBlockBody.addStatement(irReturn)
        }

        postprocessFunctionBody()

        return irBlockBody
    }

    private fun resetInternalContext() {
        loopTable.clear()
    }

    private fun postprocessFunctionBody() {
    }

    fun generatePropertyInitializerBody(ktInitializer: KtExpression): IrBody =
            IrExpressionBodyImpl(ktInitializer.startOffset, ktInitializer.endOffset,
                                 createStatementGenerator().generateExpression(ktInitializer))

    private fun createStatementGenerator() =
            StatementGenerator(context, scopeOwner, this, scope)

    fun putLoop(expression: KtExpression, irLoop: IrLoop) {
        loopTable[expression] = irLoop
    }

    fun getLoop(expression: KtExpression): IrLoop? =
            loopTable[expression]
}

