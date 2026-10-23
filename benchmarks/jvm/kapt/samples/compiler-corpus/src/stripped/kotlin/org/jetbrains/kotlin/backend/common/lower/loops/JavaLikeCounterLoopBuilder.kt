/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.util.OperatorNameConventions

class JavaLikeCounterLoopBuilder(private val context: CommonBackendContext) {
    private val booleanNot =
        context.irBuiltIns.booleanClass.owner.findDeclaration<IrSimpleFunction> {
            it.name == OperatorNameConventions.NOT
        } ?: error("No '${OperatorNameConventions.NOT}' in ${context.irBuiltIns.booleanClass.owner.render()}")

    fun buildJavaLikeDoWhileCounterLoop(
        oldLoop: IrLoop,
        newLoopCondition: IrExpression,
        newBody: IrExpression?,
        loopOrigin: IrStatementOrigin?
    ): LoopReplacement { TODO() }


    private fun createNegatedConditionCheck(
        newLoopCondition: IrExpression,
        doWhileLoop: IrDoWhileLoop
    ): IrWhenImpl { TODO() }

    fun moveInductionVariableUpdateToLoopCondition(doWhileLoop: IrDoWhileLoop) { TODO() }
}