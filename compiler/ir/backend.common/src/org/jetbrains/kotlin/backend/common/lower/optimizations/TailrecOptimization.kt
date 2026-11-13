/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.optimizations

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBreak
import org.jetbrains.kotlin.ir.builders.irContinue
import org.jetbrains.kotlin.ir.builders.irDoWhile
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.builders.parent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultValueForType
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal interface TailrecOptimization {
    val context: LoweringContext
    val useProperComputationOrderOfTailrecDefaultParameters: Boolean
        get() = true

    fun followFunctionReference(reference: IrFunctionReference): Boolean =
        false

    fun followRichFunctionReference(reference: IrRichFunctionReference): Boolean =
        false

    fun nullConst(startOffset: Int, endOffset: Int, type: IrType): IrExpression =
        IrConstImpl.defaultValueForType(startOffset, endOffset, type)
}

internal fun TailrecOptimization.lowerTailRecursionCalls(
    irFunction: IrFunction,
    someCallsAreFromOtherFunctions: Boolean,
    tailRecursionCalls: Set<IrCall>,
) {
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
        val parameterToVariable = irFunction.parameters.associateWith {
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
                for ([parameter, variable] in parameterToVariable.entries) {
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
    private val tailrecOptimization: TailrecOptimization,
    private val builder: IrBuilderWithScope,
    irFunction: IrFunction,
    private val loop: IrLoop,
    private val parameterToVariable: Map<IrValueParameter, IrValueDeclaration>,
    private val tailRecursionCalls: Set<IrCall>,
) : VariableRemapper(parameterToVariable) {

    val parameters = irFunction.parameters

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)
        if (expression !in tailRecursionCalls) {
            return expression
        }
        return builder.at(expression).genTailCall(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        if (tailrecOptimization.followFunctionReference(expression)) {
            expression.symbol.owner.body?.transformChildrenVoid(this)
        }
        return super.visitFunctionReference(expression)
    }

    override fun visitRichFunctionReference(expression: IrRichFunctionReference): IrExpression {
        return if (tailrecOptimization.followRichFunctionReference(expression)) {
            super.visitRichFunctionReference(expression)
        } else {
            expression.boundValues.transformInPlace(this, null)
            expression
        }
    }

    private fun IrBuilderWithScope.genTailCall(expression: IrCall) = this.irBlock(expression) {
        // Get all specified arguments:
        val parameterToArgument = expression.getArgumentsWithIr().associateTo(mutableMapOf()) { [parameter, argument] ->
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
            irTemporary(tailrecOptimization.nullConst(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.type))
        }
        // Now replace those variables with ones containing actual default values. Unused null-valued temporaries will hopefully
        // be optimized out later.
        val remapper = VariableRemapper(parameterToArgument)
        defaultValuedParameters.let { if (tailrecOptimization.useProperComputationOrderOfTailrecDefaultParameters) it else it.asReversed() }
            .associateWithTo(parameterToArgument) { parameter ->
                val originalDefaultValue = parameter.defaultValue?.expression ?: throw Error("no argument specified for $parameter")
                irTemporary(originalDefaultValue.deepCopyWithSymbols(parent).transform(remapper, null))
            }

        // Copy the new `val`s into the `var`s declared outside the loop:
        parameterToArgument.forEach { [parameter, argument] ->
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
