package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.getArguments
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

//-----------------------------------------------------------------------------//

internal class FunctionInlining(val context: Context): IrElementTransformerVoid() {

    fun inline(irFile: IrFile) = irFile.accept(this, null)

    //-------------------------------------------------------------------------//

    fun removeReturn(statements: MutableList<IrStatement>) =
        statements.map { statement ->
            (statement as? IrReturn)?.value ?: statement
        } as MutableList<IrStatement>

    //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall): IrExpression {
        val functionDescriptor = expression.descriptor as FunctionDescriptor                //
        if (!functionDescriptor.isInline) return super.visitCall(expression)                // function is not to be inlined - do nothing

        val functionDeclaration = context.ir.moduleIndex.functions[functionDescriptor]      // get FunctionDeclaration by FunctionDescriptor
        if (functionDeclaration == null) return super.visitCall(expression)                 // TODO what if we do not have declaration?
        val copyFuncDeclaration = functionDeclaration.accept(DeepCopyIrTree(), null) as IrFunction

        val body        = copyFuncDeclaration.body!! as IrBlockBody
        val statements  = removeReturn(body.statements)
        val startOffset = copyFuncDeclaration.startOffset
        val endOffset   = copyFuncDeclaration.endOffset
        val returnType  = copyFuncDeclaration.descriptor.returnType!!
        val irBlock     = IrBlockImpl(startOffset, endOffset, returnType, null, statements) // create

        val parameterToExpression = expression.getArguments()
        val parametersTransformer = ParametersTransformer(parameterToExpression)
        irBlock.accept(parametersTransformer, null)

        return irBlock
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
        if (descriptor !is ParameterDescriptor) {
            return super.visitGetValue(expression)
        }
        val parExp = parameterToExpression.find { it.first == descriptor }
        return parExp?.let { parExp.second } ?: super.visitGetValue(expression)
    }
}