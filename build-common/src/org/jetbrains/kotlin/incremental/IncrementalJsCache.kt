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
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumerImpl
import org.jetbrains.kotlin.incremental.js.IrTranslationResultValue
import org.jetbrains.kotlin.incremental.js.TranslationResultValue
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
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
        private const val HEADER_FILE_NAME = "header.meta"
        private const val PACKAGE_META_FILE = "packages-meta"
        private const val SOURCE_TO_JS_OUTPUT = "source-to-js-output"

        fun hasHeaderFile(cachesDir: File) = File(cachesDir, HEADER_FILE_NAME).exists()
    }

    private val protoData = ProtoDataProvider(serializerProtocol)

    override val sourceToClassesMap = registerMap(SourceToFqNameMap(SOURCE_TO_CLASSES.storageFile, icContext))
    override val dirtyOutputClassesMap = registerMap(DirtyClassesFqNameMap(DIRTY_OUTPUT_CLASSES.storageFile, icContext))
    private val translationResults = registerMap(TranslationResultMap(TRANSLATION_RESULT_MAP.storageFile, protoData, icContext))
    private val irTranslationResults = registerMap(IrTranslationResultMap(IR_TRANSLATION_RESULT_MAP.storageFile, icContext))
    private val packageMetadata = registerMap(PackageMetadataMap(PACKAGE_META_FILE.storageFile, icContext))
    private val sourceToJsOutputsMap = registerMap(SourceToJsOutputMap(SOURCE_TO_JS_OUTPUT.storageFile, icContext))

    private val dirtySources = hashSetOf<File>()

    private val headerFile: File
        get() = File(cachesDir, HEADER_FILE_NAME)

    var header: ByteArray
        get() = headerFile.readBytes()
        set(value) {
            icContext.transaction.writeBytes(headerFile.toPath(), value)
        }

    override fun markDirty(removedAndCompiledSources: Collection<File>) {
        removedAndCompiledSources.forEach { sourceFile ->
            sourceToJsOutputsMap.remove(sourceFile)
            // The common prefix of all FQN parents has to be the file package
            sourceToClassesMap[sourceFile].orEmpty().map { it.parentOrNull()?.asString() ?: "" }.minByOrNull { it.length }?.let {
                packageMetadata.remove(it)
            }
        }
        super.markDirty(removedAndCompiledSources)
        dirtySources.addAll(removedAndCompiledSources)
    }

    fun compare(translatedFiles: Map<File, TranslationResultValue>, changesCollector: ChangesCollector) {
        for ((srcFile, data) in translatedFiles) {
            val oldProtoMap = translationResults[srcFile]?.metadata?.let { protoData(srcFile, it) } ?: emptyMap()
            val newProtoMap = protoData(srcFile, data.metadata)

            for (classId in oldProtoMap.keys + newProtoMap.keys) {
                changesCollector.collectProtoChanges(oldProtoMap[classId], newProtoMap[classId])
            }
        }
    }

    fun getOutputsBySource(sourceFile: File): Collection<File> {
        return sourceToJsOutputsMap[sourceFile].orEmpty()
    }

    fun compareAndUpdate(incrementalResults: IncrementalResultsConsumerImpl, changesCollector: ChangesCollector) {
        val translatedFiles = incrementalResults.packageParts

        for ((srcFile, data) in translatedFiles) {
            dirtySources.remove(srcFile)
            val (binaryMetadata, binaryAst, inlineData) = data

            val oldProtoMap = translationResults[srcFile]?.metadata?.let { protoData(srcFile, it) } ?: emptyMap()
            val newProtoMap = protoData(srcFile, binaryMetadata)

            for ((classId, protoData) in newProtoMap) {
                registerOutputForFile(srcFile, classId.asSingleFqName())

                if (protoData is ClassProtoData) {
                    addToClassStorage(protoData, srcFile)
                }
            }

            for (classId in oldProtoMap.keys + newProtoMap.keys) {
                changesCollector.collectProtoChanges(oldProtoMap[classId], newProtoMap[classId])
            }

            translationResults.put(srcFile, binaryMetadata, binaryAst, inlineData)
        }

        for ((packageName, metadata) in incrementalResults.packageMetadata) {
            packageMetadata[packageName] = metadata
        }

        for ((srcFile, irData) in incrementalResults.irFileData) {
            val (fileData, types, signatures, strings, declarations, bodies, fqn, fileMetadata, debugInfos) = irData
            irTranslationResults.put(srcFile, fileData, types, signatures, strings, declarations, bodies, fqn, fileMetadata, debugInfos)
        }
    }

    private fun registerOutputForFile(srcFile: File, name: FqName) {
        sourceToClassesMap.append(srcFile, name)
        dirtyOutputClassesMap.notDirty(name)
    }

    override fun clearCacheForRemovedClasses(changesCollector: ChangesCollector) {
        dirtySources.forEach {
            translationResults.remove(it, changesCollector)
            irTranslationResults.remove(it)
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

    fun packageMetadata(): Map<String, ByteArray> = hashMapOf<String, ByteArray>().apply {
        for (fqNameString in packageMetadata.keys()) {
            put(fqNameString, packageMetadata[fqNameString]!!)
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

    fun updateSourceToOutputMap(
        generatedFiles: Iterable<GeneratedFile>,
    ) {
        for (generatedFile in generatedFiles) {
            for (source in generatedFile.sourceFiles) {
                if (dirtySources.contains(source))
                    sourceToJsOutputsMap.append(source, generatedFile.outputFile)
            }
        }
    }
}

private object TranslationResultValueExternalizer : DataExternalizer<TranslationResultValue> {
    override fun save(output: DataOutput, value: TranslationResultValue) {
        output.writeInt(value.metadata.size)
        output.write(value.metadata)

        output.writeInt(value.binaryAst.size)
        output.write(value.binaryAst)

        output.writeInt(value.inlineData.size)
        output.write(value.inlineData)
    }

    override fun read(input: DataInput): TranslationResultValue {
        val metadataSize = input.readInt()
        val metadata = ByteArray(metadataSize)
        input.readFully(metadata)

        val binaryAstSize = input.readInt()
        val binaryAst = ByteArray(binaryAstSize)
        input.readFully(binaryAst)

        val inlineDataSize = input.readInt()
        val inlineData = ByteArray(inlineDataSize)
        input.readFully(inlineData)

        return TranslationResultValue(metadata = metadata, binaryAst = binaryAst, inlineData = inlineData)
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
        "Metadata: ${value.metadata.md5()}, Binary AST: ${value.binaryAst.md5()}, InlineData: ${value.inlineData.md5()}"

    @Synchronized
    fun put(sourceFile: File, newMetadata: ByteArray, newBinaryAst: ByteArray, newInlineData: ByteArray) {
        this[sourceFile] =
            TranslationResultValue(metadata = newMetadata, binaryAst = newBinaryAst, inlineData = newInlineData)
    }

    @Synchronized
    fun remove(sourceFile: File, changesCollector: ChangesCollector) {
        val protoBytes = this[sourceFile]!!.metadata
        val protoMap = protoData(sourceFile, protoBytes)

        for ((_, protoData) in protoMap) {
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
        output.writeArray(value.fileMetadata)
        value.debugInfo?.let { output.writeArray(it) }
    }

    private fun DataOutput.writeArray(array: ByteArray) {
        writeInt(array.size)
        write(array)
    }

    private fun DataInput.readArray(): ByteArray {
        val dataSize = readInt()
        val filedata = ByteArray(dataSize)
        readFully(filedata)
        return filedata
    }

    private fun DataInput.readArrayOrNull(): ByteArray? {
        try {
            val dataSize = readInt()
            val filedata = ByteArray(dataSize)
            readFully(filedata)
            return filedata
        } catch (e: Throwable) {
            return null
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
        val fileMetadata = input.readArray()
        val debugInfos = input.readArrayOrNull()

        return IrTranslationResultValue(fileData, types, signatures, strings, declarations, bodies, fqn, fileMetadata, debugInfos)
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
        newFileMetadata: ByteArray,
        debugInfos: ByteArray?,
    ) {
        this[sourceFile] =
            IrTranslationResultValue(newFiledata, newTypes, newSignatures, newStrings, newDeclarations, newBodies, fqn, newFileMetadata, debugInfos)
    }
}

private class ProtoDataProvider(private val serializerProtocol: SerializerExtensionProtocol) {
    operator fun invoke(sourceFile: File, metadata: ByteArray): Map<ClassId, ProtoData> {
        val classes = hashMapOf<ClassId, ProtoData>()
        val proto = ProtoBuf.PackageFragment.parseFrom(metadata, serializerProtocol.extensionRegistry)
        val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)

        proto.class_List.forEach {
            val classId = nameResolver.getClassId(it.fqName)
            classes[classId] = ClassProtoData(it, nameResolver)
        }

        proto.`package`.apply {
            val packageNameId = getExtensionOrNull(serializerProtocol.packageFqName)
            val packageFqName = packageNameId?.let { FqName(nameResolver.getPackageFqName(it)) } ?: FqName.ROOT
            val packagePartClassId = ClassId(packageFqName, Name.identifier(sourceFile.nameWithoutExtension.capitalizeAsciiOnly() + "Kt"))
            classes[packagePartClassId] = PackagePartProtoData(this, nameResolver, packageFqName)
        }

        return classes
    }
}

// TODO: remove this method once AbstractJsProtoComparisonTest is fixed
fun getProtoData(sourceFile: File, metadata: ByteArray): Map<ClassId, ProtoData> {
    val classes = hashMapOf<ClassId, ProtoData>()
    val proto = ProtoBuf.PackageFragment.parseFrom(metadata, KlibMetadataSerializerProtocol.extensionRegistry)
    val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)

    proto.class_List.forEach {
        val classId = nameResolver.getClassId(it.fqName)
        classes[classId] = ClassProtoData(it, nameResolver)
    }

    proto.`package`.apply {
        val packageFqName = getExtensionOrNull(KlibMetadataProtoBuf.packageFqName)?.let(nameResolver::getPackageFqName)?.let(::FqName) ?: FqName.ROOT
        val packagePartClassId = ClassId(packageFqName, Name.identifier(sourceFile.nameWithoutExtension.capitalizeAsciiOnly() + "Kt"))
        classes[packagePartClassId] = PackagePartProtoData(this, nameResolver, packageFqName)
    }

    return classes
}

private object ByteArrayExternalizer : DataExternalizer<ByteArray> {
    override fun save(output: DataOutput, value: ByteArray) {
        output.writeInt(value.size)
        output.write(value)
    }

    override fun read(input: DataInput): ByteArray {
        val size = input.readInt()
        val array = ByteArray(size)
        input.readFully(array)
        return array
    }
}


private class PackageMetadataMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : BasicStringMap<ByteArray>(storageFile, ByteArrayExternalizer, icContext) {
    fun put(packageName: String, newMetadata: ByteArray) {
        storage[packageName] = newMetadata
    }

    fun keys() = storage.keys

    override fun dumpValue(value: ByteArray): String = "Package metadata: ${value.md5()}"
}
