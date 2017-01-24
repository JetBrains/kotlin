package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.IrInlineFunctionBody
import org.jetbrains.kotlin.backend.konan.ir.getArguments
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

//-----------------------------------------------------------------------------//

internal class FunctionInlining(val context: Context): IrElementTransformerVoid() {

    fun inline(irFile: IrFile) = irFile.accept(this, null)

    //-------------------------------------------------------------------------//

    fun evaluateExpressionParameters(callExpression: IrCall,
        statements: MutableList<IrStatement>): List<Pair<ParameterDescriptor, IrExpression>> {

        val scope         = Scope(callExpression.descriptor as FunctionDescriptor)
        val parametersOld = callExpression.getArguments()                                   // Create map inline_function_parameter -> containing_function_expression.
        val parametersNew = parametersOld.map {
            val parameter = it.first
            val value     = it.second
            if (value is IrGetValue) return@map it                                          // If value is not an expression - do nothing.

            val newVar = scope.createTemporaryVariable(value, "inline", false)              // Create new variable and init it with the expression.
            statements.add(0, newVar)                                                       // Add initialization of the new variable in statement list.

            val getVal = IrGetValueImpl(0, 0, newVar.descriptor)                            // Create new IR element representing access the new variable
            parameter to getVal                                                             // Parameter will be replaced with the new variable.
        }
        return parametersNew
    }


    //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall): IrExpression {
        val functionDescriptor = expression.descriptor as FunctionDescriptor
        if (!functionDescriptor.isInline) return super.visitCall(expression)                // Function is not to be inlined - do nothing.

        val functionDeclaration = context.ir.moduleIndex.functions[functionDescriptor]      // Get FunctionDeclaration by FunctionDescriptor.
        if (functionDeclaration == null) return super.visitCall(expression)                 // Function is declared in another module.
        val copyFuncDeclaration = functionDeclaration.accept(DeepCopyIrTree(), null) as IrFunction // Create copy of the function.

        val body        = copyFuncDeclaration.body!! as IrBlockBody
        val startOffset = copyFuncDeclaration.startOffset
        val endOffset   = copyFuncDeclaration.endOffset
        val returnType  = copyFuncDeclaration.descriptor.returnType!!
        val statements  = body.statements

        val parameters  = evaluateExpressionParameters(expression, statements)              // Evaluate parameters representing expression.
        val transformer = ParametersTransformer(parameters)
        val irBlock     = IrInlineFunctionBody(startOffset, endOffset, returnType, null, statements)
        irBlock.accept(transformer, null)                                                   // Replace parameters with expression.

        return irBlock                                                                      // Return newly created IrBlock instead of IrCall.
    }

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) = element.accept(this, null)
}

//-----------------------------------------------------------------------------//

internal class ParametersTransformer(val parameterToExpression: List <Pair<ParameterDescriptor, IrExpression>>): IrElementTransformerVoid() {

    override fun visitElement(element: IrElement) = element.accept(this, null)

    //-------------------------------------------------------------------------//

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val descriptor = expression.descriptor
        if (descriptor !is ParameterDescriptor) {                                           // TODO do we need this check?
            return super.visitGetValue(expression)
        }
        val parExp = parameterToExpression.find { it.first == descriptor }                  // Find expression to replace this parameter.
        return parExp?.let { parExp.second } ?: super.visitGetValue(expression)             // TODO should we proceed with IR iteration here?
    }
}
