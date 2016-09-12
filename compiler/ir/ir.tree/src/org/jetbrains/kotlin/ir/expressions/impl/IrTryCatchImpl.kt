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
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTryCatch
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBase
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.SmartList

class IrTryCatchImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType
) : IrExpressionBase(startOffset, endOffset, type), IrTryCatch {
    private var tryResultImpl: IrExpression? = null
    override var tryResult: IrExpression
        get() = tryResultImpl!!
        set(value) {
            tryResultImpl?.detach()
            tryResultImpl = value
            value.setTreeLocation(this, TRY_RESULT_SLOT)
        }

    private val catchClauseParameters = SmartList<VariableDescriptor>()
    private val catchClauseResults = SmartList<IrExpression>()

    override val catchClausesCount: Int get() = catchClauseResults.size

    fun addCatchClause(parameter: VariableDescriptor, result: IrExpression) {
        result.setTreeLocation(this, catchClausesCount)
        catchClauseParameters.add(parameter)
        catchClauseResults.add(result)
    }

    override fun getNthCatchParameter(n: Int): VariableDescriptor? =
            catchClauseParameters.getOrNull(n)

    override fun getNthCatchResult(n: Int): IrExpression? =
            catchClauseResults.getOrNull(n)

    override var finallyExpression: IrExpression? = null
        set(value) {
            field?.detach()
            field = value
            value?.setTreeLocation(this, FINALLY_EXPRESSION_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when {
                slot == TRY_RESULT_SLOT ->
                    tryResult
                slot >= 0 ->
                    catchClauseResults.getOrNull(slot)
                slot == FINALLY_EXPRESSION_SLOT ->
                    finallyExpression
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when {
            slot == TRY_RESULT_SLOT ->
                tryResult = newChild.assertCast()
            slot >= 0 ->
                putCatchClauseElement(catchClauseResults, newChild, slot)
            slot == FINALLY_EXPRESSION_SLOT ->
                finallyExpression = newChild.assertCast()
            else ->
                throwNoSuchSlot(slot)
        }
    }

    private inline fun <reified T : IrElement> putCatchClauseElement(list: MutableList<T>, newChild: IrElement, slot: Int) {
        if (slot < 0 || slot >= list.size) throwNoSuchSlot(slot)

        list[slot].detach()
        list[slot] = newChild.assertCast()
        newChild.setTreeLocation(this, slot)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitTryCatch(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        tryResult.accept(visitor, data)
        for (index in 0 ..catchClausesCount - 1) {
            catchClauseResults[index].accept(visitor, data)
        }
        finallyExpression?.accept(visitor, data)
    }
}