/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.intermediate

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi2ir.generators.StatementGenerator

class SpecialExpressionAssignmentReceiver(
    private val statementGenerator: StatementGenerator,
    private val ktExpression: KtExpression,
    private val origin: IrStatementOrigin,
    override val type: IrType
) :
    LValue,
    AssignmentReceiver {

    override fun load(): IrExpression =
        statementGenerator.generateExpression(ktExpression)

    override fun store(irExpression: IrExpression): IrExpression =
        throw AssertionError(
            "This is an expression assignment receiver for ${ktExpression.text}, " +
                    "it can be used only in augmented assignment operator convention for '<op>Assign'"
        )

    override fun assign(withLValue: (LValue) -> IrExpression): IrExpression =
        withLValue(this)
}