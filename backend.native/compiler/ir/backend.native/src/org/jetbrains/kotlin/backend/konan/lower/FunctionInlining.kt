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

@file:Suppress("FoldInitializerAndIfToElvis")

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DeepCopyIrTreeWithDescriptors
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.reportWarning
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isFunctionInvoke
import org.jetbrains.kotlin.backend.konan.descriptors.needsInlining
import org.jetbrains.kotlin.backend.konan.descriptors.resolveFakeOverride
import org.jetbrains.kotlin.backend.konan.ir.DeserializerDriver
import org.jetbrains.kotlin.backend.konan.ir.IrReturnableBlockImpl
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.getDefault
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrMemberAccessExpressionBase
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor

//-----------------------------------------------------------------------------//

internal class FunctionInlining(val context: Context): IrElementTransformerVoidWithContext() {

    private val deserializer = DeserializerDriver(context)

    //-------------------------------------------------------------------------//

    fun inline(irModule: IrModuleFragment) = irModule.accept(this, null)

    //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall): IrExpression {

        val irCall = super.visitCall(expression) as IrCall
        val functionDescriptor = irCall.descriptor as FunctionDescriptor
        if (!functionDescriptor.needsInlining) return irCall                                // This call does not need inlining.

        val functionDeclaration = getFunctionDeclaration(irCall)                            // Get declaration of the function to be inlined.
        if (functionDeclaration == null) {                                                  // We failed to get the declaration.
            val message = "Inliner failed to obtain function declaration: " +
                          functionDescriptor.fqNameSafe.toString()
            context.reportWarning(message, currentFile, irCall)                             // Report warning.
            return irCall
        }

        functionDeclaration.transformChildrenVoid(this)                                     // Process recursive inline.
        val inliner = Inliner(currentScope!!, context)                                      // Create inliner for this scope.
        return inliner.inline(irCall, functionDeclaration)                                  // Return newly created IrInlineBody instead of IrCall.
    }

    //-------------------------------------------------------------------------//

    private fun getFunctionDeclaration(irCall: IrCall): IrFunction? {

        val functionDescriptor = irCall.descriptor as FunctionDescriptor
        val originalDescriptor = functionDescriptor.resolveFakeOverride().original
        val functionDeclaration =
            context.ir.originalModuleIndex.functions[originalDescriptor] ?:                 // If function is declared in the current module.
                deserializer.deserializeInlineBody(originalDescriptor)                      // Function is declared in another module.
        return functionDeclaration as IrFunction?
    }

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) = element.accept(this, null)
}

//-----------------------------------------------------------------------------//

private class Inliner(val currentScope: ScopeWithIr, val context: Context) {

    val copyIrElement = DeepCopyIrTreeWithDescriptors(currentScope.scope.scopeOwner, context) // Create DeepCopy for current scope.
    val substituteMap = mutableMapOf<ValueDescriptor, IrExpression>()

    //-------------------------------------------------------------------------//

    fun inline(irCall             : IrCall,                                                 // Call to be substituted.
               functionDeclaration: IrFunction): IrReturnableBlockImpl {                    // Function to substitute.

        val inlineFunctionBody = inlineFunction(irCall, functionDeclaration)
        val descriptorSubstitutor = copyIrElement.descriptorSubstitutorForExternalScope
        currentScope.irElement.transformChildrenVoid(descriptorSubstitutor)                 // Transform calls to object that might be returned from inline function call.
        return inlineFunctionBody
    }

    //-------------------------------------------------------------------------//

    private fun inlineFunction(irCall             : IrCall,                                 // Call to be substituted.
                               functionDeclaration: IrFunction): IrReturnableBlockImpl {    // Function to substitute.

        val evaluationStatements = evaluateArguments(irCall, functionDeclaration)           // And list of evaluation statements.

        val copyFunctionDeclaration = copyIrElement.copy(                                   // Create copy of original function.
            irElement       = functionDeclaration,                                          // Descriptors declared inside the function will be copied.
            typeSubstitutor = createTypeSubstitutor(irCall)                                 // Type parameters will be substituted with type arguments.
        ) as IrFunction

        val statements = (copyFunctionDeclaration.body as IrBlockBody).statements           // IR statements from function copy.
        val returnType = copyFunctionDeclaration.descriptor.returnType!!                    // Substituted return type.
        val inlineFunctionBody = IrReturnableBlockImpl(                                     // Create new IR element to replace "call".
            startOffset = copyFunctionDeclaration.startOffset,
            endOffset   = copyFunctionDeclaration.endOffset,
            type        = returnType,
            descriptor  = copyFunctionDeclaration.descriptor.original,
            origin      = null,
            statements  = statements
        )

        val transformer = ParameterSubstitutor()
        inlineFunctionBody.transformChildrenVoid(transformer)                               // Replace value parameters with arguments.
        inlineFunctionBody.statements.addAll(0, evaluationStatements)                       // Insert evaluation statements.
        return inlineFunctionBody                                                           // Replace call site with InlineFunctionBody.
    }

    //---------------------------------------------------------------------//

    private inner class ParameterSubstitutor: IrElementTransformerVoid() {

        override fun visitGetValue(expression: IrGetValue): IrExpression {
            val newExpression = super.visitGetValue(expression) as IrGetValue
            val descriptor = newExpression.descriptor
            val argument = substituteMap[descriptor]                                        // Find expression to replace this parameter.
            if (argument == null) return newExpression                                      // If there is no such expression - do nothing.

            return copyIrElement.copy(                                                      // Make copy of argument expression.
                irElement       = argument,
                typeSubstitutor = null
            ) as IrExpression
        }

        //-----------------------------------------------------------------//

        override fun visitCall(expression: IrCall): IrExpression {

            if (!isLambdaCall(expression)) return super.visitCall(expression)               // If it is not lambda call - return.

            val dispatchReceiver = expression.dispatchReceiver as IrGetValue                // Here we can have only GetValue as dispatch receiver.
            val functionArgument = substituteMap[dispatchReceiver.descriptor]               // Try to find lambda representation.   // TODO original?
            if (functionArgument == null)     return super.visitCall(expression)            // It is not call of argument lambda - nothing to substitute.
            if (functionArgument !is IrBlock) return super.visitCall(expression)

            val dispatchDescriptor = dispatchReceiver.descriptor                            // Check if this functional parameter has "noInline" tag
            if (dispatchDescriptor is ValueParameterDescriptor &&
                dispatchDescriptor.isNoinline) return super.visitCall(expression)

            val functionDeclaration = getLambdaFunction(functionArgument)
            val newExpression = inlineFunction(expression, functionDeclaration)             // Inline the lambda. Lambda parameters will be substituted with lambda arguments.
            return newExpression.transform(this, null)                                      // Substitute lambda arguments with target function arguments.
        }

        //-----------------------------------------------------------------//

        override fun visitElement(element: IrElement) = element.accept(this, null)
    }

    //--- Helpers -------------------------------------------------------------//

    private fun isLambdaCall(irCall: IrCall) : Boolean {
        if (!(irCall.descriptor as FunctionDescriptor).isFunctionInvoke) return false       // Lambda mast be called by "invoke".
        if (irCall.dispatchReceiver !is IrGetValue)                      return false       // Dispatch receiver mast be IrGetValue.
        return true                                                                         // It is lambda call.
    }

    //-------------------------------------------------------------------------//

    private fun isLambdaExpression(expression: IrExpression) : Boolean {
        if (expression !is IrBlock)                                     return false        // Lambda mast be represented with IrBlock.
        if (expression.origin != IrStatementOrigin.LAMBDA &&                                // Origin mast be LAMBDA or ANONYMOUS.
            expression.origin != IrStatementOrigin.ANONYMOUS_FUNCTION)  return false

        val statements          = expression.statements
        val irFunction          = statements[0]
        val irCallableReference = statements[1]
        if (irFunction !is IrFunction)                   return false                       // First statement of the block must be lambda declaration.
        if (irCallableReference !is IrCallableReference) return false                       // Second statement of the block must be CallableReference.
        return true                                                                         // The expression represents lambda.
    }

    //-------------------------------------------------------------------------//

    private fun getLambdaFunction(lambdaArgument: IrBlock): IrFunction {
        val statements = lambdaArgument.statements
        return statements[0] as IrFunction
    }

    //-------------------------------------------------------------------------//

    private fun argumentNeedsEvaluation(expression: IrExpression): Boolean {
        if (expression is IrGetValue)          return false                                 // Parameter is already GetValue - nothing to evaluate.
        if (expression is IrConst<*>)          return false                                 // Parameter is constant - nothing to evaluate.
        if (expression is IrCallableReference) return false                                 // Parameter is callable reference - nothing to evaluate.
        if (isLambdaExpression(expression))    return false                                 // Parameter is lambda - will be inlined.
        return true
    }

    //-------------------------------------------------------------------------//

    private fun createTypeSubstitutor(irCall: IrCall): TypeSubstitutor? {

        val typeArgumentsMap = (irCall as IrMemberAccessExpressionBase).typeArguments
        if (typeArgumentsMap == null) return null
        val substitutionContext = typeArgumentsMap.entries.associate {
            (typeParameter, typeArgument) ->
            typeParameter.typeConstructor to TypeProjectionImpl(typeArgument)
        }
        return TypeSubstitutor.create(substitutionContext)
    }

    //-------------------------------------------------------------------------//

    private class ParameterToArgument(
        val parameterDescriptor: ValueDescriptor,
        val argumentExpression : IrExpression
    )

    //-------------------------------------------------------------------------//

    private fun buildParameterToArgument(irCall     : IrCall,                               // Call site.
                                         irFunction: IrFunction                             // Function to be called.
    ): MutableList<ParameterToArgument> {

        val parameterToArgument = mutableListOf<ParameterToArgument>()                      // Result list.
        val functionDescriptor = irFunction.descriptor.original                             // Descriptor of function to be called.

        if (irCall.dispatchReceiver != null &&                                              // Only if there are non null dispatch receivers both
            functionDescriptor.dispatchReceiverParameter != null)                           // on call site and in function declaration.
            parameterToArgument += ParameterToArgument(
                parameterDescriptor = functionDescriptor.dispatchReceiverParameter!!,
                argumentExpression  = irCall.dispatchReceiver!!
            )

        val valueArguments =
                irCall.descriptor.valueParameters.map { irCall.getValueArgument(it) }.toMutableList()

        if (functionDescriptor.extensionReceiverParameter != null) {
            parameterToArgument += ParameterToArgument(
                    parameterDescriptor = functionDescriptor.extensionReceiverParameter!!,
                    argumentExpression = if (irCall.extensionReceiver != null) {
                        irCall.extensionReceiver!!
                    } else {
                        // Special case: lambda with receiver is called as usual lambda:
                        valueArguments.removeAt(0)!!
                    }
            )
        } else if (irCall.extensionReceiver != null) {
            // Special case: usual lambda is called as lambda with receiver:
            valueArguments.add(0, irCall.extensionReceiver!!)
        }

        functionDescriptor.valueParameters.forEach { parameterDescriptor ->                 // Iterate value parameter descriptors.
            val argument = valueArguments[parameterDescriptor.index]                        // Get appropriate argument from call site.
            when {
                argument != null -> {                                                       // Argument is good enough.
                    parameterToArgument += ParameterToArgument(                             // Associate current parameter with the argument.
                        parameterDescriptor = parameterDescriptor,
                        argumentExpression  = argument
                    )
                }

                parameterDescriptor.hasDefaultValue() -> {                                  // There is no argument - try default value.
                    val defaultArgument = irFunction.getDefault(parameterDescriptor)!!
                    parameterToArgument += ParameterToArgument(
                        parameterDescriptor = parameterDescriptor,
                        argumentExpression  = defaultArgument.expression
                    )
                }

                parameterDescriptor.varargElementType != null -> {
                    val emptyArray = IrVarargImpl(
                        startOffset       = irCall.startOffset,
                        endOffset         = irCall.endOffset,
                        type              = parameterDescriptor.type,
                        varargElementType = parameterDescriptor.varargElementType!!
                    )
                    parameterToArgument += ParameterToArgument(
                        parameterDescriptor = parameterDescriptor,
                        argumentExpression  = emptyArray
                    )
                }

                else -> {
                    val message = "Incomplete expression: call to $functionDescriptor " +
                        "has no argument at index ${parameterDescriptor.index}"
                    throw Error(message)
                }
            }
        }
        return parameterToArgument
    }

    //-------------------------------------------------------------------------//

    private fun evaluateArguments(irCall             : IrCall,                              // Call site.
                                  functionDeclaration: IrFunction                           // Function to be called.
        ): List<IrStatement> {

        val parameterToArgumentOld = buildParameterToArgument(irCall, functionDeclaration)  // Create map parameter_descriptor -> original_argument_expression.
        val evaluationStatements   = mutableListOf<IrStatement>()                           // List of evaluation statements.
        parameterToArgumentOld.forEach {
            val parameterDescriptor = it.parameterDescriptor
            val argumentExpression  = it.argumentExpression

            if (!argumentNeedsEvaluation(argumentExpression)) {                             // If argument does not need evaluation
                substituteMap[parameterDescriptor] = argumentExpression                     // Copy parameterDescriptor -> argumentExpression to new map.
                return@forEach
            }

            val newExpression = copyIrElement.copy(argumentExpression, null) as IrExpression
            val newVariable = currentScope.scope.createTemporaryVariable(                   // Create new variable and init it with the parameter expression.
                irExpression = newExpression,
                nameHint     = functionDeclaration.descriptor.name.toString(),
                isMutable    = false)

            evaluationStatements.add(newVariable)                                           // Add initialization of the new variable in statement list.
            val getVal = IrGetValueImpl(                                                    // Create new expression, representing access the new variable.
                startOffset = currentScope.irElement.startOffset,
                endOffset   = currentScope.irElement.endOffset,
                descriptor  = newVariable.descriptor
            )
            substituteMap[parameterDescriptor] = getVal                                     // Parameter will be replaced with the new variable.
        }
        return evaluationStatements
    }
}






