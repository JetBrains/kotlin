/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.builtins.FunctionInterfacePackageFragment
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.SerializedIrModule

abstract class IrModuleSerializer<Serializer : IrFileSerializer>(
    protected val diagnosticReporter: IrDiagnosticReporter,
    protected val compatibilityMode: CompatibilityMode,
    protected val normalizeAbsolutePaths: Boolean,
    protected val sourceBaseDirs: Collection<String>,
    private val shouldCheckSignaturesOnUniqueness: Boolean,
) {
    abstract fun createSerializerForFile(file: IrFile): Serializer

    /**
     * Allows to skip [file] during serialization.
     *
     * For example, some files should be generated anew instead of deserialization.
     */
    protected open fun backendSpecificFileFilter(file: IrFile): Boolean =
        true

    protected abstract val globalDeclarationTable: GlobalDeclarationTable

    private fun serializeIrFile(file: IrFile): SerializedIrFile {
        val fileSerializer = createSerializerForFile(file)
        return fileSerializer.serializeIrFile(file)
    }

    fun serializedIrModule(module: IrModuleFragment): SerializedIrModule {
        val serializedFiles = module.files
            .filter { it.packageFragmentDescriptor !is FunctionInterfacePackageFragment }
            .filter(this::backendSpecificFileFilter)
            .map(this::serializeIrFile)
        if (shouldCheckSignaturesOnUniqueness) {
            globalDeclarationTable.clashDetector.reportErrorsTo(diagnosticReporter)
        }
        return SerializedIrModule(serializedFiles)
    }
}