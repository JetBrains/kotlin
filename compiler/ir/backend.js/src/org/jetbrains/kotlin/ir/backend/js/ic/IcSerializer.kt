/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.ir.backend.js.JsMapping
import org.jetbrains.kotlin.ir.backend.js.SerializedMappings
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsGlobalDeclarationTable
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrFileSerializer
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.serialization.SerializedCarriers
import org.jetbrains.kotlin.ir.serialization.serializeCarriers
import org.jetbrains.kotlin.ir.types.impl.IrErrorTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.types.Variance

class IcSerializer(
    irBuiltIns: IrBuiltIns,
    val mappings: JsMapping,
    val irFactory: PersistentIrFactory,
    val linker: JsIrLinker,
    val module: IrModuleFragment
) {

    private val signaturer = IdSignatureSerializer(JsManglerIr)
    private val globalDeclarationTable = JsGlobalDeclarationTable(signaturer, irBuiltIns)

    fun serializeDeclarations(declarations: Iterable<IrDeclaration>): SerializedIcData {

        // TODO serialize body carriers and new bodies as well
        val moduleDeserializer = linker.moduleDeserializer(module.descriptor)

        val fileToDeserializer = moduleDeserializer.fileDeserializers().associateBy { it.file }

        val filteredDeclarations = declarations.filter {
            when {
                it.fileOrNull.let { it == null || fileToDeserializer[it] == null } -> false
                it is IrFakeOverrideFunction -> it.isBound
                it is IrFakeOverrideProperty -> it.isBound
                else -> true
            }
        }

        val dataToSerialize = filteredDeclarations.groupBy {
            // TODO don't move declarations or effects outside the original file
            // TODO Or invent a different mechanism for that

            it.file
        }.entries

        val fileToIndex = mutableMapOf<IrFile, Int>()
        dataToSerialize.forEach { (f, _) ->
            fileToIndex[f] = fileToIndex.size
        }

        val icData = dataToSerialize.map { (file, declarations) ->
            val fileDeserializer = fileToDeserializer[file]!!

            val maxFileLocalIndex = fileDeserializer.reversedSignatureIndex.keys.filterIsInstance<IdSignature.FileLocalSignature>().maxOfOrNull { it.id } ?: -1
            val maxScopeLocalIndex = fileDeserializer.reversedSignatureIndex.keys.filterIsInstance<IdSignature.ScopeLocalDeclaration>().maxOfOrNull { it.id } ?: -1

            val icDeclarationTable = IcDeclarationTable(globalDeclarationTable, irFactory, maxFileLocalIndex + 1, maxScopeLocalIndex + 1)
            val fileSerializer = JsIrFileSerializer(IrMessageLogger.None, icDeclarationTable, mutableMapOf(), skipExpects = true, icMode = true) { fileToIndex[it]!! }

            // Serialize old bodies as they have probably changed.
            // Need to keep the order same as before.
            val bodies = (linker.bodyToIndex[file] ?: emptyMap())

            // TODO add local bodies

//            val sortedBodyEntries = bodies.entries.sortedBy { it.value }


            val indexToBody = mutableMapOf<Int, IrBody>()
            bodies.entries.forEach { (k, v) -> indexToBody[v] = k }

            val maxIndex = indexToBody.keys.maxOrNull() ?: -1
            for (i in 0..maxIndex) {
                val body = indexToBody[i] ?: run {
                    val errorType = IrErrorTypeImpl(null, emptyList(), Variance.INVARIANT)
                    irFactory.createBlockBody(
                        -1, -1, listOf(IrErrorExpressionImpl(-1, -1, errorType, "Statement body is not deserialized yet"))
                    )
                }
                if (body is IrExpressionBody) {
                    fileSerializer.serializeIrExpressionBody(body.expression)
                } else {
                    fileSerializer.serializeIrStatementBody(body)
                }
            }

            if ("CharCode.kt" in file.name) {
                1
            }

            // Only save newly created declarations
            val newDeclarations = declarations.filter { d ->
                d is PersistentIrDeclarationBase<*> && d.createdOn > 0
            }

            val serializedCarriers = fileSerializer.serializeCarriers(
                declarations,
                bodies.keys,
            )

            val serializedMappings = mappings.state.serializeMappings(declarations) { symbol ->
                fileSerializer.serializeIrSymbol(symbol)
            }

            val serializedIrFile = fileSerializer.serializeDeclarationsForIC(file, newDeclarations)

            SerializedIcDataForFile(
                serializedIrFile,
                serializedCarriers,
                serializedMappings,
            )
        }
        return SerializedIcData(icData)
    }

    // Returns precomputed signatures for the newly created declarations. Delegates to the default table otherwise.
    class IcDeclarationTable(
        globalDeclarationTable: JsGlobalDeclarationTable,
        val irFactory: PersistentIrFactory,
        newLocalIndex: Long,
        newScopeIndex: Int,
    ) : DeclarationTable(globalDeclarationTable) {

        init {
            signaturer.reset(newLocalIndex, newScopeIndex)
        }

        override fun isExportedDeclaration(declaration: IrDeclaration): Boolean {
//            if (declaration is PersistentIrDeclarationBase<*>) return true
            return super.isExportedDeclaration(declaration)
        }

        override fun tryComputeBackendSpecificSignature(declaration: IrDeclaration): IdSignature? {
            return super.tryComputeBackendSpecificSignature(declaration)
        }

        override fun signatureByDeclaration(declaration: IrDeclaration): IdSignature {
            return irFactory.declarationSignature(declaration) ?: super.signatureByDeclaration(declaration)
        }
    }
}

class SerializedIcDataForFile(
    val file: SerializedIrFile,
    val carriers: SerializedCarriers,
    val mappings: SerializedMappings,
)

class SerializedIcData(
    val files: Collection<SerializedIcDataForFile>,
)