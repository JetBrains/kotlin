/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.serialization.JvmIrSerializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.IrMessageLogger

fun serializeIrFile(context: JvmBackendContext, irFile: IrFile): ByteArray {
    return makeSerializer(context).serializeJvmIrFile(irFile).toByteArray()
}

fun serializeTopLevelIrClass(context: JvmBackendContext, irClass: IrClass): ByteArray {
    assert(irClass.parent is IrFile)
    return makeSerializer(context).serializeTopLevelClass(irClass).toByteArray()
}

private fun makeSerializer(context: JvmBackendContext) =
    JvmIrSerializer(
        context.configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None,
        context.declarationTable,
        mutableMapOf(),
    )