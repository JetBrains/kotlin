package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isFunctionInvoke
import org.jetbrains.kotlin.backend.konan.ir.DeserializerDriver
import org.jetbrains.kotlin.backend.konan.ir.IrInlineFunctionBody
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.makeNullable

//-----------------------------------------------------------------------------//

internal class FunctionInlining(val context: Context): IrElementTransformerVoid() {

    var currentFile     : IrFile?     = null
    var currentFunction : IrFunction? = null
    var currentScope    : Scope?      = null

    //-------------------------------------------------------------------------//
    val deserializer = DeserializerDriver(context)

    fun inline(irFile: IrFile) = irFile.accept(this, null)

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
        currentFunction = declaration
        currentScope    = Scope(declaration.descriptor)
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
        if (expression is IrGetValue)          return false                                 // Parameter is already GetValue - nothing to evaluate.
        if (expression is IrConst<*>)          return false                                 // Parameter is constant - nothing to evaluate.
        if (expression is IrCallableReference) return false                                 // Parameter is nothing to evaluate.
        if (expression is IrBlock)             return false                                 // Parameter is nothing to evaluate.
        if (isLambdaExpression(expression))    return false                                 // Parameter is lambda - will be inlined.
        return true
    }

    //-------------------------------------------------------------------------//

    fun getArguments(irCall: IrCall, declaration: IrFunction): MutableMap<ValueDescriptor, IrExpression> {
        val result = mutableMapOf<ValueDescriptor, IrExpression>()
        val descriptor = irCall.descriptor.original

        irCall.dispatchReceiver?.let {
            result += (descriptor.dispatchReceiverParameter!! to it)
        }

        irCall.extensionReceiver?.let {
            result += (descriptor.extensionReceiverParameter!! to it)
        }

        descriptor.valueParameters.forEach { parameter ->
            val argument = irCall.getValueArgument(parameter.index)
            if (argument != null) {
                result += (parameter to argument)
            } else {
                val defaultArgument = declaration.getDefault(parameter)!!.expression
                result += (parameter to defaultArgument)
            }
        }

        return result
    }

    //-------------------------------------------------------------------------//

    fun evaluateParameters(parametersOld: MutableMap<ValueDescriptor, IrExpression>,
                           statements: MutableList<IrStatement>): MutableMap<ValueDescriptor, IrExpression> {

        val parametersNew = mutableMapOf<ValueDescriptor, IrExpression> ()
        parametersOld.forEach {
            val parameter = it.key.original as ValueDescriptor
            val argument  = it.value

            if (!needsEvaluation(argument)) {
                parametersNew[parameter] = argument
                return@forEach
            }

            val varName = currentScope!!.scopeOwner.name.toString() + "_inline"
            val newVar = currentScope!!.createTemporaryVariable(argument, varName, false)  // Create new variable and init it with the parameter expression.
            statements.add(0, newVar)                                                       // Add initialization of the new variable in statement list.

            val getVal = IrGetValueImpl(0, 0, newVar.descriptor)                            // Create new IR element representing access the new variable.
            parametersNew[parameter] = getVal                                               // Parameter will be replaced with the new variable.
        }
        return parametersNew
    }

    //-------------------------------------------------------------------------//

    fun inlineFunction(irCall: IrCall): IrExpression {
        val functionDescriptor = irCall.descriptor as FunctionDescriptor
        val originalDescriptor = functionDescriptor.original
        val functionDeclaration = 
            context.ir.originalModuleIndex.functions[originalDescriptor] ?:                 // Function is declared in the current module.
            deserializer.deserializeInlineBody(originalDescriptor)                          // Function is declared in another module.
        if (functionDeclaration == null) return super.visitCall(irCall)   
        val copyFuncDeclaration = functionDeclaration.accept(InlineCopyIr(),                // Create copy of the function.
            null) as IrFunction

        val startOffset = copyFuncDeclaration.startOffset
        val endOffset   = copyFuncDeclaration.endOffset
        val returnType  = copyFuncDeclaration.descriptor.returnType!!

        if (copyFuncDeclaration.body == null) return irCall                                 // TODO workaround
        val blockBody   = copyFuncDeclaration.body!! as IrBlockBody
        val statements  = blockBody.statements
        val inlineBody  = IrInlineFunctionBody(startOffset, endOffset, returnType, null, statements)

        val evaluationStatements = mutableListOf<IrStatement>()
        val parametersOld = getArguments(irCall, copyFuncDeclaration)                                          // Create map call_site_argument -> inline_function_parameter.
        val parameterToArgument = evaluateParameters(parametersOld, evaluationStatements)
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

        override fun visitCall(expression: IrCall): IrExpression {

            val irCall = super.visitCall(expression) as IrCall
            if (irCall !is IrCallImpl) return irCall

            val oldTypeArguments = (irCall as IrMemberAccessExpressionBase).typeArguments
            if (typeArgsMap      == null) return irCall
            if (oldTypeArguments == null) return irCall

            val substitutionContext = typeArgsMap.entries.associate {
                    (typeParameter, typeArgument) ->
                    typeParameter.typeConstructor to TypeProjectionImpl(typeArgument)
                }
            val typeSubstitutor = TypeSubstitutor.create(substitutionContext)

            val newTypeArguments = oldTypeArguments.map {
                val typeParameterDescriptor = it.key
                val oldTypeArgument         = it.value
                val newTypeArgument         = typeSubstitutor.substitute(oldTypeArgument, Variance.INVARIANT) ?: oldTypeArgument
                typeParameterDescriptor to newTypeArgument
            }.toMap()

            val descriptor      = irCall.descriptor
            val type            = typeSubstitutor.substitute(irCall.type, Variance.INVARIANT) ?: irCall.type
            val startOffset     = irCall.startOffset
            val endOffset       = irCall.endOffset
            val origin          = irCall.origin
            val superQualifier  = irCall.superQualifier

            return IrCallImpl(startOffset, endOffset, type, descriptor, newTypeArguments, origin, superQualifier)
                .apply {
                    descriptor.valueParameters.forEach {
                        val valueArgument = irCall.getValueArgument(it)
                        putValueArgument(it.index, valueArgument)
                    }
                    extensionReceiver = irCall.extensionReceiver
                    dispatchReceiver  = irCall.dispatchReceiver
                }
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

            val dispatchDescriptor = dispatchReceiver.descriptor
            if (dispatchDescriptor is ValueParameterDescriptor &&
                dispatchDescriptor.isNoinline) return super.visitCall(irCall)

            val lambdaFunction = getLambdaFunction(lambdaArgument)
            if (lambdaFunction == null) return super.visitCall(irCall)                      // TODO

            val parametersOld = buildParameterToArgument(lambdaFunction, irCall)
            val evaluationStatements = mutableListOf<IrStatement>()
            val parameterToArgument = evaluateParameters(parametersOld, evaluationStatements)

            val copyLambdaFunction = lambdaFunction.accept(InlineCopyIr(),                  // Create copy of the function.
                null) as IrFunction
            val lambdaStatements = (copyLambdaFunction.body as IrBlockBody).statements
            val lambdaReturnType = copyLambdaFunction.descriptor.returnType!!
            val inlineBody       = IrInlineFunctionBody(0, 0, lambdaReturnType, null, lambdaStatements)

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
        return if(expression is IrInlineFunctionBody) {
            IrInlineFunctionBody(
                expression.startOffset, expression.endOffset,
                expression.type,
                mapStatementOrigin(expression.origin),
                expression.statements.map { it.transform(this, null) }
            )
        } else {
            super.visitBlock(expression)
        }
    }
}



