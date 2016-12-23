package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.ModuleIndex
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class FunctionInlining(val context: Context) {
    fun inline(irFile: IrFile) {
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                val functionDescriptor = expression.descriptor as FunctionDescriptor
                if (functionDescriptor.isInline == false) {
                    super.visitCall(expression)
                } else {
                    val functionDeclaration = context.ir.moduleIndex.functions[functionDescriptor]
                    println("inline_akm")
                }
            }
        })

    }
}
