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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.types.typeUtil.makeNullable

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
        copyWithDescriptors = DeepCopyIrTreeWithDescriptors(currentFunction!!, context)
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

    private fun createInlineFunctionBody(functionDeclaration: IrFunction): IrInlineFunctionBody? {

        val originBlockBody = functionDeclaration.body
        if (originBlockBody == null) return null                                            // TODO workaround

        val copyBlockBody = originBlockBody.accept(InlineCopyIr(), null) as IrBlockBody     // Create copy of original function body.
        val functionName = functionDeclaration.descriptor.name.toString()
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
        val inlineBody = createInlineFunctionBody(functionDeclaration)
        if (inlineBody == null) return irCall

        val parametersOld = getArguments(irCall, functionDeclaration)                       // Create map call_site_argument -> inline_function_parameter.
        val evaluatedParameters = evaluateParameters(parametersOld)
        val parameterToArgument = evaluatedParameters.parameters
        val evaluationStatements = evaluatedParameters.statements
        val lambdaInliner = LambdaInliner(parameterToArgument)
        inlineBody.transformChildrenVoid(lambdaInliner)

        val typeArgsMap = (irCall as IrMemberAccessExpressionBase).typeArguments
        val transformer = ParametersTransformer(parameterToArgument, typeArgsMap, evaluationStatements)
        inlineBody.transformChildrenVoid(transformer)                                       // Replace parameters with expression.
        inlineBody.statements.addAll(0, evaluationStatements)

        return inlineBody
    }

    //-------------------------------------------------------------------------//

    private inner class ParametersTransformer(val substituteMap: MutableMap <ValueDescriptor, IrExpression>,
                                      val typeArgsMap: Map <TypeParameterDescriptor, KotlinType>?,
                                      val statements: MutableList<IrStatement>): IrElementTransformerVoid() {

        override fun visitElement(element: IrElement) = element.accept(this, null)

        //---------------------------------------------------------------------//

        fun getTypeOperatorReturnType(operator: IrTypeOperator, type: KotlinType) : KotlinType {
            return when (operator) {
                IrTypeOperator.CAST,
                IrTypeOperator.IMPLICIT_CAST,
                IrTypeOperator.IMPLICIT_NOTNULL,
                IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
                IrTypeOperator.IMPLICIT_INTEGER_COERCION    -> type
                IrTypeOperator.SAFE_CAST                    -> type.makeNullable()
                IrTypeOperator.INSTANCEOF,
                IrTypeOperator.NOT_INSTANCEOF               -> context.builtIns.booleanType
            }
        }

        //---------------------------------------------------------------------//

        override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {

            val newExpression = super.visitTypeOperator(expression) as IrTypeOperatorCall
            if (typeArgsMap == null) return newExpression

            if (newExpression.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {          // Nothing to do for IMPLICIT_COERCION_TO_UNIT
                return newExpression
            }

            val operandTypeDescriptor = newExpression.typeOperand.constructor.declarationDescriptor
            if (operandTypeDescriptor !is TypeParameterDescriptor) return newExpression        // It is not TypeParameter - do nothing

            var typeNew = typeArgsMap[operandTypeDescriptor]
                    ?: return expression
            if (newExpression.typeOperand.isMarkedNullable)
                typeNew = typeNew.makeNullable()
            val startOffset     = newExpression.startOffset
            val endOffset       = newExpression.endOffset
            val operator        = newExpression.operator
            val argument        = newExpression.argument
            val type            = getTypeOperatorReturnType(operator, typeNew)

            return IrTypeOperatorCallImpl(startOffset, endOffset, type, operator, typeNew, argument)
        }

        //---------------------------------------------------------------------//

        override fun visitGetValue(expression: IrGetValue): IrExpression {
            val newExpression = super.visitGetValue(expression) as IrGetValue
            val descriptor = newExpression.descriptor
            val argument = substituteMap[descriptor]?.accept(InlineCopyIr(), null) as IrExpression?  // Find expression to replace this parameter.
            if (argument == null) return newExpression                                    // If there is no such expression - do nothing

            return argument
        }

        //---------------------------------------------------------------------//

        private fun newVariable(oldVariable: IrVariable): IrVariable {
            val initializer = oldVariable.initializer!!
            val isMutable   = oldVariable.descriptor.isVar
            val varName = currentScope!!.scopeOwner.name.toString() + "_inline"
            return currentScope!!.createTemporaryVariable(initializer, varName, isMutable) // Create new variable and init it with the parameter expression.
        }

        //---------------------------------------------------------------------//

        override fun visitVariable(declaration: IrVariable): IrStatement {

            val newDeclaration = super.visitVariable(declaration) as IrVariable             // Process variable initializer.
            val newVariable    = newVariable(newDeclaration)                                // Create new local variable.
            val getVal         = IrGetValueImpl(0, 0, newVariable.descriptor)               // Create new IR element representing access the new variable.
            val descriptor     = declaration.descriptor.original as ValueDescriptor
            substituteMap[descriptor] = getVal
            return newVariable
        }

        //---------------------------------------------------------------------//

        override fun visitSetVariable(expression: IrSetVariable): IrExpression {

            val result = super.visitSetVariable(expression)
            val substitute = substituteMap[expression.descriptor]                           // Get substitution for this variable.
            if (substitute == null) return result                                           // If there is no substitution - do nothing.

            val startOffset = expression.startOffset
            val endOffset   = expression.endOffset
            val descriptor  = (substitute as IrGetValue).descriptor as VariableDescriptor
            val value       = expression.value
            val origin      = expression.origin

            return IrSetVariableImpl(startOffset, endOffset, descriptor, value, origin)     // Create SetVariable expression for the new descriptor.
        }

        //---------------------------------------------------------------------//

        private fun createTypeSubstitutor(): TypeSubstitutor {

            val substitutionContext = typeArgsMap!!.entries.associate {
                (typeParameter, typeArgument) ->
                typeParameter.typeConstructor to TypeProjectionImpl(typeArgument)
            }
            return TypeSubstitutor.create(substitutionContext)
        }

        //---------------------------------------------------------------------//

        private fun substituteTypeArguments(irCall: IrCall, typeSubstitutor: TypeSubstitutor): Map <TypeParameterDescriptor, KotlinType>? {

            val oldTypeArguments = (irCall as IrMemberAccessExpressionBase).typeArguments
            if (oldTypeArguments == null) return null

            val newTypeArguments = oldTypeArguments.map {
                val typeParameterDescriptor = it.key
                val oldTypeArgument         = it.value
                val newTypeArgument         = typeSubstitutor.substitute(oldTypeArgument, Variance.INVARIANT) ?: oldTypeArgument
                typeParameterDescriptor to newTypeArgument
            }.toMap()

            return newTypeArguments
        }

        //---------------------------------------------------------------------//

        override fun visitCall(expression: IrCall): IrExpression {

            val irCall = super.visitCall(expression) as IrCall
            if (irCall !is IrCallImpl) return irCall
            if (typeArgsMap == null)   return irCall

            val typeSubstitutor = createTypeSubstitutor()
            val typeArguments   = substituteTypeArguments(irCall, typeSubstitutor)
            val returnType = typeSubstitutor.substitute(irCall.type, Variance.INVARIANT) ?: irCall.type
            val descriptor = irCall.descriptor.substitute(typeSubstitutor)!!
            val superQualifier = irCall.superQualifier?.substitute(typeSubstitutor)

            return IrCallImpl(irCall.startOffset, irCall.endOffset, returnType, descriptor,
                typeArguments, irCall.origin, superQualifier).apply {
                    irCall.descriptor.valueParameters.forEach {
                        val valueArgument = irCall.getValueArgument(it)
                        putValueArgument(it.index, valueArgument)
                    }
                    extensionReceiver = irCall.extensionReceiver
                    dispatchReceiver  = irCall.dispatchReceiver
                }
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
            copyWithDescriptors!!.copy(copyLambdaFunction, "lambda")                        // TODO merge DeepCopyIrTreeWithDescriptors with InlineCopyIr

            val lambdaStatements = (copyLambdaFunction.body as IrBlockBody).statements
            val lambdaReturnType = copyLambdaFunction.descriptor.returnType!!
            val inlineBody       = IrInlineFunctionBody(0, 0, lambdaReturnType, lambdaFunction.descriptor, null, lambdaStatements)

            val transformer = ParametersTransformer(parameterToArgument, null, lambdaStatements)
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



