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
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isFunctionInvoke
import org.jetbrains.kotlin.backend.konan.ir.DeserializerDriver
import org.jetbrains.kotlin.backend.konan.ir.IrInlineFunctionBody
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrContainerExpressionBase
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrMemberAccessExpressionBase
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.types.KotlinType

//-----------------------------------------------------------------------------//

internal class FunctionInlining(val context: Context): IrElementTransformerVoid() {

    private var currentFile        : IrFile?                        = null
    private var currentFunction    : IrFunction?                    = null
    private var currentScope       : Scope?                         = null
    private var copyWithDescriptors: DeepCopyIrTreeWithDescriptors? = null

    private val deserializer = DeserializerDriver(context)

    //-------------------------------------------------------------------------//

    fun inline(irModule: IrModuleFragment) = irModule.accept(this, null)

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) = element.accept(this, null)

    //-------------------------------------------------------------------------//

    override fun visitFile(declaration: IrFile): IrFile {
        currentFile = declaration
        return super.visitFile(declaration)
    }

    //-------------------------------------------------------------------------//

    override fun visitProperty(declaration: IrProperty): IrStatement {
        currentScope = Scope(declaration.descriptor)
        return super.visitProperty(declaration)
    }

    //-------------------------------------------------------------------------//

    override fun visitFunction(declaration: IrFunction): IrStatement {
        currentFunction     = declaration
        currentScope        = Scope(declaration.descriptor)
        return super.visitFunction(declaration)
    }

    //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall): IrExpression {

        val irCall = super.visitCall(expression) as IrCall

        val functionDescriptor = irCall.descriptor as FunctionDescriptor
        if (functionDescriptor.isInline)  return inlineFunction(irCall)                     // Return newly created IrInlineBody instead of IrCall.

        return irCall
    }

    //---------------------------------------------------------------------//

    private fun isLambdaExpression(expression: IrExpression) : Boolean {
        if (expression !is IrContainerExpressionBase)      return false
        if (expression.origin != IrStatementOrigin.LAMBDA) return false
        return true
    }

    //---------------------------------------------------------------------//

    private fun needsEvaluation(expression: IrExpression): Boolean {
        if (expression is IrGetValue)          return false                                 // Parameter is already GetValue - nothing to evaluate.
        if (expression is IrConst<*>)          return false                                 // Parameter is constant - nothing to evaluate.
        if (isLambdaExpression(expression))    return false                                 // Parameter is lambda - will be inlined.
        return true
    }

    //-------------------------------------------------------------------------//

    private class ArgumentWithValue(val descriptor: ValueDescriptor, val value: IrExpression)

    private fun getArguments(irCall: IrCall, declaration: IrFunction): MutableList<ArgumentWithValue> {
        val result = mutableListOf<ArgumentWithValue>()
        val descriptor = irCall.descriptor.original

        irCall.dispatchReceiver?.let {
            result += ArgumentWithValue(descriptor.dispatchReceiverParameter!!, it)
        }

        irCall.extensionReceiver?.let {
            result += ArgumentWithValue(descriptor.extensionReceiverParameter!!, it)
        }

        descriptor.valueParameters.forEach { parameter ->
            val argument = irCall.getValueArgument(parameter.index)
            when {
                argument != null -> result += ArgumentWithValue(parameter, argument)
                parameter.hasDefaultValue() -> {
                    val defaultArgument = declaration.getDefault(parameter)!!.expression
                    result += ArgumentWithValue(parameter, defaultArgument)
                }
                parameter.varargElementType != null -> {
                    val emptyArray = IrVarargImpl(irCall.startOffset, irCall.endOffset, parameter.type, parameter.varargElementType!!)
                    result += ArgumentWithValue(parameter, emptyArray)
                }
                else -> throw Error("Incomplete expression: call to $descriptor has no argument at index ${parameter.index}")
            }
        }

        return result
    }

    //-------------------------------------------------------------------------//

    private class EvaluatedParameters(val parameters: MutableMap<ValueDescriptor, IrExpression>, val statements: MutableList<IrStatement>)

    private fun evaluateParameters(parametersOld: MutableList<ArgumentWithValue>): EvaluatedParameters {

        val parametersNew = mutableMapOf<ValueDescriptor, IrExpression> ()
        val statements = mutableListOf<IrStatement>()
        parametersOld.forEach {
            val parameter = it.descriptor.original as ValueDescriptor
            val argument  = it.value

            if (!needsEvaluation(argument)) {
                parametersNew[parameter] = argument
                return@forEach
            }

            val varName = currentScope!!.scopeOwner.name.toString() + "_inline"
            val newVar = currentScope!!.createTemporaryVariable(argument, varName, false)  // Create new variable and init it with the parameter expression.
            statements.add(newVar)                                                       // Add initialization of the new variable in statement list.

            val getVal = IrGetValueImpl(0, 0, newVar.descriptor)                            // Create new IR element representing access the new variable.
            parametersNew[parameter] = getVal                                               // Parameter will be replaced with the new variable.
        }
        return EvaluatedParameters(parametersNew, statements)
    }

    //-------------------------------------------------------------------------//

    private fun getFunctionDeclaration(irCall: IrCall): IrDeclaration? {

        val functionDescriptor = irCall.descriptor as FunctionDescriptor
        val originalDescriptor = functionDescriptor.original
        val functionDeclaration =
            (context.ir.originalModuleIndex.functions[originalDescriptor] ?:                // Function is declared in the current module.
                deserializer.deserializeInlineBody(originalDescriptor))                     // Function is declared in another module.
        return functionDeclaration
    }

    //-------------------------------------------------------------------------//

    private fun createInlineFunctionBody(functionDeclaration: IrFunction, typeArgsMap: Map <TypeParameterDescriptor, KotlinType>?): IrInlineFunctionBody? {
        functionDeclaration.transformChildrenVoid(this)     // TODO recursive inline is already processed in visitCall. Check

        val originBlockBody = functionDeclaration.body
        if (originBlockBody == null) return null                                            // TODO workaround

        copyWithDescriptors = DeepCopyIrTreeWithDescriptors(currentFunction!!, typeArgsMap, context)
        val functionName = functionDeclaration.descriptor.name.toString()

        val copyBlockBody = originBlockBody.accept(InlineCopyIr(), null) as IrBlockBody     // Create copy of original function body.
        copyWithDescriptors!!.copy(copyBlockBody, functionName)                             // TODO merge DeepCopyIrTreeWithDescriptors with InlineCopyIr

        val originalDescriptor = functionDeclaration.descriptor.original
        val startOffset = functionDeclaration.startOffset
        val endOffset   = functionDeclaration.endOffset
        val returnType  = functionDeclaration.descriptor.returnType!!

        return IrInlineFunctionBody(startOffset, endOffset, returnType, originalDescriptor, null, copyBlockBody.statements)
    }

    //-------------------------------------------------------------------------//

    private fun inlineFunction(irCall: IrCall): IrExpression {

        val irDeclaration = getFunctionDeclaration(irCall)
        if (irDeclaration == null) return irCall

        val functionDeclaration = irDeclaration as IrFunction
        val typeArgsMap = (irCall as IrMemberAccessExpressionBase).typeArguments
        val inlineBody = createInlineFunctionBody(functionDeclaration, typeArgsMap)
        if (inlineBody == null) return irCall

        val parametersOld = getArguments(irCall, functionDeclaration)                       // Create map call_site_argument -> inline_function_parameter.
        val evaluatedParameters = evaluateParameters(parametersOld)
        val parameterToArgument = evaluatedParameters.parameters
        val evaluationStatements = evaluatedParameters.statements
        val lambdaInliner = LambdaInliner(parameterToArgument)
        inlineBody.transformChildrenVoid(lambdaInliner)

        val transformer = ParametersTransformer(parameterToArgument)
        inlineBody.transformChildrenVoid(transformer)                                       // Replace parameters with expression.
        inlineBody.statements.addAll(0, evaluationStatements)

        return inlineBody
    }

    //-------------------------------------------------------------------------//

    private inner class ParametersTransformer(val substituteMap: MutableMap <ValueDescriptor, IrExpression>): IrElementTransformerVoid() {

        override fun visitElement(element: IrElement) = element.accept(this, null)

        //---------------------------------------------------------------------//

        override fun visitGetValue(expression: IrGetValue): IrExpression {

            val newExpression = super.visitGetValue(expression) as IrGetValue
            val descriptor = newExpression.descriptor
            val argument = substituteMap[descriptor]?.accept(InlineCopyIr(), null) as IrExpression?  // Find expression to replace this parameter.
            if (argument == null) return newExpression                                    // If there is no such expression - do nothing

            return argument
        }
    }

    //-------------------------------------------------------------------------//

    private inner class LambdaInliner(val substituteMap: MutableMap <ValueDescriptor, IrExpression>): IrElementTransformerVoid() {

        override fun visitElement(element: IrElement) = element.accept(this, null)

        //---------------------------------------------------------------------//

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

            // TODO: the following checks must be asserts, however it is not sane until the bugs are fixed.

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

        private fun buildParameterToArgument(lambdaFunction: IrFunction, irCall: IrCall): MutableList<ArgumentWithValue> {
            val descriptor = lambdaFunction.descriptor
            val parameterToArgument = mutableListOf<ArgumentWithValue>()

            irCall.extensionReceiver?.let {
                parameterToArgument += ArgumentWithValue(descriptor.extensionReceiverParameter!!, it)
            }

            val parameters = descriptor.valueParameters                                     // Get lambda function parameters.
            parameters.forEach {                                                            // Iterate parameters.
                val argument  = irCall.getValueArgument(it.index)                           // Get corresponding argument.
                parameterToArgument += ArgumentWithValue(it, argument!!)                    // Create (parameter -> argument) pair.
            }
            return parameterToArgument
        }

        //---------------------------------------------------------------------//

        private fun inlineLambda(irCall: IrCall): IrExpression {

            val dispatchReceiver = irCall.dispatchReceiver as IrGetValue                    //
            val lambdaArgument = substituteMap[dispatchReceiver.descriptor]                 // Find expression to replace this parameter.
            if (lambdaArgument == null) return super.visitCall(irCall)                      // It is not function parameter - nothing to substitute.

            val dispatchDescriptor = dispatchReceiver.descriptor
            if (dispatchDescriptor is ValueParameterDescriptor &&
                dispatchDescriptor.isNoinline) return super.visitCall(irCall)

            val lambdaFunction = getLambdaFunction(lambdaArgument)
            if (lambdaFunction == null) return super.visitCall(irCall)                      // TODO

            val parametersOld = buildParameterToArgument(lambdaFunction, irCall)
            val evaluatedParameters = evaluateParameters(parametersOld)
            val parameterToArgument = evaluatedParameters.parameters
            val evaluationStatements = evaluatedParameters.statements

            val copyLambdaFunction = lambdaFunction.accept(InlineCopyIr(),                  // Create copy of the function.
                null) as IrFunction
            copyWithDescriptors!!.copy(copyLambdaFunction, "lambda")                  // TODO merge DeepCopyIrTreeWithDescriptors with InlineCopyIr

            val lambdaStatements = (copyLambdaFunction.body as IrBlockBody).statements
            val lambdaReturnType = copyLambdaFunction.descriptor.returnType!!
            val inlineBody       = IrInlineFunctionBody(0, 0, lambdaReturnType, lambdaFunction.descriptor, null, lambdaStatements)

            val transformer = ParametersTransformer(parameterToArgument)
            inlineBody.accept(transformer, null)                                            // Replace parameters with expression.
            inlineBody.statements.addAll(0, evaluationStatements)

            return inlineBody                                                               // Replace call site with InlineFunctionBody.
        }

        //---------------------------------------------------------------------//

        override fun visitCall(expression: IrCall): IrExpression {
            val newExpression = super.visitCall(expression)
            if (newExpression !is IrCall) return newExpression

            if (!isLambdaCall(newExpression)) return newExpression                          // If call it is not lambda call - do nothing.
            return inlineLambda(newExpression)
        }
    }
}

//-----------------------------------------------------------------------------//

class InlineCopyIr() : DeepCopyIrTree() {

    override fun visitBlock(expression: IrBlock): IrBlock {
        return if (expression is IrInlineFunctionBody) {
            IrInlineFunctionBody(
                expression.startOffset, expression.endOffset,
                expression.type,
                expression.descriptor,
                mapStatementOrigin(expression.origin),
                expression.statements.map { it.transform(this, null) }
            )
        } else {
            super.visitBlock(expression)
        }
    }
}



