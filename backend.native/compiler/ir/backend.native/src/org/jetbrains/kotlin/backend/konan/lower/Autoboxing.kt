package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.llvm.ir2string
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody

internal class Autoboxing(val context: KonanBackendContext) : FunctionLoweringPass {

    var boxedCount = 0

    /*
    override fun lower(irBody: IrBody) {
        println("Boxer body " +  ir2string(irBody))
    } */

    override fun lower(irFunction: IrFunction) {
        println("Boxer function " +  ir2string(irFunction))

    }
}
