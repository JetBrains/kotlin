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

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTryCatchImpl
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext

class TryCatchExpressionGenerator(val statementGenerator: StatementGenerator) : GeneratorWithScope {
    override val scope: Scope get() = statementGenerator.scope
    override val context: GeneratorContext get() = statementGenerator.context

    fun generateTryCatch(ktTry: KtTryExpression): IrExpression {
        val resultType = getInferredTypeWithSmartcasts(ktTry)
        val irTryCatch = IrTryCatchImpl(ktTry.startOffset, ktTry.endOffset, resultType)

        irTryCatch.tryResult = statementGenerator.generateExpression(ktTry.tryBlock)

        for (ktCatchClause in ktTry.catchClauses) {
            val ktCatchParameter = ktCatchClause.catchParameter!!
            val ktCatchBody = ktCatchClause.catchBody!!

            val catchParameterDescriptor = getOrFail(BindingContext.VALUE_PARAMETER, ktCatchParameter)

            val irCatchResult = statementGenerator.generateExpression(ktCatchBody)

            irTryCatch.addCatchClause(catchParameterDescriptor, irCatchResult)
        }

        irTryCatch.finallyExpression = ktTry.finallyBlock?.let{ statementGenerator.generateExpression(it.finalExpression) }

        return irTryCatch
    }
}