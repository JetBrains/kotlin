/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.util.io.DataExternalizer
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.incremental.impl.hashToLong
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumerImpl
import org.jetbrains.kotlin.incremental.js.IrTranslationResultValue
import org.jetbrains.kotlin.incremental.js.TranslationResultValue
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.library.impl.IrArrayReader
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import java.io.DataInput
import java.io.DataOutput
import java.io.File

open class IncrementalJsCache(
    cachesDir: File,
    icContext: IncrementalCompilationContext,
    serializerProtocol: SerializerExtensionProtocol,
) : AbstractIncrementalCache<FqName>(cachesDir, icContext) {
    companion object {
        private const val TRANSLATION_RESULT_MAP = "translation-result"
        private const val IR_TRANSLATION_RESULT_MAP = "ir-translation-result"
        private const val INLINE_FUNCTIONS = "inline-functions"
        private const val INLINE_FUNCTIONS_IDS = "inline-functions-ids"
    }

    private val protoData = ProtoDataProvider(serializerProtocol)

    override val sourceToClassesMap = registerMap(SourceToFqNameMap(SOURCE_TO_CLASSES.storageFile, icContext))
    override val dirtyOutputClassesMap = registerMap(DirtyClassesFqNameMap(DIRTY_OUTPUT_CLASSES.storageFile, icContext))
    private val translationResults = registerMap(TranslationResultMap(TRANSLATION_RESULT_MAP.storageFile, protoData, icContext))
    private val irTranslationResults = registerMap(IrTranslationResultMap(IR_TRANSLATION_RESULT_MAP.storageFile, icContext))
    private val irInlineTranslationResults = registerMap(IrTranslationResultMap(INLINE_FUNCTIONS.storageFile, icContext))
    private val irInlineIdsResults = registerMap(IrInlineIdsMap(INLINE_FUNCTIONS_IDS.storageFile, icContext))

    private val dirtySources = hashSetOf<File>()

    override fun markDirty(removedAndCompiledSources: Collection<File>) {
        super.markDirty(removedAndCompiledSources)
        dirtySources.addAll(removedAndCompiledSources)
    }

    fun compare(translatedFiles: Map<File, TranslationResultValue>, changesCollector: ChangesCollector) {
        for ([srcFile, data] in translatedFiles) {
            val oldProtoMap = translationResults[srcFile]?.metadata?.let { protoData(srcFile, it) } ?: emptyMap()
            val newProtoMap = protoData(srcFile, data.metadata)

            for (classId in oldProtoMap.keys + newProtoMap.keys) {
                changesCollector.collectProtoChanges(oldProtoMap[classId], newProtoMap[classId])
            }
        }
    }

    fun compareAndUpdate(incrementalResults: IncrementalResultsConsumerImpl, changesCollector: ChangesCollector) {
        for ([srcFile, newData] in incrementalResults.packageParts) {
            dirtySources.remove(srcFile)
            val oldProtoMap = translationResults[srcFile]?.metadata?.let { protoData(srcFile, it) } ?: emptyMap()
            val newProtoMap = protoData(srcFile, newData.metadata)

            for ([classId, protoData] in newProtoMap) {
                registerOutputForFile(srcFile, classId.asSingleFqName())

                if (protoData is ClassProtoData) {
                    addToClassStorage(protoData, srcFile)
                }
            }

            for (classId in oldProtoMap.keys + newProtoMap.keys) {
                changesCollector.collectProtoChanges(oldProtoMap[classId], newProtoMap[classId])
            }

            translationResults.put(srcFile, newData.metadata)
        }

        for ([srcFile, newIrData] in incrementalResults.irFileData) {
            val (fileData, types, signatures, strings, declarations, bodies, fqn, debugInfos = debugInfo, fileEntries) = newIrData
            irTranslationResults.put(
                srcFile, fileData, types, signatures, strings, declarations, bodies, fqn, debugInfos, fileEntries
            )
        }

        for ([srcFile, newIrData] in incrementalResults.irInlineFileData) {
            compareInlineFunctions(srcFile, incrementalResults, changesCollector)

            val (fileData, types, signatures, strings, declarations, bodies, fqn, debugInfos = debugInfo, fileEntries) = newIrData
            irInlineTranslationResults.put(
                srcFile, fileData, types, signatures, strings, declarations, bodies, fqn, debugInfos, fileEntries
            )
        }

        for ([srcFile, inlineIds] in incrementalResults.irInlineIds) {
            irInlineIdsResults.put(srcFile, inlineIds)
        }
    }

    private fun compareInlineFunctions(
        srcFile: File,
        incrementalResults: IncrementalResultsConsumerImpl,
        changesCollector: ChangesCollector,
    ) {
        val oldInlineFunctions: Map<CallableId, MutableList<Long>> = buildMap {
            val oldIds = irInlineIdsResults[srcFile] ?: return@buildMap
            val oldData = irInlineTranslationResults[srcFile] ?: return@buildMap
            for (i in oldIds.indices) {
                getOrPut(oldIds[i]) { mutableListOf() }.add(oldData.hash())
            }
        }

        val newInlineFunctions: Map<CallableId, MutableList<Long>> = buildMap {
            val newIds = incrementalResults.irInlineIds[srcFile] ?: return@buildMap
            val newData = incrementalResults.irInlineFileData[srcFile] ?: return@buildMap
            for (i in newIds.indices) {
                getOrPut(newIds[i]) { mutableListOf() }.add(newData.hash())
            }
        }

        (newInlineFunctions.keys + oldInlineFunctions.keys).forEach { callableId ->
            val scope = callableId.classId?.asSingleFqName() ?: callableId.packageName
            val name = callableId.callableName.asString()
            changesCollector.collectMemberIfValueWasChanged(scope, name, oldInlineFunctions[callableId], newInlineFunctions[callableId])
        }
    }

    private fun IrTranslationResultValue.hash(): Long {
        val hashCodes = listOfNotNull(
            this.fileData.hashToLong(),
            this.types.hashToLong(),
            this.signatures.hashToLong(),
            this.strings.hashToLong(),
            this.declarations.hashToLong(),
            this.bodies.hashToLong(),
            this.fqn.hashToLong(),
            this.debugInfo?.hashToLong(),
            this.fileEntries?.hashToLong(),
        )
        return hashCodes.reduce { acc, hashCode -> acc * 31 + hashCode }
    }

    private fun registerOutputForFile(srcFile: File, name: FqName) {
        sourceToClassesMap.append(srcFile, name)
        dirtyOutputClassesMap.notDirty(name)
    }

    override fun clearCacheForRemovedClasses(changesCollector: ChangesCollector) {
        dirtySources.forEach {
            translationResults.remove(it, changesCollector)
            irTranslationResults.remove(it)
            irInlineTranslationResults.remove(it)
        }
        removeAllFromClassStorage(dirtyOutputClassesMap.getDirtyOutputClasses(), changesCollector)
        dirtySources.clear()
        dirtyOutputClassesMap.clear()
    }

    fun nonDirtyPackageParts(): Map<File, TranslationResultValue> =
        hashMapOf<File, TranslationResultValue>().apply {
            for (file in translationResults.keys) {

                if (file !in dirtySources) {
                    put(file, translationResults[file]!!)
                }
            }
        }

    fun nonDirtyIrParts(): Map<File, IrTranslationResultValue> =
        hashMapOf<File, IrTranslationResultValue>().apply {
            for (file in irTranslationResults.keys) {

                if (file !in dirtySources) {
                    put(file, irTranslationResults[file]!!)
                }
            }
        }

    fun nonDirtyIrInlineParts(): Map<File, IrTranslationResultValue> =
        hashMapOf<File, IrTranslationResultValue>().apply {
            for (file in irInlineTranslationResults.keys) {

                if (file !in dirtySources) {
                    put(file, irInlineTranslationResults[file]!!)
                }
            }
        }

    fun nonDirtyIrInlineIds(): Map<File, List<CallableId>> =
        hashMapOf<File, List<CallableId>>().apply {
            for (file in irInlineIdsResults.keys) {

                if (file !in dirtySources) {
                    put(file, irInlineIdsResults[file]!!)
                }
            }
        }
}

private object TranslationResultValueExternalizer : DataExternalizer<TranslationResultValue> {
    override fun save(output: DataOutput, value: TranslationResultValue) {
        output.writeInt(value.metadata.size)
        output.write(value.metadata)
    }

    override fun read(input: DataInput): TranslationResultValue {
        val metadataSize = input.readInt()
        val metadata = ByteArray(metadataSize)
        input.readFully(metadata)

        return TranslationResultValue(metadata = metadata)
    }
}

private class TranslationResultMap(
    storageFile: File,
    private val protoData: ProtoDataProvider,
    icContext: IncrementalCompilationContext,
) : AbstractBasicMap<File, TranslationResultValue>(
    storageFile,
    icContext.fileDescriptorForSourceFiles,
    TranslationResultValueExternalizer,
    icContext
) {

    @TestOnly
    override fun dumpValue(value: TranslationResultValue): String =
        "Metadata: ${value.metadata.md5()}"

    @Synchronized
    fun put(sourceFile: File, newMetadata: ByteArray) {
        this[sourceFile] =
            TranslationResultValue(metadata = newMetadata)
    }

    @Synchronized
    fun remove(sourceFile: File, changesCollector: ChangesCollector) {
        val protoBytes = this[sourceFile]!!.metadata
        val protoMap = protoData(sourceFile, protoBytes)

        for ([_, protoData] in protoMap) {
            changesCollector.collectProtoChanges(oldData = protoData, newData = null)
        }
        remove(sourceFile)
    }
}

private object IrTranslationResultValueExternalizer : DataExternalizer<IrTranslationResultValue> {
    override fun save(output: DataOutput, value: IrTranslationResultValue) {
        output.writeArray(value.fileData)
        output.writeArray(value.types)
        output.writeArray(value.signatures)
        output.writeArray(value.strings)
        output.writeArray(value.declarations)
        output.writeArray(value.bodies)
        output.writeArray(value.fqn)
        output.writeOptionalArray(value.debugInfo)
        output.writeOptionalArray(value.fileEntries)
    }

    private fun DataOutput.writeArray(array: ByteArray) {
        writeInt(array.size)
        write(array)
    }

    private fun DataOutput.writeOptionalArray(array: ByteArray?) {
        if (array != null)
            writeArray(array)
        else {
            writeInt(-1)
        }
    }

    private fun DataInput.readArray(): ByteArray {
        val dataSize = readInt()
        val filedata = ByteArray(dataSize)
        readFully(filedata)
        return filedata
    }

    private fun DataInput.readOptionalArray(): ByteArray? {
        val dataSize = readInt()
        return if (dataSize == -1)
            null
        else {
            val filedata = ByteArray(dataSize)
            readFully(filedata)
            filedata
        }
    }

    override fun read(input: DataInput): IrTranslationResultValue {
        val fileData = input.readArray()
        val types = input.readArray()
        val signatures = input.readArray()
        val strings = input.readArray()
        val declarations = input.readArray()
        val bodies = input.readArray()
        val fqn = input.readArray()
        val debugInfos = input.readOptionalArray()
        val fileEntries = input.readOptionalArray()

        return IrTranslationResultValue(
            fileData, types, signatures, strings, declarations, bodies, fqn, debugInfos, fileEntries
        )
    }
}

private class IrTranslationResultMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : AbstractBasicMap<File, IrTranslationResultValue>(
    storageFile,
    icContext.fileDescriptorForSourceFiles,
    IrTranslationResultValueExternalizer,
    icContext
) {

    @TestOnly
    override fun dumpValue(value: IrTranslationResultValue): String =
        "Filedata: ${value.fileData.md5()}, " +
                "Types: ${value.types.md5()}, " +
                "Signatures: ${value.signatures.md5()}, " +
                "Strings: ${value.strings.md5()}, " +
                "Declarations: ${value.declarations.md5()}, " +
                "Bodies: ${value.bodies.md5()}"

    @Synchronized
    fun put(
        sourceFile: File,
        newFiledata: ByteArray,
        newTypes: ByteArray,
        newSignatures: ByteArray,
        newStrings: ByteArray,
        newDeclarations: ByteArray,
        newBodies: ByteArray,
        fqn: ByteArray,
        debugInfos: ByteArray?,
        fileEntries: ByteArray?,
    ) {
        this[sourceFile] =
            IrTranslationResultValue(
                newFiledata, newTypes, newSignatures, newStrings, newDeclarations, newBodies, fqn, debugInfos, fileEntries
            )
    }
}

private class IrInlineIdsMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : AbstractBasicMap<File, List<CallableId>>(
    storageFile,
    icContext.fileDescriptorForSourceFiles,
    ListExternalizer(CallableIdExternalizer),
    icContext
) {

    @TestOnly
    override fun dumpValue(value: List<CallableId>): String =
        "Filedata: ${value.joinToString().toByteArray().md5()}"

    @Synchronized
    fun put(
        sourceFile: File,
        ids: List<CallableId>,
    ) {
        this[sourceFile] = ids
    }
}

