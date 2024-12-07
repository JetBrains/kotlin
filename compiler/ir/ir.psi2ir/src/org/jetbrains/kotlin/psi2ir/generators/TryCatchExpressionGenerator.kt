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

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCatchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.resolve.BindingContext

internal class TryCatchExpressionGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {
    fun generateTryCatch(ktTry: KtTryExpression): IrExpression {
        val resultType = getExpressionTypeWithCoercionToUnitOrFail(ktTry).toIrType()
        val irTryCatch = IrTryImpl(ktTry.startOffsetSkippingComments, ktTry.endOffset, resultType)

        irTryCatch.tryResult = ktTry.tryBlock.genExpr()

        for (ktCatchClause in ktTry.catchClauses) {
            val ktCatchParameter = ktCatchClause.catchParameter!!
            val ktCatchBody = ktCatchClause.catchBody!!
            val catchParameterDescriptor = getOrFail(BindingContext.VALUE_PARAMETER, ktCatchParameter)

            val irCatch = IrCatchImpl(
                ktCatchClause.startOffsetSkippingComments, ktCatchClause.endOffset,
                context.symbolTable.descriptorExtension.declareVariable(
                    ktCatchParameter.startOffsetSkippingComments, ktCatchParameter.endOffset,
                    IrDeclarationOrigin.CATCH_PARAMETER,
                    catchParameterDescriptor, catchParameterDescriptor.type.toIrType()
                ),
                ktCatchBody.genExpr()
            )

            irTryCatch.catches.add(irCatch)
        }

        irTryCatch.finallyExpression = ktTry.finallyBlock?.run { finalExpression.genExpr() }

        return irTryCatch
    }
}
