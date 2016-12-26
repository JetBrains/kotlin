package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal class FunctionInlining(val context: Context) {
    fun inline(irFile: IrFile) {
        irFile.accept(object : IrElementTransformerVoid() {
            override fun visitElement(element: IrElement) = element.accept(this, null)

            override fun visitCall(expression: IrCall): IrExpression {
                val functionDescriptor = expression.descriptor as FunctionDescriptor                //
                if (functionDescriptor.isInline == false) return super.visitCall(expression)        // function is not to be inlined - do nothing

                if (!functionDescriptor.name.asString().contains("foo")) return super.visitCall(expression)

                val functionDeclaration = context.ir.moduleIndex.functions[functionDescriptor]!!    // get FunctionDeclaration by FunctionDescriptor
                val body = functionDeclaration.body!! as IrBlockBodyImpl

                val startOffset = functionDeclaration.startOffset
                val endOffset   = functionDeclaration.endOffset
                val returnType  = functionDeclaration.descriptor.returnType!!
                val statements  = body.statements
                val irBlock = IrBlockImpl(startOffset, endOffset, returnType, null, statements)     // create
                return irBlock
            }
        }, null)

    }
}
