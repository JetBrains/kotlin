package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.types.typeUtil.isUnit

internal class ReturnsInsertionLowering(val context: Context) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFunction(declaration: IrFunction) {
                declaration.acceptChildrenVoid(this)

                val body = declaration.body
                if ((declaration.descriptor is ConstructorDescriptor || declaration.descriptor.returnType!!.isUnit()) && body != null) {
                    val irBuilder = context.createIrBuilder(declaration.symbol, declaration.startOffset, declaration.endOffset)
                    irBuilder.run {
                         (body as IrBlockBody).statements += irReturn(irGetObject(this@ReturnsInsertionLowering.context.ir.symbols.unit))
                    }
                }
            }
        })
    }
}