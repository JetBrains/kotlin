/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.common.lower.loops.handlers.ArrayIterationHandler
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction

@PhaseDescription(
    name = "JvmForLoopsLowering",
    description = "Jvm `for` loops lowering"
)
class JvmForLoopsLowering(context: JvmBackendContext) : ForLoopsLowering(context, JvmArrayIterationHandler(context))

class JvmArrayIterationHandler(override val context: JvmBackendContext) : ArrayIterationHandler(context) {
    override val IrType.sizePropertyGetter: IrSimpleFunction
        get() {
            val clazz = getClass()!!
            return clazz.getPropertyGetter("size")?.owner
                ?: clazz.functions.single { it.name.asString() == "getSize"}
        }

    override val IrType.getFunction: IrSimpleFunction
        get() = context.ir.symbols.arrayGet[getClass()!!.name] ?: context.ir.symbols.list.getSimpleFunction("get")!!.owner
}