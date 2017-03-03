package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isFunctionInvoke
import org.jetbrains.kotlin.backend.konan.ir.IrInlineFunctionBody
import org.jetbrains.kotlin.backend.konan.ir.getArguments
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrContainerExpressionBase
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrMemberAccessExpressionBase
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree
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
        statements: MutableList<IrStatement>): MutableList<Pair<DeclarationDescriptor, IrExpression>> {

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

        val result = super.visitCall(irCall)
        if (functionDeclaration == null) return result                                      // Function is declared in another module.
//        print("inline function ${currentFile!!.name} ")                                   // TODO debug output
//        print("function: ${currentFunction!!.descriptor.name} ")                          // TODO debug output
//        println("call: ${functionDescriptor.name} ${irCall.startOffset}")                 // TODO debug output

        val copyFuncDeclaration = functionDeclaration.accept(DeepCopyIrTree(),              // Create copy of the function.
            null) as IrFunction

        val startOffset = copyFuncDeclaration.startOffset
        val endOffset   = copyFuncDeclaration.endOffset
        val returnType  = copyFuncDeclaration.descriptor.returnType!!

        if (copyFuncDeclaration.body == null) return result                                 // TODO workaround
        val blockBody   = copyFuncDeclaration.body!! as IrBlockBody
        val statements  = blockBody.statements
        val parameters  = evaluateParameters(irCall, statements)                            // Evaluate parameters representing expression.
        val inlineBody  = IrInlineFunctionBody(startOffset, endOffset, returnType, null, statements)

        val lambdaInliner = LambdaInliner(parameters)
        inlineBody.accept(lambdaInliner, null)
        val transformer = ParametersTransformer(parameters, irCall)
        inlineBody.accept(transformer, null)                                                // Replace parameters with expression.

        return super.visitBlock(inlineBody)
    }

    //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall): IrExpression {

        val fqName = currentFile!!.packageFragmentDescriptor.fqName.asString()              // TODO to be removed after stdlib compilation
        if(fqName.contains("kotlin")) return super.visitCall(expression)                    // TODO to be removed after stdlib compilation
        // if (currentFunction!!.descriptor.isInline) return super.visitCall(expression)       // TODO workaround

        val functionDescriptor = expression.descriptor as FunctionDescriptor
        if (functionDescriptor.isInline) return inlineFunction(expression)                  // Return newly created IrInlineBody instead of IrCall.

        return super.visitCall(expression)
    }

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) = element.accept(this, null)
}

//-----------------------------------------------------------------------------//

internal class LambdaInliner(val parameterToArgument:
      MutableList<Pair<DeclarationDescriptor, IrExpression>>): IrElementTransformerVoid() {

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

    fun getLambdaFunction(lambdaArgument: IrBlock): IrFunction? {
        val statements = (lambdaArgument as IrContainerExpressionBase).statements
        val irFunction = statements[0]
        if (irFunction !is IrFunction) return null                                          // TODO
        val lambdaFunction = irFunction as IrFunction
        return lambdaFunction
    }

    //-------------------------------------------------------------------------//

    fun inlineLambda(irCall: IrCall): IrExpression {

        val dispatchReceiver  = irCall.dispatchReceiver as IrGetValue                       //
        val parameterArgument = parameterToArgument.find {                                  // Find expression to replace this parameter.
            it.first == dispatchReceiver.descriptor
        }
        if (parameterArgument == null) return super.visitCall(irCall)                       // It is not function parameter.

        val lambdaArgument = parameterArgument.second
        val lambdaFunction = getLambdaFunction(lambdaArgument as IrBlock)

        if (lambdaFunction == null) return super.visitCall(irCall)                          // TODO

        val parameters = lambdaFunction.descriptor.valueParameters                          // Lambda function parameters
        val res = parameters.map {
            val argument  = irCall.getValueArgument(it.index)
            val parameter = it as DeclarationDescriptor
            parameter to argument!!
        }.toMutableList()

        val lambdaStatements = getLambdaStatements(lambdaArgument)
        val lambdaReturnType = getLambdaReturnType(lambdaArgument)
        val inlineBody       = IrInlineFunctionBody(0, 0, lambdaReturnType, null, lambdaStatements)

        val transformer = ParametersTransformer(res, irCall)
        inlineBody.accept(transformer, null)                                                // Replace parameters with expression.

        return inlineBody                                                                   // Replace call site with InlineFunctionBody.
    }

    //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall): IrExpression {
        if (!isLambdaCall(expression)) return super.visitCall(expression)                   // If call it is not lambda call - do nothing
        return inlineLambda(expression)
    }
}

//-----------------------------------------------------------------------------//

internal class ParametersTransformer(val parameterToArgument: MutableList<Pair<DeclarationDescriptor, IrExpression>>,
                                     val callSite: IrCall): IrElementTransformerVoid() {

    override fun visitElement(element: IrElement) = element.accept(this, null)
    val scope  = Scope(callSite.descriptor as FunctionDescriptor)

    //-------------------------------------------------------------------------//

    override fun visitTypeOperator(oldExpression: IrTypeOperatorCall): IrExpression {

        val expression = super.visitTypeOperator(oldExpression) as IrTypeOperatorCall

        if (expression.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {              // Nothing to do for IMPLICIT_COERCION_TO_UNIT
            return expression
        }

        val typeArgsMap = (callSite as IrMemberAccessExpressionBase).typeArguments          // If there are no type args - do nothing.
        if (typeArgsMap == null) return expression

        val typeDescriptor = expression.typeOperand.constructor.declarationDescriptor
        if (typeDescriptor !is TypeParameterDescriptor) return expression                   // It is not TypeParameter - do nothing

        val typeOld         = typeDescriptor
        val typeNew         = typeArgsMap[typeOld]!!
        val startOffset     = expression.startOffset
        val endOffset       = expression.endOffset
        val type            = expression.type
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
        if (typeArgsMap == null) return null
        val oldType     = variable.descriptor.type
        val typeDescriptor = oldType.constructor.declarationDescriptor
        return typeArgsMap[typeDescriptor]
    }

    //-------------------------------------------------------------------------//

    override fun visitVariable(declaration: IrVariable): IrStatement {

        val result = super.visitVariable(declaration)
        val type = substituteType(declaration)
        if (type == null) {
            return result
        }

        val newVar = newVariable(declaration, type)
        val rr = parameterToArgument.find { it.first == declaration.descriptor }
        if (rr == null) {
            val getVal = IrGetValueImpl(0, 0, newVar.descriptor)                            // Create new IR element representing access the new variable.
            parameterToArgument.add(declaration.descriptor to getVal)
        } else {
            return rr.second
        }

        return newVar
    }

}

