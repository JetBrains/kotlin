/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.consumers

import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.FilterVersion
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callRichFunctionReference
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.createSequenceWhile
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies.irAsNotNull
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.util.functions

internal class FilterToBodyReplacementCreator(
    val version: FilterVersion,
    val context: JvmBackendContext,
    val builder: IrBuilderWithScope,
    val parent: IrDeclarationParent,
    val sequenceData: SequenceData
) : ConsumerBodyReplacementCreator() {
    override fun create(expression: IrCall): IrExpression? {
        val destination = expression.arguments.getOrNull(1) ?: return null
        val predicate = expression.arguments.getOrNull(2) as? IrRichFunctionReference
        if (version !is FilterVersion.FilterNotNull && predicate == null) return null

        val addFunction = context.irBuiltIns.mutableCollectionClass.owner.functions.singleOrNull {
            it.name.asString() == "add" && it.parameters.size == 2
        } ?: return expression
        val loop = builder.createSequenceWhile()
        val destinationVariable = builder.scope.createTemporaryVariable(destination, "filterToDestination")
        val updatedSequenceData = sequenceData.addDeclaration(destinationVariable)
        val body = { loopVariable: IrVariable ->
            builder.irBlock {
                val destinationAddCall = irCall(addFunction).apply {
                    arguments[0] = irGet(destinationVariable)
                    if (version == FilterVersion.FilterNotNull) {
                        arguments[1] = irAsNotNull(irGet(loopVariable))
                    } else {
                        arguments[1] = irGet(loopVariable)
                    }
                }
                val shouldAddVariableCheck = when (version) {
                    FilterVersion.Filter -> callRichFunctionReference(predicate!!, parent, irGet(loopVariable))
                    FilterVersion.FilterNot -> irNot(callRichFunctionReference(predicate!!, parent, irGet(loopVariable)))
                    FilterVersion.FilterNotNull -> irNot(irEquals(irGet(loopVariable), irNull()))
                }
                +irIfThen(
                    context.irBuiltIns.unitType,
                    shouldAddVariableCheck,
                    destinationAddCall
                )
            }
        }

        return lowerAndReturnVariable(builder, parent, updatedSequenceData, loop, destinationVariable, body, destination.type)
    }
}
