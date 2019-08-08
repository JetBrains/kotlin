/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.library.IrIrSerializedIrFile
import org.jetbrains.kotlin.library.SerializedIrModule

abstract class IrModuleSerializer<F : IrFileSerializer>(protected val logger: LoggingContext) {
    abstract fun createSerializerForFile(file: IrFile): F

    private fun serializeIrFile(file: IrFile): IrIrSerializedIrFile {
        val fileSerializer = createSerializerForFile(file)
        return fileSerializer.serializeIrFile(file)
    }

    fun serializedIrModule(module: IrModuleFragment): SerializedIrModule {
        return SerializedIrModule(module.files.map { serializeIrFile(it) })
    }
}