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

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.getArgumentsWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This pass lowers tail recursion calls in `tailrec` functions.
 *
 * Note: it currently can't handle local functions and classes declared in default arguments.
 * See [deepCopyWithVariables].
 */
class TailrecLowering(val context: BackendContext) : FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        lowerTailRecursionCalls(context, irFunction)
    }
}

private fun lowerTailRecursionCalls(context: BackendContext, irFunction: IrFunction) {
    val tailRecursionCalls = collectTailRecursionCalls(irFunction)
    if (tailRecursionCalls.isEmpty()) {
        return
    }

    val oldBody = irFunction.body as IrBlockBody
    val builder = context.createIrBuilder(irFunction.symbol).at(oldBody)

    val parameters = irFunction.explicitParameters

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
                    it to irTemporary(irGet(variable), nameHint = it.suggestVariableName()).symbol
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
        val parameterToNew: Map<IrValueParameterSymbol, IrValueSymbol>,
        val parameterToVariable: Map<IrValueParameterSymbol, IrVariableSymbol>,
        val tailRecursionCalls: Set<IrCall>
) : IrElementTransformerVoid() {

    val parameters = irFunction.explicitParameters

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        expression.transformChildrenVoid(this)
        val value = parameterToNew[expression.symbol] ?: return expression
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
        val parameterToArgument = expression.getArgumentsWithSymbols().map { (parameter, argument) ->
            parameter to argument
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

            val originalDefaultValue = parameter.owner.defaultValue?.expression ?:
                    throw Error("no argument specified for $parameter")

            // Copy default value, mapping parameters to variables containing freshly computed arguments:
            val defaultValue = originalDefaultValue
                    .deepCopyWithVariables()
                    .transform(object : IrElementTransformerVoid() {

                        override fun visitGetValue(expression: IrGetValue): IrExpression {
                            expression.transformChildrenVoid(this)

                            val variableSymbol = parameterToVariable[expression.symbol] ?: return expression
                            return IrGetValueImpl(
                                    expression.startOffset, expression.endOffset,
                                    variableSymbol, expression.origin
                            )
                        }
                    }, data = null)

            +irSetVar(parameterToVariable[parameter]!!, defaultValue)
        }

        // Jump to the entry:
        +irContinue(loop)
    }
}

private fun IrValueParameterSymbol.suggestVariableName(): String = if (descriptor.name.isSpecial) {
    val oldNameStr = descriptor.name.asString()
    "$" + oldNameStr.substring(1, oldNameStr.length - 1)
} else {
    descriptor.name.identifier
}