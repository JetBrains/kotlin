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
import org.jetbrains.kotlin.types.KotlinType

//-----------------------------------------------------------------------------//

internal class FunctionInlining(val context: Context): IrElementTransformerVoid() {

    var currentFile     : IrFile?     = null
    var currentFunction : IrFunction? = null
    var functionScope   : Scope?      = null

    //-------------------------------------------------------------------------//

    fun inline(irFile: IrFile) = irFile.accept(this, null)

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

    fun isLambdaExpression(expression: IrExpression) : Boolean {
        if (expression !is IrContainerExpressionBase)      return false
        if (expression.origin != IrStatementOrigin.LAMBDA) return false
        return true
    }

    //-------------------------------------------------------------------------//

    fun evaluateParameters(irCall: IrCall,
        statements: MutableList<IrStatement>): MutableList<Pair<ValueDescriptor, IrExpression>> {

        val parametersOld  = irCall.getArguments()                                          // Create map call_site_argument -> inline_function_parameter.
        val parametersNew  = parametersOld.map {
            val argument  = it.first.original
            val parameter = it.second
            if (parameter is IrGetValue)       return@map argument to it.second             // Parameter is already GetValue - nothing to evaluate.
            if (parameter is IrConst<*>)       return@map argument to it.second             // Parameter is constant - nothing to evaluate.
            if (isLambdaExpression(parameter)) return@map argument to it.second             // Parameter is lambda - will be inlined.

            val newVar = functionScope!!.createTemporaryVariable(parameter, "inline", false) // Create new variable and init it with the parameter expression.
            statements.add(0, newVar)                                                       // Add initialization of the new variable in statement list.

            val getVal = IrGetValueImpl(0, 0, newVar.descriptor)                            // Create new IR element representing access the new variable.
            argument to getVal                                                              // Parameter will be replaced with the new variable.
        }
        return parametersNew.toMutableList()
    }

    //-------------------------------------------------------------------------//

    fun inlineFunction(irCall: IrCall): IrExpression {

        val functionDescriptor = irCall.descriptor as FunctionDescriptor
        val functionDeclaration = context.ir.originalModuleIndex
            .functions[functionDescriptor.original]                                         // Get FunctionDeclaration by FunctionDescriptor.

        if (functionDeclaration == null) return irCall                                      // Function is declared in another module.
        print("file: ${currentFile!!.fileEntry.name} ")                                     // TODO debug output
        print("function: ${currentFunction!!.descriptor.name} ")                            // TODO debug output
        println("call: ${functionDescriptor.name} ${irCall.startOffset}")                   // TODO debug output

        val copyFuncDeclaration = functionDeclaration.accept(DeepCopyIrTree(),              // Create copy of the function.
            null) as IrFunction

        val startOffset = copyFuncDeclaration.startOffset
        val endOffset   = copyFuncDeclaration.endOffset
        val returnType  = copyFuncDeclaration.descriptor.returnType!!

        if (copyFuncDeclaration.body == null) return irCall                                 // TODO workaround
        val blockBody   = copyFuncDeclaration.body!! as IrBlockBody
        val statements  = blockBody.statements
        val inlineBody  = IrInlineFunctionBody(startOffset, endOffset, returnType, null, statements)

        val newBody = super.visitBlock(inlineBody) as IrBlock

        val parameters  = evaluateParameters(irCall, newBody.statements)                            // Evaluate parameters representing expression.
        val lambdaInliner = LambdaInliner(parameters, functionScope!!)
        newBody.accept(lambdaInliner, null)     // TODO
        val transformer = ParametersTransformer(parameters, irCall, functionScope!!)
        newBody.accept(transformer, null)       // TODO                                             // Replace parameters with expression.

        return newBody
    }

    //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall): IrExpression {

        val fqName = currentFile!!.packageFragmentDescriptor.fqName.asString()              // TODO to be removed after stdlib compilation
        //if(fqName.contains("kotlin")) return super.visitCall(expression)                    // TODO to be removed after stdlib compilation
        val fileName = currentFile!!.fileEntry.name
        if (fileName.contains("cinterop")) return super.visitCall(expression)
        if (currentFunction!!.descriptor.isInline) return super.visitCall(expression)       // TODO workaround

        val functionDescriptor = expression.descriptor as FunctionDescriptor
        if (functionDescriptor.isInline) return inlineFunction(expression)                  // Return newly created IrInlineBody instead of IrCall.

        return super.visitCall(expression)
    }

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) = element.accept(this, null)
}

//-----------------------------------------------------------------------------//

internal class LambdaInliner(val parameterToArgument:
      MutableList<Pair<ValueDescriptor, IrExpression>>, val scope: Scope): IrElementTransformerVoid() {

    override fun visitElement(element: IrElement) = element.accept(this, null)

    //-------------------------------------------------------------------------//

    fun isLambdaCall(irCall: IrCall) : Boolean {
        if (!(irCall.descriptor as FunctionDescriptor).isFunctionInvoke) return false       // If it is lambda call.
        if (irCall.dispatchReceiver !is IrGetValue)                      return false       // Do not process such dispatch receiver.
        return true
    }

    //-------------------------------------------------------------------------//

    fun getLambdaStatements(value: IrExpression) : MutableList<IrStatement> {
        val statements = (value as IrContainerExpressionBase).statements
        val lambdaFunction = statements[0] as IrFunction
        val lambdaBody = lambdaFunction.body as IrBlockBody
        return lambdaBody.statements
    }

    //-------------------------------------------------------------------------//

    fun getLambdaReturnType(value: IrExpression) : KotlinType {
        val statements = (value as IrContainerExpressionBase).statements
        val lambdaFunction = statements[0] as IrFunction
        return lambdaFunction.descriptor.returnType!!
    }

    //-------------------------------------------------------------------------//

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

    //-------------------------------------------------------------------------//

    fun inlineLambda(irCall: IrCall): IrExpression {

        val dispatchReceiver  = irCall.dispatchReceiver as IrGetValue                       //
        val parameterArgument = parameterToArgument.find {                                  // Find expression to replace this parameter.
            it.first == dispatchReceiver.descriptor
        }
        if (parameterArgument == null) return super.visitCall(irCall)                       // It is not function parameter.

        val lambdaArgument = parameterArgument.second
        val lambdaFunction = getLambdaFunction(lambdaArgument)

        if (lambdaFunction == null) return super.visitCall(irCall)                          // TODO

        val parameters = lambdaFunction.descriptor.valueParameters                          // Lambda function parameters
        val res = parameters.map {
            val argument  = irCall.getValueArgument(it.index)
            val parameter = it as ValueDescriptor
            parameter to argument!!
        }.toMutableList()

        val lambdaStatements = getLambdaStatements(lambdaArgument)
        val lambdaReturnType = getLambdaReturnType(lambdaArgument)
        val inlineBody       = IrInlineFunctionBody(0, 0, lambdaReturnType, null, lambdaStatements)

        val transformer = ParametersTransformer(res, irCall, scope)
        val aa = inlineBody.accept(transformer, null)          // TODO                                      // Replace parameters with expression.

        return inlineBody                                                                   // Replace call site with InlineFunctionBody.
    }

    //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall): IrExpression {
        if (!isLambdaCall(expression)) return super.visitCall(expression)                   // If call it is not lambda call - do nothing
        return inlineLambda(expression)
    }
}

//-----------------------------------------------------------------------------//

internal class ParametersTransformer(val parameterToArgument: MutableList<Pair<ValueDescriptor, IrExpression>>,
                                     val callSite: IrCall, val scope: Scope): IrElementTransformerVoid() {

    override fun visitElement(element: IrElement) = element.accept(this, null)

    //-------------------------------------------------------------------------//

    override fun visitTypeOperator(oldExpression: IrTypeOperatorCall): IrExpression {

        val expression = super.visitTypeOperator(oldExpression) as IrTypeOperatorCall
        if (expression.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {              // Nothing to do for IMPLICIT_COERCION_TO_UNIT
            return expression
        }

        val typeArgsMap = (callSite as IrMemberAccessExpressionBase).typeArguments          // If there are no type args - do nothing.
        if (typeArgsMap == null) return expression

        val operandTypeDescriptor = expression.typeOperand.constructor.declarationDescriptor
        if (operandTypeDescriptor !is TypeParameterDescriptor) return expression            // It is not TypeParameter - do nothing

        println("typeArgsMap: $typeArgsMap")
        println("operandTypeDescriptor: $operandTypeDescriptor")
        val typeNew         = typeArgsMap[operandTypeDescriptor]!!
        val startOffset     = expression.startOffset
        val endOffset       = expression.endOffset
        val type            = expression.type               // TODO
        val operator        = expression.operator
        val argument        = expression.argument

        return IrTypeOperatorCallImpl(startOffset, endOffset, type, operator, typeNew, argument)
    }

    //-------------------------------------------------------------------------//

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val descriptor = expression.descriptor
        val result = super.visitGetValue(expression)
        val parameterArgument = parameterToArgument.find { it.first == descriptor }         // Find expression to replace this parameter.
        if (parameterArgument == null) {
            return result
        }

        return  parameterArgument.second                                                    // TODO should we proceed with IR iteration here?
    }

    //-------------------------------------------------------------------------//

    fun newVariable(oldVariable: IrVariable, newType: KotlinType): IrVariable {

        val initializer = oldVariable.initializer!!
        val newVariable = scope.createTemporaryVariable(initializer, "inline", false)       // Create new variable and init it with the parameter expression.

        return newVariable
    }

    //-------------------------------------------------------------------------//

    fun substituteType(variable: IrVariable): KotlinType? {

        val typeArgsMap = (callSite as IrMemberAccessExpressionBase).typeArguments
        if (typeArgsMap == null) return variable.descriptor.type
        val oldType        = variable.descriptor.type
        val typeDescriptor = oldType.constructor.declarationDescriptor
        val newType        = typeArgsMap[typeDescriptor]
        if (newType == null) return variable.descriptor.type
        return newType
    }

    //-------------------------------------------------------------------------//

    override fun visitVariable(declaration: IrVariable): IrStatement {

//        println("visitVariableAkm ${declaration.descriptor.name}")

        val replacement = parameterToArgument.find {
            if (it.second !is IrGetValue) return super.visitVariable(declaration)
            val aa = it.second as IrGetValue
            aa.descriptor == declaration.descriptor
        }   // Try to find
        if (replacement != null) return super.visitVariable(declaration)

        val nn = super.visitVariable(declaration) as IrVariable
        val newType     = substituteType(nn)!!                                     // If variable type should be replaced with type arg.
        val newVariable = newVariable(nn, newType)                                 // Create new local variable.
        val getVal = IrGetValueImpl(0, 0, newVariable.descriptor)                       // Create new IR element representing access the new variable.
        parameterToArgument.add(nn.descriptor to getVal)
        return newVariable
    }

}

