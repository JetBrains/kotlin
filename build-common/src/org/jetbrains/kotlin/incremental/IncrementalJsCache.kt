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
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumerImpl
import org.jetbrains.kotlin.incremental.js.IrTranslationResultValue
import org.jetbrains.kotlin.incremental.js.TranslationResultValue
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.js.JsProtoBuf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import java.io.DataInput
import java.io.DataOutput
import java.io.File

open class IncrementalJsCache(
    cachesDir: File,
    pathConverter: FileToPathConverter,
    serializerProtocol: SerializerExtensionProtocol
) : AbstractIncrementalCache<FqName>(cachesDir, pathConverter) {
    companion object {
        private const val TRANSLATION_RESULT_MAP = "translation-result"
        private const val IR_TRANSLATION_RESULT_MAP = "ir-translation-result"
        private const val INLINE_FUNCTIONS = "inline-functions"
        private const val HEADER_FILE_NAME = "header.meta"
        private const val PACKAGE_META_FILE = "packages-meta"

        fun hasHeaderFile(cachesDir: File) = File(cachesDir, HEADER_FILE_NAME).exists()
    }

    private val protoData = ProtoDataProvider(serializerProtocol)

    override val sourceToClassesMap = registerMap(SourceToFqNameMap(SOURCE_TO_CLASSES.storageFile, pathConverter))
    override val dirtyOutputClassesMap = registerMap(DirtyClassesFqNameMap(DIRTY_OUTPUT_CLASSES.storageFile))
    private val translationResults = registerMap(TranslationResultMap(TRANSLATION_RESULT_MAP.storageFile, pathConverter, protoData))
    private val irTranslationResults = registerMap(IrTranslationResultMap(IR_TRANSLATION_RESULT_MAP.storageFile, pathConverter))
    private val inlineFunctions = registerMap(InlineFunctionsMap(INLINE_FUNCTIONS.storageFile, pathConverter))
    private val packageMetadata = registerMap(PackageMetadataMap(PACKAGE_META_FILE.storageFile))

    private val dirtySources = hashSetOf<File>()

    private val headerFile: File
        get() = File(cachesDir, HEADER_FILE_NAME)

    var header: ByteArray
        get() = headerFile.readBytes()
        set(value) {
            cachesDir.mkdirs()
            headerFile.writeBytes(value)
        }

    override fun markDirty(removedAndCompiledSources: Collection<File>) {
        removedAndCompiledSources.forEach { sourceFile ->
            // The common prefix of all FQN parents has to be the file package
            sourceToClassesMap[sourceFile].map { it.parentOrNull()?.asString() ?: "" }.minBy { it.length }?.let {
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
                    addToClassStorage(protoData.proto, protoData.nameResolver, srcFile)
                }
            }

            for (classId in oldProtoMap.keys + newProtoMap.keys) {
                changesCollector.collectProtoChanges(oldProtoMap[classId], newProtoMap[classId])
            }

            translationResults.put(srcFile, binaryMetadata, binaryAst, inlineData)
        }

        for ((srcFile, inlineDeclarations) in incrementalResults.inlineFunctions) {
            inlineFunctions.process(srcFile, inlineDeclarations, changesCollector)
        }

        for ((packageName, metadata) in incrementalResults.packageMetadata) {
            packageMetadata.put(packageName, metadata)
        }

        for ((srcFile, irData) in incrementalResults.irFileData) {
            val (fileData, symbols, types, strings, declarations, bodies, fqn) = irData
            irTranslationResults.put(srcFile, fileData, symbols, types, strings, declarations, bodies, fqn)
        }
    }

    private fun registerOutputForFile(srcFile: File, name: FqName) {
        sourceToClassesMap.add(srcFile, name)
        dirtyOutputClassesMap.notDirty(name)
    }

    override fun clearCacheForRemovedClasses(changesCollector: ChangesCollector) {
        dirtySources.forEach {
            translationResults.remove(it, changesCollector)
            irTranslationResults.remove(it)
            inlineFunctions.remove(it)
        }
        removeAllFromClassStorage(dirtyOutputClassesMap.getDirtyOutputClasses(), changesCollector)
        dirtySources.clear()
        dirtyOutputClassesMap.clean()
    }

    fun nonDirtyPackageParts(): Map<File, TranslationResultValue> =
        hashMapOf<File, TranslationResultValue>().apply {
            for (file in translationResults.keys()) {

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
            for (file in irTranslationResults.keys()) {

                if (file !in dirtySources) {
                    put(file, irTranslationResults[file]!!)
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
    private val pathConverter: FileToPathConverter,
    private val protoData: ProtoDataProvider
) :
    BasicStringMap<TranslationResultValue>(storageFile, TranslationResultValueExternalizer) {
    override fun dumpValue(value: TranslationResultValue): String =
        "Metadata: ${value.metadata.md5()}, Binary AST: ${value.binaryAst.md5()}, InlineData: ${value.inlineData.md5()}"

    fun put(sourceFile: File, newMetadata: ByteArray, newBinaryAst: ByteArray, newInlineData: ByteArray) {
        storage[pathConverter.toPath(sourceFile)] =
            TranslationResultValue(metadata = newMetadata, binaryAst = newBinaryAst, inlineData = newInlineData)
    }

    operator fun get(sourceFile: File): TranslationResultValue? =
        storage[pathConverter.toPath(sourceFile)]

    fun keys(): Collection<File> =
        storage.keys.map { pathConverter.toFile(it) }

    fun remove(sourceFile: File, changesCollector: ChangesCollector) {
        val path = pathConverter.toPath(sourceFile)
        val protoBytes = storage[path]!!.metadata
        val protoMap = protoData(sourceFile, protoBytes)

        for ((_, protoData) in protoMap) {
            changesCollector.collectProtoChanges(oldData = protoData, newData = null)
        }
        storage.remove(path)
    }
}

private object IrTranslationResultValueExternalizer : DataExternalizer<IrTranslationResultValue> {
    override fun save(output: DataOutput, value: IrTranslationResultValue) {
        output.writeArray(value.fileData)
        output.writeArray(value.symbols)
        output.writeArray(value.types)
        output.writeArray(value.strings)
        output.writeArray(value.declarations)
        output.writeArray(value.bodies)
        output.writeArray(value.fqn)
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

    override fun read(input: DataInput): IrTranslationResultValue {
        val fileData = input.readArray()
        val symbols = input.readArray()
        val types = input.readArray()
        val strings = input.readArray()
        val declarations = input.readArray()
        val bodies = input.readArray()
        val fqn = input.readArray()

        return IrTranslationResultValue(fileData, symbols, types, strings, declarations, bodies, fqn)
    }
}

private class IrTranslationResultMap(
    storageFile: File,
    private val pathConverter: FileToPathConverter
) :
    BasicStringMap<IrTranslationResultValue>(storageFile, IrTranslationResultValueExternalizer) {
    override fun dumpValue(value: IrTranslationResultValue): String =
        "Filedata: ${value.fileData.md5()}, " +
                "Symbols: ${value.symbols.md5()}, " +
                "Types: ${value.types.md5()}, " +
                "Strings: ${value.strings.md5()}, " +
                "Declarations: ${value.declarations.md5()}, " +
                "Bodies: ${value.bodies.md5()}"

    fun put(
        sourceFile: File,
        newFiledata: ByteArray,
        newSymbols: ByteArray,
        newTypes: ByteArray,
        newStrings: ByteArray,
        newDeclarations: ByteArray,
        newBodies: ByteArray,
        fqn: ByteArray
    ) {
        storage[pathConverter.toPath(sourceFile)] =
            IrTranslationResultValue(newFiledata, newSymbols, newTypes, newStrings, newDeclarations, newBodies, fqn)
    }

    operator fun get(sourceFile: File): IrTranslationResultValue? =
        storage[pathConverter.toPath(sourceFile)]

    fun keys(): Collection<File> =
        storage.keys.map { pathConverter.toFile(it) }

    fun remove(sourceFile: File) {
        val path = pathConverter.toPath(sourceFile)
        storage.remove(path)
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
            val packagePartClassId = ClassId(packageFqName, Name.identifier(sourceFile.nameWithoutExtension.capitalize() + "Kt"))
            classes[packagePartClassId] = PackagePartProtoData(this, nameResolver, packageFqName)
        }

        return classes
    }
}

// TODO: remove this method once AbstractJsProtoComparisonTest is fixed
fun getProtoData(sourceFile: File, metadata: ByteArray): Map<ClassId, ProtoData> {
    val classes = hashMapOf<ClassId, ProtoData>()
    val proto = ProtoBuf.PackageFragment.parseFrom(metadata, JsSerializerProtocol.extensionRegistry)
    val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)

    proto.class_List.forEach {
        val classId = nameResolver.getClassId(it.fqName)
        classes[classId] = ClassProtoData(it, nameResolver)
    }

    proto.`package`.apply {
        val packageFqName = getExtensionOrNull(JsProtoBuf.packageFqName)?.let(nameResolver::getPackageFqName)?.let(::FqName) ?: FqName.ROOT
        val packagePartClassId = ClassId(packageFqName, Name.identifier(sourceFile.nameWithoutExtension.capitalize() + "Kt"))
        classes[packagePartClassId] = PackagePartProtoData(this, nameResolver, packageFqName)
    }

    return classes
}

private class InlineFunctionsMap(
    storageFile: File,
    private val pathConverter: FileToPathConverter
) : BasicStringMap<Map<String, Long>>(storageFile, StringToLongMapExternalizer) {
    fun process(srcFile: File, newMap: Map<String, Long>, changesCollector: ChangesCollector) {
        val key = pathConverter.toPath(srcFile)
        val oldMap = storage[key] ?: emptyMap()

        if (newMap.isNotEmpty()) {
            storage[key] = newMap
        } else {
            storage.remove(key)
        }

        for (fn in oldMap.keys + newMap.keys) {
            val fqNameSegments = fn.removePrefix("<get>").removePrefix("<set>").split(".")
            val fqName = FqName.fromSegments(fqNameSegments)
            changesCollector.collectMemberIfValueWasChanged(fqName.parent(), fqName.shortName().asString(), oldMap[fn], newMap[fn])
        }
    }

    fun remove(sourceFile: File) {
        storage.remove(pathConverter.toPath(sourceFile))
    }

    override fun dumpValue(value: Map<String, Long>): String =
        value.dumpMap { java.lang.Long.toHexString(it) }
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


private class PackageMetadataMap(storageFile: File) : BasicStringMap<ByteArray>(storageFile, ByteArrayExternalizer) {
    fun put(packageName: String, newMetadata: ByteArray) {
        storage[packageName] = newMetadata
    }

    fun remove(packageName: String) {
        storage.remove(packageName)
    }

    fun keys() = storage.keys

    operator fun get(packageName: String) = storage[packageName]

    override fun dumpValue(value: ByteArray): String = "Package metadata: ${value.md5()}"
}