/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.builtins.FunctionInterfacePackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.SerializedIrModule

abstract class IrModuleSerializer<F : IrFileSerializer>(protected val messageLogger: IrMessageLogger, protected val compatibilityMode: CompatibilityMode) {
    abstract fun createSerializerForFile(file: IrFile): F

    /**
     * Allows to skip [file] during serialization.
     *
     * For example, some files should be generated anew instead of deserialization.
     */
    protected open fun backendSpecificFileFilter(file: IrFile): Boolean =
        true

    private fun serializeIrFile(file: IrFile): SerializedIrFile {
        val fileSerializer = createSerializerForFile(file)
        return fileSerializer.serializeIrFile(file)
    }

    fun serializedIrModule(module: IrModuleFragment): SerializedIrModule {
        val serializedFiles = module.files
            .filter { it.packageFragmentDescriptor !is FunctionInterfacePackageFragment }
            .filter(this::backendSpecificFileFilter)
            .map(this::serializeIrFile)
        return SerializedIrModule(serializedFiles)
    }
}