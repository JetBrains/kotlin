/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callRichFunctionReference
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.createSequenceWhile
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBreak
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference

internal class FindBodyReplacementCreator(
    val isFirst: Boolean,
    val context: JvmBackendContext,
    val builder: IrBuilderWithScope,
    val parent: IrDeclarationParent,
    val sequenceData: SequenceData
) : ConsumerBodyReplacementCreator() {
    override fun create(expression: IrCall): IrExpression? {
        val findPredicate = expression.arguments.getOrNull(1) as? IrRichFunctionReference ?: return null
        val loop = builder.createSequenceWhile()
        val resultVariable = builder.scope.createTemporaryVariable(builder.irNull(), isMutable = true, irType = expression.type)
        val findBody = { loopVariable: IrVariable ->
            builder.irBlock {
                val predicateCall = callRichFunctionReference(findPredicate, parent, irGet(loopVariable))
                val isFoundVariable = irTemporary(predicateCall)
                val thenPart = irBlock {
                    +irSet(resultVariable, irGet(loopVariable))
                    if (isFirst) +irBreak(loop)
                }
                +irIfThen(context.irBuiltIns.unitType, irGet(isFoundVariable), thenPart)
            }
        }

        val updatedSequenceData = sequenceData.addDeclaration(resultVariable)
        return lowerAndReturnVariable(builder, parent, updatedSequenceData, loop, resultVariable, findBody, expression.type)
    }
}
