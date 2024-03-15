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
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
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
 */
open class TailrecLowering(val context: BackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container is IrFunction) {
            // Lower local declarations
            irBody.acceptChildrenVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                    declaration.acceptChildrenVoid(this)
                    if (declaration.isTailrec) {
                        lowerTailRecursionCalls(declaration)
                    }
                }
            })

            if (container is IrSimpleFunction && container.isTailrec) {
                lowerTailRecursionCalls(container)
            }
        }
    }

    open val useProperComputationOrderOfTailrecDefaultParameters: Boolean
        get() = true

    open fun followFunctionReference(reference: IrFunctionReference): Boolean = false

    open fun nullConst(startOffset: Int, endOffset: Int, type: IrType): IrExpression =
        IrConstImpl.defaultValueForType(startOffset, endOffset, type)
}

private fun TailrecLowering.lowerTailRecursionCalls(irFunction: IrFunction) {
    val (tailRecursionCalls, someCallsAreFromOtherFunctions) = collectTailRecursionCalls(irFunction, ::followFunctionReference)
    if (tailRecursionCalls.isEmpty()) {
        return
    }

    val oldBody = irFunction.body as? IrBlockBody ?: return
    val oldBodyStatements = ArrayList(oldBody.statements)
    val builder = context.createIrBuilder(irFunction.symbol).at(oldBody)

    oldBody.statements.clear()
    oldBody.statements += builder.irBlockBody {
        // `return recursiveCall(...)` is rewritten into assignments to parameters followed by a jump to the start.
        // While we may be able to write to the parameters directly, the recursive call may be inside an inline lambda,
        // so the parameters are captured and assigning to them requires temporarily rewriting their types (see
        // `SharedVariablesLowering`), and that we can't do. So we have to create new `var`s for this purpose.
        // TODO: an optimization pass will rewrite the types of vars back since the lambdas are guaranteed to be inlined
        //  in place (otherwise they can't jump to the start of the function at all), so this is all a waste of CPU time.
        val parameterToVariable = irFunction.explicitParameters.associateWith {
            if (someCallsAreFromOtherFunctions || !it.isAssignable)
                createTmpVariable(irGet(it), nameHint = it.symbol.suggestVariableName(), isMutable = true)
            else
                it
        }

        +irDoWhile().apply loop@{
            body = irBlock(startOffset, endOffset, resultType = context.irBuiltIns.unitType) {
                val transformer = BodyTransformer(
                    this@lowerTailRecursionCalls, builder, irFunction, this@loop, parameterToVariable, tailRecursionCalls
                )
                oldBodyStatements.forEach {
                    +it.transformStatement(transformer)
                }
                +irBreak(this@loop)
            }
            condition = irBlock {
                // The problem with creating new `var`s is that they do not show up in the debugger, so stopping inside
                // a nested call will still display the parameters from the outermost call. To fix this, we need to
                // write the new values back even though the parameters are now otherwise unused.
                for ((parameter, variable) in parameterToVariable.entries) {
                    if (parameter.isAssignable && parameter !== variable) {
                        +irSet(parameter, irGet(variable))
                    }
                }
                +irTrue()
            }
        }
    }.statements

    // TODO BodyTransformer creates temporary variables with wrong parents in nested functions
    oldBody.patchDeclarationParents(irFunction)
}

private class BodyTransformer(
    private val lowering: TailrecLowering,
    private val builder: IrBuilderWithScope,
    irFunction: IrFunction,
    private val loop: IrLoop,
    private val parameterToVariable: Map<IrValueParameter, IrValueDeclaration>,
    private val tailRecursionCalls: Set<IrCall>,
) : VariableRemapper(parameterToVariable) {

    val parameters = irFunction.explicitParameters

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)
        if (expression !in tailRecursionCalls) {
            return expression
        }
        return builder.at(expression).genTailCall(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        if (lowering.followFunctionReference(expression)) {
            expression.symbol.owner.body?.transformChildrenVoid(this)
        }
        return super.visitFunctionReference(expression)
    }

    private fun IrBuilderWithScope.genTailCall(expression: IrCall) = this.irBlock(expression) {
        // Get all specified arguments:
        val parameterToArgument = expression.getArgumentsWithIr().associateTo(mutableMapOf()) { (parameter, argument) ->
            // Note that we create `val`s for those parameters so that if some default value contains an object
            // that captures another parameter, it won't capture it as a mutable ref.
            parameter to irTemporary(argument)
        }
        // Create new null-initialized variables for all other values in case of forward references:
        //   fun f(x: () -> T = { y }, y: T = ...) // in `f()`, `x()` returns `null`
        val defaultValuedParameters = parameters.filter { it !in parameterToArgument }
        defaultValuedParameters.associateWithTo(parameterToArgument) {
            // Note that we intentionally keep the original type of the parameter for the variable even though that violates type safety
            // if it's non-null. This ensures that capture parameters have the same types for all copies of `x`.
            irTemporary(lowering.nullConst(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.type))
        }
        // Now replace those variables with ones containing actual default values. Unused null-valued temporaries will hopefully
        // be optimized out later.
        val remapper = VariableRemapper(parameterToArgument)
        defaultValuedParameters.let { if (lowering.useProperComputationOrderOfTailrecDefaultParameters) it else it.asReversed() }
            .associateWithTo(parameterToArgument) { parameter ->
                val originalDefaultValue = parameter.defaultValue?.expression ?: throw Error("no argument specified for $parameter")
                irTemporary(originalDefaultValue.deepCopyWithSymbols(parent).transform(remapper, null))
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
