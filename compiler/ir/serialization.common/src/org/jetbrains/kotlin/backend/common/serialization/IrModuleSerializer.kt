/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.builtins.FunctionInterfacePackageFragment
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.packageFragmentDescriptor
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.callableId
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.originalOfPreparedInlineFunctionCopy
import org.jetbrains.kotlin.ir.util.preparedInlineFunctionCopies
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.name.CallableId

class SerializedIrFiles(
    val irData: SerializedIrFile,
    val irInlineData: SerializedIrFile?,
    val exportedSignatures: Set<IdSignature.CommonSignature>,
    val importedSignatures: Set<IdSignature.CommonSignature>,
    val callableIdsOfInlineFuns: List<CallableId>,
)

private class SerializedIrFileWithSignatures(
    val irData: SerializedIrFile,
    val declaredSignatures: Set<IdSignature.CommonSignature>,
    val referencedSignatures: Set<IdSignature.CommonSignature>,
)

abstract class IrModuleSerializer<Serializer : IrFileSerializer>(
    protected val settings: IrSerializationSettings,
    protected val diagnosticReporter: IrDiagnosticReporter,
) {
    abstract fun createFileSerializer(settings: IrSerializationSettings = this.settings): Serializer

    /**
     * Allows to skip [file] during serialization.
     *
     * For example, some files should be generated anew instead of deserialization.
     */
    protected open fun backendSpecificFileFilter(file: IrFile): Boolean =
        true

    protected abstract val globalDeclarationTable: GlobalDeclarationTable

    private fun serializeIrFile(file: IrFile): SerializedIrFileWithSignatures {
        val fileSerializer = createFileSerializer()
        val serializedFile = fileSerializer.serializeIrFile(file)

        return SerializedIrFileWithSignatures(
            serializedFile,
            fileSerializer.signaturesOfPublicTopLevelDeclarations.takeIf { settings.collectDataForSignatureIndex }.orEmpty(),
            fileSerializer.signaturesOfTopLevelReferencedDeclarations.takeIf { settings.collectDataForSignatureIndex }.orEmpty()
        )
    }

    private fun serializePreparedInlinableFunctions(file: IrFile, preparedInlineFunctionCopies: List<IrSimpleFunction>): SerializedIrFile {
        return createFileSerializer().serializeIrFileWithPreparedInlineFunctions(file, preparedInlineFunctionCopies)
    }

    private fun extractCallableIdsFromInlineFuns(irModuleFragment: IrModuleFragment?): Map<IrFile, List<CallableId>> {
        return irModuleFragment?.preparedInlineFunctionCopies
            ?.groupBy { it.file }
            ?.mapValues {
                it.value.map { func ->
                    val original = func.originalOfPreparedInlineFunctionCopy!!
                    original.correspondingPropertySymbol?.owner?.callableId ?: original.callableId
                }
            } ?: emptyMap()
    }

    fun serializedIrModule(module: IrModuleFragment): List<SerializedIrFiles> {
        val inlinableFunctionsFiles = module.preparedInlineFunctionCopies?.groupBy { it.file }?.mapValues {
            serializePreparedInlinableFunctions(it.key, it.value)
        }.orEmpty()

        val callableIdsOfInlineFunByFile = extractCallableIdsFromInlineFuns(module)

        val serializedFiles = module.files.asSequence()
            .filter { it.packageFragmentDescriptor !is FunctionInterfacePackageFragment }
            .filter(this::backendSpecificFileFilter)
            .map {
                val (irData, declaredSignatures, referencedSignatures) = serializeIrFile(it)
                SerializedIrFiles(
                    irData,
                    inlinableFunctionsFiles[it],
                    declaredSignatures,
                    referencedSignatures - declaredSignatures,
                    callableIdsOfInlineFunByFile[it] ?: emptyList(),
                )
            }
            .toList()

        if (settings.shouldCheckSignaturesOnUniqueness) {
            globalDeclarationTable.clashDetector.reportErrorsTo(diagnosticReporter)
        }

        return serializedFiles
    }
}
