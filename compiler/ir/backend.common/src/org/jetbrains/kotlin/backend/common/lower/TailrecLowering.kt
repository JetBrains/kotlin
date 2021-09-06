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
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.collectTailRecursionCalls
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This pass lowers tail recursion calls in `tailrec` functions.
 *
 * Note: it currently can't handle local functions and classes declared in default arguments.
 * See [deepCopyWithVariables].
 */
open class TailrecLowering(val context: BackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container is IrFunction) {
            // TODO Shouldn't this be done after local declarations lowering?
            // Lower local declarations
            irBody.acceptChildrenVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunction(declaration: IrFunction) {
                    declaration.acceptChildrenVoid(this)
                    lowerTailRecursionCalls(declaration)
                }
            })

            lowerTailRecursionCalls(container)
        }
    }

    private fun lowerTailRecursionCalls(function: IrFunction) =
        lowerTailRecursionCalls(context, function, useProperComputationOrderOfTailrecDefaultParameters())

    open fun useProperComputationOrderOfTailrecDefaultParameters() = true
}

private fun lowerTailRecursionCalls(
    context: BackendContext,
    irFunction: IrFunction,
    properComputationOrderOfTailrecDefaultParameters: Boolean
) {
    val tailRecursionCalls = collectTailRecursionCalls(irFunction)
    if (tailRecursionCalls.isEmpty()) {
        return
    }

    val oldBody = irFunction.body as IrBlockBody
    val oldBodyStatements = ArrayList(oldBody.statements)
    val builder = context.createIrBuilder(irFunction.symbol).at(oldBody)

    val parameters = irFunction.explicitParameters

    oldBody.statements.clear()
    oldBody.statements += builder.irBlockBody {
        // Define variables containing current values of parameters:
        val parameterToVariable = parameters.associateWith {
            createTmpVariable(irGet(it), nameHint = it.symbol.suggestVariableName(), isMutable = true)
        }
        // (these variables are to be updated on any tail call).

        +irWhile().apply {
            val loop = this
            condition = irTrue()

            body = irBlock(startOffset, endOffset, resultType = context.irBuiltIns.unitType) {
                // Read variables containing current values of parameters:
                val parameterToNew = parameters.associateWith {
                    createTmpVariable(irGet(parameterToVariable[it]!!), nameHint = it.symbol.suggestVariableName())
                }

                val transformer = BodyTransformer(
                    builder, irFunction, loop,
                    parameterToNew, parameterToVariable, tailRecursionCalls,
                    properComputationOrderOfTailrecDefaultParameters
                )

                oldBodyStatements.forEach {
                    +it.transformStatement(transformer)
                }

                +irBreak(loop)
            }
        }
    }.statements
}

private class BodyTransformer(
    val builder: IrBuilderWithScope,
    val irFunction: IrFunction,
    val loop: IrLoop,
    val parameterToNew: Map<IrValueParameter, IrValueDeclaration>,
    val parameterToVariable: Map<IrValueParameter, IrVariable>,
    val tailRecursionCalls: Set<IrCall>,
    val properComputationOrderOfTailrecDefaultParameters: Boolean
) : VariableRemapper(parameterToNew) {

    val parameters = irFunction.explicitParameters

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)
        if (expression !in tailRecursionCalls) {
            return expression
        }
        return builder.at(expression).genTailCall(expression)
    }

    private fun IrBuilderWithScope.genTailCall(expression: IrCall) = this.irBlock(expression) {
        // Get all specified arguments:
        val parameterToArgument = expression.getArgumentsWithIr().associateTo(mutableMapOf()) { (parameter, argument) ->
            // Note that we create `val`s for those parameters so that if some default value contains an object
            // that captures another parameter, it won't capture it as a mutable ref.
            parameter to irTemporary(argument)
        }

        // For each unspecified argument set the corresponding variable to default:
        parameters
            .filter { it !in parameterToArgument }
            .let { if (properComputationOrderOfTailrecDefaultParameters) it else it.asReversed() }
            .associateWithTo(parameterToArgument) { parameter ->
                val originalDefaultValue = parameter.defaultValue?.expression ?: throw Error("no argument specified for $parameter")
                // Copy default value, mapping parameters to variables containing freshly computed arguments:
                val defaultValue = originalDefaultValue
                    .deepCopyWithVariables().patchDeclarationParents(parent)
                    .transform(object : VariableRemapper(parameterToArgument) {
                        override fun visitGetValue(expression: IrGetValue): IrExpression {
                            // If this parameter references a different parameter declared later, produce null:
                            if (expression.symbol.owner.let { it is IrValueParameter && it.parent == irFunction && it !in parameterToArgument })
                                return IrConstImpl.defaultValueForType(startOffset, endOffset, expression.type.makeNullable())
                            return super.visitGetValue(expression)
                        }
                    }, null)
                irTemporary(defaultValue)
            }

        // Copy the new `val`s into the `var`s declared outside the loop:
        parameterToArgument.forEach { (parameter, argument) ->
            at(argument)
            +irSet(parameterToVariable[parameter]!!.symbol, irGet(argument))
        }

        // Jump to the entry:
        +irContinue(loop)
    }
}

private fun IrValueParameterSymbol.suggestVariableName(): String =
    if (owner.name.isSpecial) {
        val oldNameStr = owner.name.asString()
        "$" + oldNameStr.substring(1, oldNameStr.length - 1)
    } else {
        owner.name.identifier
    }