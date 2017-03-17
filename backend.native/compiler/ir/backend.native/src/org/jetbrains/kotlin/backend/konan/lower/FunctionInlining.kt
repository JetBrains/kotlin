package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isFunctionInvoke
import org.jetbrains.kotlin.backend.konan.ir.IrInlineFunctionBody
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNullable

//-----------------------------------------------------------------------------//

internal class FunctionInlining(val context: Context): IrElementTransformerVoid() {

    var currentFile     : IrFile?     = null
    var currentFunction : IrFunction? = null
    var functionScope   : Scope?      = null

    //-------------------------------------------------------------------------//

    fun inline(irFile: IrFile) = irFile.accept(this, null)

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) = element.accept(this, null)

    //-------------------------------------------------------------------------//

    override fun visitFile(declaration: IrFile): IrFile {
        currentFile = declaration
        return super.visitFile(declaration)
    }

    //-------------------------------------------------------------------------//

    override fun visitFunction(declaration: IrFunction): IrStatement {
        currentFunction = declaration
        functionScope   = Scope(declaration.descriptor)
        return super.visitFunction(declaration)
    }

    //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall): IrExpression {

        val fqName = currentFile!!.packageFragmentDescriptor.fqName.asString()              // TODO to be removed after stdlib compilation
        if(fqName.contains("kotlin")) return super.visitCall(expression)                    // TODO to be removed after stdlib compilation

        val functionDescriptor = expression.descriptor as FunctionDescriptor
        if (functionDescriptor.isInline) {
            val inlineFunctionBody = inlineFunction(expression)
            inlineFunctionBody.transformChildrenVoid(this)
            return inlineFunctionBody                                                       // Return newly created IrInlineBody instead of IrCall.
        }

        return super.visitCall(expression)
    }

    //---------------------------------------------------------------------//

    fun isLambdaExpression(expression: IrExpression) : Boolean {
        if (expression !is IrContainerExpressionBase)      return false
        if (expression.origin != IrStatementOrigin.LAMBDA) return false
        return true
    }

    //---------------------------------------------------------------------//

    fun needsEvaluation(expression: IrExpression): Boolean {
        if (expression is IrGetValue)       return false                                // Parameter is already GetValue - nothing to evaluate.
        if (expression is IrConst<*>)       return false                                // Parameter is constant - nothing to evaluate.
        if (isLambdaExpression(expression)) return false                                // Parameter is lambda - will be inlined.
        return true
    }

    //-------------------------------------------------------------------------//

    fun getArguments(irCall: IrCall): List<Pair<ParameterDescriptor, IrExpression>> {
        val result = mutableListOf<Pair<ParameterDescriptor, IrExpression>>()
        val descriptor = irCall.descriptor.original

        irCall.dispatchReceiver?.let {
            result += (descriptor.dispatchReceiverParameter!! to it)
        }

        irCall.extensionReceiver?.let {
            result += (descriptor.extensionReceiverParameter!! to it)
        }

        descriptor.valueParameters.forEach {
            val arg = irCall.getValueArgument(it.index)
            if (arg != null) {
                result += (it to arg)
            }
        }

        return result
    }

    //-------------------------------------------------------------------------//

    fun evaluateParameters(irCall: IrCall, statements: MutableList<IrStatement>): MutableMap<ValueDescriptor, IrExpression> {

        val parametersOld = getArguments(irCall)                                          // Create map call_site_argument -> inline_function_parameter.
        val parametersNew = mutableMapOf<ValueDescriptor, IrExpression> ()

        parametersOld.forEach {
            val parameter = it.first.original
            val argument  = it.second

            if (!needsEvaluation(argument)) {
                parametersNew[parameter] = argument
                return@forEach
            }

            val newVar = functionScope!!.createTemporaryVariable(argument, "inline", false) // Create new variable and init it with the parameter expression.
            statements.add(0, newVar)                                                       // Add initialization of the new variable in statement list.

            val getVal = IrGetValueImpl(0, 0, newVar.descriptor)                            // Create new IR element representing access the new variable.
            parametersNew[parameter] = getVal                                               // Parameter will be replaced with the new variable.
        }
        return parametersNew
    }

    //-------------------------------------------------------------------------//

    fun inlineFunction(irCall: IrCall): IrExpression {

        val functionDescriptor = irCall.descriptor as FunctionDescriptor
        val functionDeclaration = context.ir.originalModuleIndex
            .functions[functionDescriptor.original]                                         // Get FunctionDeclaration by FunctionDescriptor.

        if (functionDeclaration == null) return irCall                                      // Function is declared in another module.

        val copyFuncDeclaration = functionDeclaration.accept(DeepCopyIrTree(),              // Create copy of the function.
            null) as IrFunction

        val startOffset = copyFuncDeclaration.startOffset
        val endOffset   = copyFuncDeclaration.endOffset
        val returnType  = copyFuncDeclaration.descriptor.returnType!!

        if (copyFuncDeclaration.body == null) return irCall                                 // TODO workaround
        val blockBody   = copyFuncDeclaration.body!! as IrBlockBody
        val statements  = blockBody.statements
        val inlineBody  = IrInlineFunctionBody(startOffset, endOffset, returnType, null, statements)

        val evaluationStatements = mutableListOf<IrStatement>()
        val parameterToArgument = evaluateParameters(irCall, evaluationStatements)
        val lambdaInliner = LambdaInliner(parameterToArgument)
        inlineBody.transformChildrenVoid(lambdaInliner)

        val typeArgsMap = (irCall as IrMemberAccessExpressionBase).typeArguments
        val transformer = ParametersTransformer(parameterToArgument, typeArgsMap, evaluationStatements)
        inlineBody.transformChildrenVoid(transformer)                                       // Replace parameters with expression.
        inlineBody.statements.addAll(0, evaluationStatements)

        return inlineBody
    }

    //-------------------------------------------------------------------------//

    inner class ParametersTransformer(val substituteMap: MutableMap <ValueDescriptor, IrExpression>,
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

            val typeNew         = typeArgsMap[operandTypeDescriptor]!!
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
            val argument = substituteMap[descriptor]                                      // Find expression to replace this parameter.
            if (argument == null) return newExpression                                    // If there is no such expression - do nothing

            return argument
        }

        //---------------------------------------------------------------------//

        fun newVariable(oldVariable: IrVariable): IrVariable {
            val initializer = oldVariable.initializer!!
            val isMutable   = oldVariable.descriptor.isVar
            return functionScope!!.createTemporaryVariable(initializer, "inline", isMutable)    // Create new variable and init it with the parameter expression.
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
    }

    //-------------------------------------------------------------------------//

    inner class LambdaInliner(val substituteMap: MutableMap <ValueDescriptor, IrExpression>): IrElementTransformerVoid() {

        override fun visitElement(element: IrElement) = element.accept(this, null)

        //---------------------------------------------------------------------//

        fun isLambdaCall(irCall: IrCall) : Boolean {
            if (!(irCall.descriptor as FunctionDescriptor).isFunctionInvoke) return false   // If it is lambda call.
            if (irCall.dispatchReceiver !is IrGetValue)                      return false   // Do not process such dispatch receiver.
            return true
        }

        //---------------------------------------------------------------------//

        fun getLambdaFunction(lambdaArgument: IrExpression): IrFunction? {
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

        fun buildParameterToArgument(lambdaFunction: IrFunction, irCall: IrCall): MutableMap<ValueDescriptor, IrExpression> {
            val descriptor = lambdaFunction.descriptor
            val parameterToArgument = mutableMapOf<ValueDescriptor, IrExpression>()

            irCall.extensionReceiver?.let {
                val parameter = descriptor.extensionReceiverParameter!!
                val argument  = it
                parameterToArgument[parameter] = argument
            }

            val parameters = descriptor.valueParameters                                     // Get lambda function parameters.
            parameters.forEach {                                                            // Iterate parameters.
                val parameter = it
                val argument  = irCall.getValueArgument(it.index)                           // Get corresponding argument.
                parameterToArgument[parameter] = argument!!                                 // Create (parameter -> argument) pair.
            }
            return parameterToArgument
        }

        //---------------------------------------------------------------------//

        fun inlineLambda(irCall: IrCall): IrExpression {

            val dispatchReceiver = irCall.dispatchReceiver as IrGetValue                    //
            val lambdaArgument = substituteMap[dispatchReceiver.descriptor]                 // Find expression to replace this parameter.
            if (lambdaArgument == null) return super.visitCall(irCall)                      // It is not function parameter - nothing to substitute.

            val lambdaFunction = getLambdaFunction(lambdaArgument)
            if (lambdaFunction == null) return super.visitCall(irCall)                      // TODO

            val parameterToArgument = buildParameterToArgument(lambdaFunction, irCall)

            val lambdaStatements = (lambdaFunction.body as IrBlockBody).statements
            val lambdaReturnType = lambdaFunction.descriptor.returnType!!
            val inlineBody       = IrInlineFunctionBody(0, 0, lambdaReturnType, null, lambdaStatements)

            val transformer = ParametersTransformer(parameterToArgument, null, lambdaStatements)
            inlineBody.accept(transformer, null)                                            // Replace parameters with expression.

            return inlineBody                                                               // Replace call site with InlineFunctionBody.
        }

        //---------------------------------------------------------------------//

        override fun visitCall(expression: IrCall): IrExpression {
            if (!isLambdaCall(expression)) return super.visitCall(expression)               // If call it is not lambda call - do nothing.
            return inlineLambda(expression)
        }
    }
}



