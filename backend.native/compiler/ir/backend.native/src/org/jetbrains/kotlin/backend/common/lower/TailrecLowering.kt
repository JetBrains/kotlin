/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.DeepCopyIrTreeWithDeclarations
import org.jetbrains.kotlin.backend.common.FunctionLoweringPass
import org.jetbrains.kotlin.backend.common.collectTailRecursionCalls
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.backend.common.descriptors.getOriginalParameter
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.getDefault
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This pass lowers tail recursion calls in `tailrec` functions.
 *
 * Note: it currently can't handle local functions and classes declared in default arguments.
 * See [DeepCopyIrTreeWithDeclarations].
 */
internal class TailrecLowering(val context: BackendContext) : FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        lowerTailRecursionCalls(context, irFunction)
    }
}

private fun lowerTailRecursionCalls(context: BackendContext, irFunction: IrFunction) {
    val tailRecursionCalls = collectTailRecursionCalls(irFunction)
    if (tailRecursionCalls.isEmpty()) {
        return
    }

    val descriptor = irFunction.descriptor
    val oldBody = irFunction.body as IrBlockBody
    val builder = context.createIrBuilder(descriptor).at(oldBody)

    val parameters = descriptor.explicitParameters

    irFunction.body = builder.irBlockBody {
        // Define variables containing current values of parameters:
        val parameterToVariable = parameters.associate {
            it to irTemporaryVar(irGet(it), nameHint = it.suggestVariableName()).symbol
        }
        // (these variables are to be updated on any tail call).

        +irWhile().apply {
            val loop = this
            condition = irTrue()

            body = irBlock(startOffset, endOffset, resultType = context.builtIns.unitType) {
                // Read variables containing current values of parameters:
                val parameterToNew = parameters.associate {
                    val variable = parameterToVariable[it]!!
                    it to defineTemporary(irGet(variable), nameHint = it.suggestVariableName())
                }

                val transformer = BodyTransformer(builder, irFunction, loop,
                        parameterToNew, parameterToVariable, tailRecursionCalls)

                oldBody.statements.forEach {
                    +it.transform(transformer, null)
                }

                +irBreak(loop)
            }
        }
    }
}

private class BodyTransformer(
        val builder: IrBuilderWithScope,
        val irFunction: IrFunction,
        val loop: IrLoop,
        val parameterToNew: Map<ParameterDescriptor, ValueDescriptor>,
        val parameterToVariable: Map<ParameterDescriptor, IrVariableSymbol>,
        val tailRecursionCalls: Set<IrCall>
) : IrElementTransformerVoid() {

    val parameters = irFunction.descriptor.explicitParameters

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        expression.transformChildrenVoid(this)
        val value = parameterToNew[expression.descriptor] ?: return expression
        return builder.at(expression).irGet(value)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)
        if (expression !in tailRecursionCalls) {
            return expression
        }

        return builder.at(expression).genTailCall(expression)
    }

    private fun IrBuilderWithScope.genTailCall(expression: IrCall) = this.irBlock(expression) {
        // Get all specified arguments:
        val parameterToArgument = expression.getArguments().map { (parameter, argument) ->
            expression.descriptor.getOriginalParameter(parameter) to argument
        }

        // For each specified argument set the corresponding variable to it in the correct order:
        parameterToArgument.forEach { (parameter, argument) ->
            at(argument)
            // Note that argument can use values of parameters, so it is important that
            // references to parameters are mapped using `parameterToNew`, not `parameterToVariable`.
            +irSetVar(parameterToVariable[parameter]!!, argument)
        }

        val specifiedParameters = parameterToArgument.map { (parameter, _) -> parameter }.toSet()

        // For each unspecified argument set the corresponding variable to default:
        parameters.filter { it !in specifiedParameters }.forEach { parameter ->

            val originalDefaultValue = irFunction.getDefaultArgumentExpression(parameter) ?:
                    throw Error("no argument specified for $parameter")

            // Copy default value, mapping parameters to variables containing freshly computed arguments:
            val defaultValue = originalDefaultValue.transform(object : DeepCopyIrTreeWithDeclarations() {
                override fun mapValueReference(descriptor: ValueDescriptor): ValueDescriptor {
                    return parameterToVariable[descriptor]?.descriptor ?: super.mapValueReference(descriptor)
                }
            }, data = null)

            +irSetVar(parameterToVariable[parameter]!!, defaultValue)
        }

        // Jump to the entry:
        +irContinue(loop)
    }
}

private fun IrFunction.getDefaultArgumentExpression(parameter: ParameterDescriptor): IrExpression? {
    if (parameter !is ValueParameterDescriptor) {
        return null
    }

    val body = this.getDefault(parameter) ?: return null

    if (body !is IrExpressionBody) {
        throw Error("unexpected default argument body: $body")
    }

    return body.expression
}

private fun ParameterDescriptor.suggestVariableName(): String = if (name.isSpecial) {
    val oldNameStr = name.asString()
    "$" + oldNameStr.substring(1, oldNameStr.length - 1)
} else {
    name.identifier
}