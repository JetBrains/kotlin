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
import org.jetbrains.kotlin.incremental.js.TranslationResultValue
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.js.JsProtoBuf
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import java.io.DataInput
import java.io.DataOutput
import java.io.File

open class IncrementalJsCache(cachesDir: File) : IncrementalCacheCommon(cachesDir) {
    companion object {
        private val TRANSLATION_RESULT_MAP = "translation-result"
        private val SOURCES_TO_CLASSES_FQNS = "sources-to-classes"
        private val INLINE_FUNCTIONS = "inline-functions"
        private val HEADER_FILE_NAME = "header.meta"
    }

    private val dirtySources = arrayListOf<File>()
    private val translationResults = registerMap(TranslationResultMap(TRANSLATION_RESULT_MAP.storageFile))
    private val sourcesToClasses = registerMap(SourceToClassesMap(SOURCES_TO_CLASSES_FQNS.storageFile))
    private val inlineFunctions = registerMap(InlineFunctionsMap(INLINE_FUNCTIONS.storageFile))

    private val headerFile: File
        get() = File(cachesDir, HEADER_FILE_NAME)

    var header: ByteArray
        get() = headerFile.readBytes()
        set(value) {
            cachesDir.mkdirs()
            headerFile.writeBytes(value)
        }

    override fun markDirty(removedAndCompiledSources: List<File>) {
        dirtySources.addAll(removedAndCompiledSources)
    }

    fun compareAndUpdate(incrementalResults: IncrementalResultsConsumerImpl, changesCollector: ChangesCollector) {
        val translatedFiles = incrementalResults.packageParts

        dirtySources.forEach {
            if (it !in translatedFiles) {
                translationResults.remove(it, changesCollector)
                inlineFunctions.remove(it)
            }

            removeAllFromClassStorage(sourcesToClasses[it])
            sourcesToClasses.clearOutputsForSource(it)
        }
        dirtySources.clear()

        for ((srcFile, data) in translatedFiles) {
            val (binaryMetadata, binaryAst) = data

            val oldProtoMap = translationResults[srcFile]?.metadata?.let { getProtoData(srcFile, it) } ?: emptyMap()
            val newProtoMap = getProtoData(srcFile, binaryMetadata)

            for (protoData in newProtoMap.values) {
                if (protoData is ClassProtoData) {
                    addToClassStorage(protoData.proto, protoData.nameResolver, srcFile)
                }
            }

            for (classId in oldProtoMap.keys + newProtoMap.keys) {
                changesCollector.collectProtoChanges(oldProtoMap[classId], newProtoMap[classId])
            }

            translationResults.put(srcFile, binaryMetadata, binaryAst)
        }

        for ((srcFile, inlineDeclarations) in incrementalResults.inlineFunctions) {
            inlineFunctions.process(srcFile, inlineDeclarations, changesCollector)
        }
    }

    fun nonDirtyPackageParts(): Map<File, TranslationResultValue> =
            hashMapOf<File, TranslationResultValue>().apply {
                for (path in translationResults.keys()) {
                    val file = File(path)
                    if (file !in dirtySources) {
                        put(file, translationResults[path]!!)
                    }
                }
            }
}

private class SourceToClassesMap(storageFile: File) : BasicStringMap<Collection<String>>(storageFile, PathStringDescriptor, StringCollectionExternalizer) {
    fun clearOutputsForSource(sourceFile: File) {
        remove(sourceFile.canonicalPath)
    }

    fun add(sourceFile: File, className: FqName) {
        storage.append(sourceFile.canonicalPath, className.asString())
    }

    operator fun get(sourceFile: File): Collection<FqName> =
            storage[sourceFile.canonicalPath].orEmpty().map { FqName(it) }

    override fun dumpValue(value: Collection<String>) = value.dumpCollection()

    private fun remove(path: String) {
        storage.remove(path)
    }
}

private object TranslationResultValueExternalizer : DataExternalizer<TranslationResultValue> {
    override fun save(output: DataOutput, value: TranslationResultValue) {
        output.writeInt(value.metadata.size)
        output.write(value.metadata)

        output.writeInt(value.binaryAst.size)
        output.write(value.binaryAst)
    }

    override fun read(input: DataInput): TranslationResultValue {
        val metadataSize = input.readInt()
        val metadata = ByteArray(metadataSize)
        input.readFully(metadata)

        val binaryAstSize = input.readInt()
        val binaryAst = ByteArray(binaryAstSize)
        input.readFully(binaryAst)

        return TranslationResultValue(metadata = metadata, binaryAst = binaryAst)
    }
}

private class TranslationResultMap(storageFile: File) : BasicStringMap<TranslationResultValue>(storageFile, TranslationResultValueExternalizer) {
    override fun dumpValue(value: TranslationResultValue): String =
            "Metadata: ${value.metadata.md5()}, Binary AST: ${value.binaryAst.md5()}"

    fun put(file: File, newMetadata: ByteArray, newBinaryAst: ByteArray) {
        storage[file.canonicalPath] = TranslationResultValue(metadata = newMetadata, binaryAst = newBinaryAst)
    }

    operator fun get(file: File): TranslationResultValue? =
            storage[file.canonicalPath]

    operator fun get(key: String): TranslationResultValue? =
            storage[key]

    fun keys(): Collection<String> =
            storage.keys

    fun remove(file: File, changesCollector: ChangesCollector) {
        val protoBytes = storage[file.canonicalPath]!!.metadata
        val protoMap = getProtoData(file, protoBytes)

        for ((_, protoData) in protoMap) {
            changesCollector.collectProtoChanges(oldData = protoData, newData = null)
        }
        storage.remove(file.canonicalPath)
    }
}

fun getProtoData(sourceFile: File, metadata: ByteArray): Map<ClassId, ProtoData>  {
    val classes = hashMapOf<ClassId, ProtoData>()
    val proto = ProtoBuf.PackageFragment.parseFrom(metadata, JsSerializerProtocol.extensionRegistry)
    val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)

    proto.class_List.forEach {
        val classId = nameResolver.getClassId(it.fqName)
        classes[classId] = ClassProtoData(it, nameResolver)
    }

    proto.`package`.apply {
        val packageFqName = if (hasExtension(JsProtoBuf.packageFqName)) {
            nameResolver.getPackageFqName(getExtension(JsProtoBuf.packageFqName))
        }
        else FqName.ROOT

        val packagePartClassId = ClassId(packageFqName, Name.identifier(sourceFile.nameWithoutExtension.capitalize() + "Kt"))
        classes[packagePartClassId] = PackagePartProtoData(this, nameResolver, packageFqName)
    }
    return classes
}

private class InlineFunctionsMap(storageFile: File) : BasicStringMap<Map<String, Long>>(storageFile, StringToLongMapExternalizer) {
    fun process(srcFile: File, newMap: Map<String, Long>, changesCollector: ChangesCollector) {
        val key = srcFile.canonicalPath
        val oldMap = storage[key] ?: emptyMap()

        if (newMap.isNotEmpty()) {
            storage[key] = newMap
        }
        else {
            storage.remove(key)
        }

        for (fn in oldMap.keys + newMap.keys) {
            val fqNameSegments = fn.removePrefix("<get>").removePrefix("<set>").split(".")
            val fqName = FqName.fromSegments(fqNameSegments)
            changesCollector.collectMemberIfValueWasChanged(fqName.parent(), fqName.shortName().asString(), oldMap[fn], newMap[fn])
        }
    }

    fun remove(sourceFile: File) {
        storage.remove(sourceFile.canonicalPath)
    }

    override fun dumpValue(value: Map<String, Long>): String =
            value.dumpMap { java.lang.Long.toHexString(it) }
}