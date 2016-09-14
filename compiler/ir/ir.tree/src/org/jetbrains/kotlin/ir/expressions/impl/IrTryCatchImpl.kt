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

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTryCatch
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.SmartList

class IrTryCatchImpl(startOffset: Int, endOffset: Int, type: KotlinType) :
        IrExpressionBase(startOffset, endOffset, type), IrTryCatch {
    override lateinit var tryResult: IrExpression

    private val catchClauseParameters = SmartList<VariableDescriptor>()
    private val catchClauseResults = SmartList<IrExpression>()

    override val catchClausesCount: Int get() = catchClauseResults.size

    fun addCatchClause(parameter: VariableDescriptor, result: IrExpression) {
        catchClauseParameters.add(parameter)
        catchClauseResults.add(result)
    }

    override fun getNthCatchParameter(n: Int): VariableDescriptor? =
            catchClauseParameters.getOrNull(n)

    override fun getNthCatchResult(n: Int): IrExpression? =
            catchClauseResults.getOrNull(n)

    override fun putNthCatchParameter(n: Int, variableDescriptor: VariableDescriptor) {
        catchClauseParameters[n] = variableDescriptor
    }

    override fun putNthCatchResult(n: Int, expression: IrExpression) {
        catchClauseResults[n] = expression
    }

    override var finallyExpression: IrExpression? = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitTryCatch(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        tryResult.accept(visitor, data)
        catchClauseResults.forEach { it.accept(visitor, data) }
        finallyExpression?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        tryResult = tryResult.transform(transformer, data)
        catchClauseResults.forEachIndexed { i, irExpression ->
            catchClauseResults[i] = irExpression.transform(transformer, data)
        }
    }
}