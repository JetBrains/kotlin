/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.builtins.FunctionInterfacePackageFragment
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.packageFragmentDescriptor
import org.jetbrains.kotlin.ir.util.erasedTopLevelCopy
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.utils.addIfNotNull

abstract class IrModuleSerializer<Serializer : IrFileSerializer>(
    protected val settings: IrSerializationSettings,
    protected val diagnosticReporter: IrDiagnosticReporter,
) {
    abstract fun createSerializer(): Serializer

    /**
     * Allows to skip [file] during serialization.
     *
     * For example, some files should be generated anew instead of deserialization.
     */
    protected open fun backendSpecificFileFilter(file: IrFile): Boolean =
        true

    protected abstract val globalDeclarationTable: GlobalDeclarationTable

    private fun serializeIrFile(file: IrFile): SerializedIrFile {
        val fileSerializer = createSerializer()
        return fileSerializer.serializeIrFile(file)
    }

    private fun serializePreprocessedInlineFunctions(module: IrModuleFragment): SerializedIrFile {
        val functions = buildList {
            module.acceptChildrenVoid(object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                    addIfNotNull(declaration.erasedTopLevelCopy)
                    super.visitSimpleFunction(declaration)
                }
            })
        }

        val fileSerializer = createSerializer()
        return fileSerializer.serializeIrFileWithPreprocessedInlineFunctions(functions)
    }

    fun serializedIrModule(module: IrModuleFragment): SerializedIrModule {
        val serializedFiles = module.files
            .filter { it.packageFragmentDescriptor !is FunctionInterfacePackageFragment }
            .filter(this::backendSpecificFileFilter)
            .map(this::serializeIrFile)
        if (settings.shouldCheckSignaturesOnUniqueness) {
            globalDeclarationTable.clashDetector.reportErrorsTo(diagnosticReporter)
        }

        val preprocessedInlineFunctionsFile = if (settings.serializePreprocessedInlineFuns)
            serializePreprocessedInlineFunctions(module)
        else null
        return SerializedIrModule(serializedFiles, preprocessedInlineFunctionsFile)
    }
}