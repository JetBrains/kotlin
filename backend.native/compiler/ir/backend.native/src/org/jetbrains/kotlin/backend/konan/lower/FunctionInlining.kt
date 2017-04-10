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

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DeepCopyIrTreeWithDescriptors
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isFunctionInvoke
import org.jetbrains.kotlin.backend.konan.descriptors.needsInlining
import org.jetbrains.kotlin.backend.konan.ir.DeserializerDriver
import org.jetbrains.kotlin.backend.konan.ir.IrInlineFunctionBody
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrContainerExpressionBase
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrMemberAccessExpressionBase
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue

//-----------------------------------------------------------------------------//

internal class FunctionInlining(val context: Context): IrElementTransformerVoidWithContext() {

    private var copyIrElement: DeepCopyIrTreeWithDescriptors? = null
    private val deserializer = DeserializerDriver(context)

    //-------------------------------------------------------------------------//

    fun inline(irModule: IrModuleFragment) = irModule.accept(this, null)

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) = element.accept(this, null)

    //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall): IrExpression {
        val irCall = super.visitCall(expression) as IrCall
        val functionDescriptor = irCall.descriptor as FunctionDescriptor
        if (functionDescriptor.needsInlining) {
            copyIrElement = DeepCopyIrTreeWithDescriptors(currentScope!!, context)
            val functionDeclaration = getFunctionDeclaration(irCall)
            if (functionDeclaration == null) return irCall

            functionDeclaration.transformChildrenVoid(this)                                            // Process recursive inline.
            return inlineFunction(irCall, functionDeclaration)                                                   // Return newly created IrInlineBody instead of IrCall.
        }

        return irCall
    }

    //-------------------------------------------------------------------------//

    private fun inlineFunction(irCall: IrCall, functionDeclaration: IrFunction): IrExpression {

        val evaluatedParameters  = evaluateParameters(irCall, functionDeclaration)
        val parameterSubstituteMap = evaluatedParameters.parameters
        val evaluationStatements = evaluatedParameters.statements

        val typeArgumentsMap = (irCall as IrMemberAccessExpressionBase).typeArguments
        val copyFunctionDeclaration = copyIrElement!!.copy(                                  // Create copy of original function.
            functionDeclaration,
            typeArgumentsMap
        ) as IrFunction

        val statements   = (copyFunctionDeclaration.body as IrBlockBody).statements
        val returnType   = copyFunctionDeclaration.descriptor.returnType!!
        val inlineFunctionBody = IrInlineFunctionBody(
            startOffset = copyFunctionDeclaration.startOffset,
            endOffset   = copyFunctionDeclaration.endOffset,
            type        = returnType,
            descriptor  = copyFunctionDeclaration.descriptor.original,
            origin      = null,
            statements  = statements
        )

        val transformer = ParameterSubstitutor(parameterSubstituteMap)
        inlineFunctionBody.transformChildrenVoid(transformer)                                   // Replace parameters with expression.
        inlineFunctionBody.statements.addAll(0, evaluationStatements)

        return inlineFunctionBody                                                               // Replace call site with InlineFunctionBody.
    }

    //-------------------------------------------------------------------------//

    private inner class ParameterSubstitutor(val substituteMap: MutableMap <ValueDescriptor, IrExpression>): IrElementTransformerVoid() {

        override fun visitElement(element: IrElement) = element.accept(this, null)

        //---------------------------------------------------------------------//

        override fun visitGetValue(expression: IrGetValue): IrExpression {

            val newExpression = super.visitGetValue(expression) as IrGetValue
            val descriptor = newExpression.descriptor
            val argument = substituteMap[descriptor]                                        // Find expression to replace this parameter.
            if (argument == null) return newExpression                                    // If there is no such expression - do nothing

            val newArgument = copyIrElement!!.copy(argument, null)
            return newArgument as IrExpression
        }

       //---------------------------------------------------------------------//

        override fun visitCall(expression: IrCall): IrExpression {
            if (!isLambdaCall(expression)) return super.visitCall(expression)

            val dispatchReceiver = expression.dispatchReceiver as IrGetValue                    //
            val lambdaArgument = substituteMap[dispatchReceiver.descriptor]    // TODO original?             // Find expression to replace this parameter.
            if (lambdaArgument == null) return super.visitCall(expression)                                       // It is not function parameter - nothing to substitute.

            val dispatchDescriptor = dispatchReceiver.descriptor
            if (dispatchDescriptor is ValueParameterDescriptor &&
                dispatchDescriptor.isNoinline) return super.visitCall(expression)

            val functionDeclaration = getLambdaFunction(lambdaArgument)
            if (functionDeclaration == null) return return super.visitCall(expression)                                       // TODO

            val newExpression = inlineFunction(expression, functionDeclaration)
            return newExpression.transform(this, null)
        }
    }

    //--- Helpers -------------------------------------------------------------//

    private fun isLambdaCall(irCall: IrCall) : Boolean {
        if (!(irCall.descriptor as FunctionDescriptor).isFunctionInvoke) return false   // If it is lambda call.
        if (irCall.dispatchReceiver !is IrGetValue)                      return false   // Do not process such dispatch receiver.
        return true
    }

    //---------------------------------------------------------------------//

    private fun getLambdaFunction(lambdaArgument: IrExpression): IrFunction? {
        if (lambdaArgument !is IrBlock) return null

        if (lambdaArgument.origin != IrStatementOrigin.ANONYMOUS_FUNCTION &&
            lambdaArgument.origin != IrStatementOrigin.LAMBDA) {

            return null
        }

        val statements = lambdaArgument.statements
        if (statements.size != 2) return null

        val irFunction = statements[0]
        if (irFunction !is IrFunction) return null                                          // TODO

        val irCallableReference = statements[1]
        if (irCallableReference !is IrCallableReference ||
            irCallableReference.descriptor.original != irFunction.descriptor ||
            irCallableReference.getArguments().isNotEmpty()) {

            return null
        }

        return irFunction
    }

    //---------------------------------------------------------------------//

    private fun isLambdaExpression(expression: IrExpression) : Boolean {
        if (expression !is IrContainerExpressionBase)                   return false
        if (expression.origin == IrStatementOrigin.LAMBDA)              return true
        if (expression.origin == IrStatementOrigin.ANONYMOUS_FUNCTION)  return true
        return false
    }

    //---------------------------------------------------------------------//

    private fun needsEvaluation(expression: IrExpression): Boolean {
        if (expression is IrGetValue)          return false                                 // Parameter is already GetValue - nothing to evaluate.
        if (expression is IrConst<*>)          return false                                 // Parameter is constant - nothing to evaluate.
        if (isLambdaExpression(expression))    return false                                 // Parameter is lambda - will be inlined.
        return true
    }

    //-------------------------------------------------------------------------//

    private fun getFunctionDeclaration(irCall: IrCall): IrFunction? {

        val functionDescriptor = irCall.descriptor as FunctionDescriptor
        val originalDescriptor = functionDescriptor.original
        val functionDeclaration =
            (context.ir.originalModuleIndex.functions[originalDescriptor] ?:                // Function is declared in the current module.
                deserializer.deserializeInlineBody(originalDescriptor) as IrFunction?)       // Function is declared in another module.
        return functionDeclaration
    }

    //-------------------------------------------------------------------------//

    private class ParameterWithArgument(
        val parameterDescriptor: ValueDescriptor,
        val argument           : IrExpression
    )

    //-------------------------------------------------------------------------//

    private class EvaluatedParameters(
        val parameters: MutableMap<ValueDescriptor, IrExpression>,
        val statements: MutableList<IrStatement>
    )

    //---------------------------------------------------------------------//

    private fun buildParameterToArgumentMap(irCall: IrCall, declaration: IrFunction): MutableList<ParameterWithArgument> {

        val result = mutableListOf<ParameterWithArgument>()
        val descriptor = declaration.descriptor.original

        if (irCall.dispatchReceiver != null && descriptor.dispatchReceiverParameter != null)
            result += ParameterWithArgument(descriptor.dispatchReceiverParameter!!, irCall.dispatchReceiver!!)

        if (irCall.extensionReceiver != null && descriptor.extensionReceiverParameter != null)
            result += ParameterWithArgument(descriptor.extensionReceiverParameter!!, irCall.extensionReceiver!!)

        descriptor.valueParameters.forEach { parameterDescriptor ->
            val argument = irCall.getValueArgument(parameterDescriptor.index)
            when {
                argument != null -> {
                    result += ParameterWithArgument(parameterDescriptor, argument)
                }

                parameterDescriptor.hasDefaultValue() -> {
                    val defaultArgument = declaration.getDefault(parameterDescriptor)!!.expression
                    result += ParameterWithArgument(parameterDescriptor, defaultArgument)
                }

                parameterDescriptor.varargElementType != null -> {
                    val emptyArray = IrVarargImpl(irCall.startOffset, irCall.endOffset, parameterDescriptor.type, parameterDescriptor.varargElementType!!)
                    result += ParameterWithArgument(parameterDescriptor, emptyArray)
                }

                else -> throw Error("Incomplete expression: call to $descriptor has no argument at index ${parameterDescriptor.index}")
            }
        }
        return result
    }

    //-------------------------------------------------------------------------//

    private fun evaluateParameters(irCall: IrCall, functionDeclaration: IrFunction): EvaluatedParameters {

        val parametersOld = buildParameterToArgumentMap(irCall, functionDeclaration)                       // Create map call_site_argument -> inline_function_parameter.
        val parametersNew = mutableMapOf<ValueDescriptor, IrExpression> ()
        val statements = mutableListOf<IrStatement>()
        parametersOld.forEach {
            val parameterDescriptor = it.parameterDescriptor.original as ValueDescriptor
            val argument  = it.argument

            if (!needsEvaluation(argument)) {
                parametersNew[parameterDescriptor] = argument
                return@forEach
            }

            val currentScope = currentScope!!
            val varName = currentScope.scope.scopeOwner.name.toString() + "_inline"
            val newVar = currentScope.scope.createTemporaryVariable(argument, varName, false)  // Create new variable and init it with the parameter expression.
            statements.add(newVar)                                                               // Add initialization of the new variable in statement list.

            val startOffset = currentScope.irElement.startOffset
            val endOffset = currentScope.irElement.endOffset
            val getVal = IrGetValueImpl(startOffset, endOffset, newVar.descriptor)               // Create new IR element representing access the new variable.
            parametersNew[parameterDescriptor] = getVal                                                    // Parameter will be replaced with the new variable.
        }
        return EvaluatedParameters(parametersNew, statements)
    }
}



