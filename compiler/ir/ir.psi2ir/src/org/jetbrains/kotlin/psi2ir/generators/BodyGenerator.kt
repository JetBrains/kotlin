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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.util.*

class BodyGenerator(val scopeOwner: CallableDescriptor, override val context: GeneratorContext): GeneratorWithScope {
    override val scope = Scope(scopeOwner)
    private val loopTable = HashMap<KtLoopExpression, IrLoop>()

    fun generateFunctionBody(ktBody: KtExpression): IrBody {
        val statementGenerator = createStatementGenerator()

        val irBlockBody = IrBlockBodyImpl(ktBody.startOffset, ktBody.endOffset)
        if (ktBody is KtBlockExpression) {
            for (ktStatement in ktBody.statements) {
                irBlockBody.addStatement(statementGenerator.generateStatement(ktStatement))
            }
        }
        else {
            val irBodyExpression = statementGenerator.generateExpression(ktBody)
            irBlockBody.addStatement(irBodyExpression.wrapWithReturn())
        }

        return irBlockBody
    }

    private fun IrExpression.wrapWithReturn() =
            if (KotlinBuiltIns.isNothing(type))
                this
            else
                IrReturnImpl(startOffset, endOffset, context.builtIns.nothingType, scopeOwner, this)

    fun generatePropertyInitializerBody(ktInitializer: KtExpression): IrBody =
            IrExpressionBodyImpl(ktInitializer.startOffset, ktInitializer.endOffset,
                                 createStatementGenerator().generateExpression(ktInitializer))

    private fun createStatementGenerator() =
            StatementGenerator(context, scopeOwner, this, scope)

    fun putLoop(expression: KtLoopExpression, irLoop: IrLoop) {
        loopTable[expression] = irLoop
    }

    fun getLoop(expression: KtExpression): IrLoop? =
            loopTable[expression]

    fun generateLambdaBody(ktFun: KtFunctionLiteral): IrBody {
        val statementGenerator = createStatementGenerator()

        val ktBody = ktFun.bodyExpression!!
        val irBlockBody = IrBlockBodyImpl(ktBody.startOffset, ktBody.endOffset)
        if (ktBody is KtBlockExpression) {
            for (ktStatement in ktBody.statements.subList(0, ktBody.statements.size - 1)) {
                irBlockBody.addStatement(statementGenerator.generateStatement(ktStatement))
            }
            val ktReturnedValue = ktBody.statements.last()
            irBlockBody.addStatement(statementGenerator.generateExpression(ktReturnedValue).wrapWithReturn())
        }
        else {
            irBlockBody.addStatement(statementGenerator.generateExpression(ktBody).wrapWithReturn())
        }

        return irBlockBody
    }
}

