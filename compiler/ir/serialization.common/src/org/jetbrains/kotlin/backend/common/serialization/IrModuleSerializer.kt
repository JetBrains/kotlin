/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.KlibSignatureIndexComponentWriterImpl
import org.jetbrains.kotlin.builtins.FunctionInterfacePackageFragment
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.packageFragmentDescriptor
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.preparedInlineFunctionCopies
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.utils.addToStdlib.runIf

abstract class IrModuleSerializer<Serializer : IrFileSerializer>(
    protected val settings: IrSerializationSettings,
    protected val diagnosticReporter: IrDiagnosticReporter,
) {
    private val signaturesOfPublicTopLevelDeclarations = hashSetOf<IdSignature.CommonSignature>()
    private val signaturesOfTopLevelReferencedDeclarations = hashSetOf<IdSignature.CommonSignature>()

    abstract fun createFileSerializer(settings: IrSerializationSettings = this.settings): Serializer

    /**
     * Allows to skip [file] during serialization.
     *
     * For example, some files should be generated anew instead of deserialization.
     */
    protected open fun backendSpecificFileFilter(file: IrFile): Boolean =
        true

    protected abstract val globalDeclarationTable: GlobalDeclarationTable

    private fun serializeIrFile(file: IrFile): SerializedIrFile {
        val fileSerializer = createFileSerializer()
        val serializedFile = fileSerializer.serializeIrFile(file)

        if (settings.collectDataForSignatureIndex) {
            signaturesOfPublicTopLevelDeclarations += fileSerializer.signaturesOfPublicTopLevelDeclarations
            signaturesOfTopLevelReferencedDeclarations += fileSerializer.signaturesOfTopLevelReferencedDeclarations
        }

        return serializedFile
    }

    private fun serializePreparedInlinableFunctions(file: IrFile, preparedInlineFunctionCopies: List<IrSimpleFunction>): SerializedIrFile {
        return createFileSerializer().serializeIrFileWithPreparedInlineFunctions(file, preparedInlineFunctionCopies)
    }

    fun serializedIrModule(module: IrModuleFragment): SerializedIrModule {
        val serializedFiles = module.files.asSequence()
            .filter { it.packageFragmentDescriptor !is FunctionInterfacePackageFragment }
            .filter(this::backendSpecificFileFilter)
            .map(this::serializeIrFile)
            .toList()

        if (settings.shouldCheckSignaturesOnUniqueness) {
            globalDeclarationTable.clashDetector.reportErrorsTo(diagnosticReporter)
        }

        val serializedSignatureIndex = runIf(settings.collectDataForSignatureIndex) {
            KlibSignatureIndexComponentWriterImpl(
                exportedTopLevelSignatures = signaturesOfPublicTopLevelDeclarations,
                importedTopLevelSignatures = signaturesOfTopLevelReferencedDeclarations - signaturesOfPublicTopLevelDeclarations,
            ).serializedSignatureIndex
        }

        val inlinableFunctionsFiles = module.preparedInlineFunctionCopies?.groupBy { it.file }?.map {
            serializePreparedInlinableFunctions(it.key, it.value)
        }

        return SerializedIrModule(
            signatureIndex = serializedSignatureIndex,
            files = serializedFiles,
            filesWithPreparedInlinableFunctions = inlinableFunctionsFiles ?: emptyList(),
        )
    }
}
