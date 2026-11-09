/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.util.OperatorNameConventions

abstract class NumericForLoopHeader<T : NumericHeaderInfo>(
    val headerInfo: T,
    builder: DeclarationIrBuilder,
    protected val context: CommonBackendContext
) : ForLoopHeader {

    override val consumesLoopVariableComponents = false

    val inductionVariable: IrVariable

    protected val stepVariable: IrVariable?
    val stepExpression: IrExpression

    protected val lastVariableIfCanCacheLast: IrVariable?
    protected val lastExpression: IrExpression
        // If this is not `IrExpressionWithCopy`, then it is `<IrGetValue>.getSize()` built in `IndexedGetIterationHandler`.
        // It is therefore safe to deep-copy as it does not contain any functions or classes.
        get() = field.shallowCopyOrNull() ?: field.deepCopyWithSymbols()

    protected val symbols = context.symbols

    init {
        with(builder) {
            with(headerInfo.progressionType) {
                // For this loop:
                //
                //   for (i in first()..last() step step())
                //
                // We need to cast first(), last(). and step() to conform to the progression type so
                // that operations on the induction variable within the loop are more efficient.
                //
                // In the above example, if first() is a Long and last() is an Int, this creates a
                // LongProgression so last() should be cast to a Long.
                inductionVariable =
                    scope.createTemporaryVariable(
                        headerInfo.first.asElementType(),
                        nameHint = inductionVariableName,
                        isMutable = true,
                        origin = this@NumericForLoopHeader.context.inductionVariableOrigin,
                        irType = elementClass.defaultType,
                        inventUniqueName = false,
                    )

                // Due to features of PSI2IR we can obtain nullable arguments here while actually
                // they are non-nullable (the frontend takes care about this). So we need to cast
                // them to non-nullable.
                // TODO: Confirm if casting to non-nullable is still necessary
                val last = headerInfo.last.asElementType()

                if (headerInfo.canCacheLast) {
                    val [variable, expression] = createLoopTemporaryVariableIfNecessary(last, nameHint = "last")
                    lastVariableIfCanCacheLast = variable
                    lastExpression = expression.shallowCopy()
                } else {
                    lastVariableIfCanCacheLast = null
                    lastExpression = last
                }

                val [tmpStepVar, tmpStepExpression] =
                    createLoopTemporaryVariableIfNecessary(
                        ensureNotNullable(headerInfo.step.asStepType()),
                        nameHint = "step",
                        irType = stepClass.defaultType
                    )
                stepVariable = tmpStepVar
                stepExpression = tmpStepExpression
            }
        }
    }

    private fun DeclarationIrBuilder.ensureNotNullable(expression: IrExpression) =
        if (expression.type is IrSimpleType && expression.type.isNullable()) {
            irImplicitCast(expression, expression.type.makeNotNull())
        } else {
            expression
        }

    /** Statement used to increment the induction variable. */
    protected fun incrementInductionVariable(builder: DeclarationIrBuilder): IrStatement = TODO()

    protected fun buildLoopCondition(builder: DeclarationIrBuilder): IrExpression { TODO() }
}
