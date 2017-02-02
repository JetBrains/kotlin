package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isFunctionInvoke
import org.jetbrains.kotlin.backend.konan.ir.IrInlineFunctionBody
import org.jetbrains.kotlin.backend.konan.ir.getArguments
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrContainerExpressionBase
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.types.KotlinType

//-----------------------------------------------------------------------------//

internal class FunctionInlining(val context: Context): IrElementTransformerVoid() {

    fun inline(irFile: IrFile) = irFile.accept(this, null)

    //-------------------------------------------------------------------------//

    fun isLambda(value: IrExpression) : Boolean {
        if (value !is IrContainerExpressionBase)      return false
        if (value.origin != IrStatementOrigin.LAMBDA) return false
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

    fun evaluateParameters(irCall: IrCall,
        statements: MutableList<IrStatement>): List<Pair<ParameterDescriptor, IrExpression>> {

        val scope          = Scope(irCall.descriptor as FunctionDescriptor)
        val parametersOld  = irCall.getArguments()                                          // Create map inline_function_parameter -> containing_function_expression.
        val parametersNew  = parametersOld.map {
            val parameter  = it.first
            val expression = it.second
            if (expression is IrGetValue) return@map it                                     // There is nothing to evaluate.
            if (isLambda(expression)) {                                                     // The expression is lambda.
                val inlineFunctionBody = inlineLambda(expression)                           // Create IrInlineFunctionBody to replace this parameter.
                return@map parameter to inlineFunctionBody
            }

            val newVar = scope.createTemporaryVariable(expression, "inline", false)         // Create new variable and init it with the expression.
            statements.add(0, newVar)                                                       // Add initialization of the new variable in statement list.

            val getVal = IrGetValueImpl(0, 0, newVar.descriptor)                            // Create new IR element representing access the new variable.
            parameter to getVal                                                             // Parameter will be replaced with the new variable.
        }
        return parametersNew
    }

    //-------------------------------------------------------------------------//

    fun inlineFunction(irCall: IrCall): IrBlock {

        val functionDescriptor = irCall.descriptor as FunctionDescriptor
        val functionDeclaration = context.ir.originalModuleIndex
                .functions[functionDescriptor]                                              // Get FunctionDeclaration by FunctionDescriptor.
        val copyFuncDeclaration = functionDeclaration!!.accept(DeepCopyIrTree(), null) as IrFunction // Create copy of the function.

        val startOffset = copyFuncDeclaration.startOffset
        val endOffset   = copyFuncDeclaration.endOffset
        val returnType  = copyFuncDeclaration.descriptor.returnType!!
        val inlineBody  = copyFuncDeclaration.body!! as IrBlockBody
        val statements  = inlineBody.statements
        val parameters  = evaluateParameters(irCall, statements)                            // Evaluate parameters representing expression.
        val irBlock     = IrInlineFunctionBody(startOffset, endOffset, returnType, null, statements)

        val transformer = ParametersTransformer(parameters)
        irBlock.accept(transformer, null)                                                   // Replace parameters with expression.
        return irBlock
    }

    //-------------------------------------------------------------------------//

    fun inlineLambda(value: IrExpression): IrBlock {

        val lambdaStatements = getLambdaStatements(value)
        val lambdaReturnType = getLambdaReturnType(value)
        val irBlock     = IrInlineFunctionBody(0, 0, lambdaReturnType, null, lambdaStatements)

        return irBlock
    }

    //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall): IrExpression {
        val functionDescriptor = expression.descriptor as FunctionDescriptor
        if (!functionDescriptor.isInline) return super.visitCall(expression)                   // Function is not to be inlined - do nothing.

        val functionDeclaration = context.ir.originalModuleIndex.functions[functionDescriptor] // Get FunctionDeclaration by FunctionDescriptor.
        if (functionDeclaration == null) return super.visitCall(expression)                    // Function is declared in another module.
        return inlineFunction(expression)                                                      // Return newly created IrBlock instead of IrCall.
    }

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) = element.accept(this, null)
}

//-----------------------------------------------------------------------------//

internal class ParametersTransformer(val parameterToExpression: List <Pair<ParameterDescriptor, IrExpression>>): IrElementTransformerVoid() {

    override fun visitElement(element: IrElement) = element.accept(this, null)

    //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall): IrExpression {
        if ((expression.descriptor as FunctionDescriptor).isFunctionInvoke) {               // If it is lambda call.
            if (expression.dispatchReceiver !is IrGetValue) super.visitCall(expression)     // Do not process such dispatch receiver.
            val getValue = expression.dispatchReceiver as IrGetValue                        //
            val parExpr = parameterToExpression.find { it.first == getValue.descriptor }    // Find expression to replace this parameter.
            if (parExpr == null) super.visitCall(expression)                                // It is not function parameter.
            return parExpr!!.second                                                         // Replace call site with InlineFunctionBody.
        }
        return super.visitCall(expression)                                                  // Function is declared in another module.
    }

    //-------------------------------------------------------------------------//

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val descriptor = expression.descriptor
        if (descriptor !is ParameterDescriptor) {                                           // TODO do we need this check?
            return super.visitGetValue(expression)
        }
        val parExp = parameterToExpression.find { it.first == descriptor }                  // Find expression to replace this parameter.
        if (parExp == null) return super.visitGetValue(expression)

        return  parExp.second                                                               // TODO should we proceed with IR iteration here?
    }
}
