/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtil.toSystemIndependentName
import com.intellij.util.io.BooleanDataDescriptor
import com.intellij.util.io.EnumeratorStringDescriptor
import gnu.trove.THashSet
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.incremental.storage.version.clean
import org.jetbrains.kotlin.incremental.storage.version.localCacheVersionManager
import org.jetbrains.kotlin.inline.inlineFunctionsJvmNames
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.metadata.jvm.deserialization.BitEncoding
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.security.MessageDigest
import java.util.*

val KOTLIN_CACHE_DIRECTORY_NAME = "kotlin"

open class IncrementalJvmCache(
    private val targetDataRoot: File,
    targetOutputDir: File?
) : AbstractIncrementalCache<JvmClassName>(File(targetDataRoot, KOTLIN_CACHE_DIRECTORY_NAME)), IncrementalCache {
    companion object {
        private val PROTO_MAP = "proto"
        private val CONSTANTS_MAP = "constants"
        private val PACKAGE_PARTS = "package-parts"
        private val MULTIFILE_CLASS_FACADES = "multifile-class-facades"
        private val MULTIFILE_CLASS_PARTS = "multifile-class-parts"
        private val INLINE_FUNCTIONS = "inline-functions"
        private val INTERNAL_NAME_TO_SOURCE = "internal-name-to-source"
        private val JAVA_SOURCES_PROTO_MAP = "java-sources-proto-map"

        private val MODULE_MAPPING_FILE_NAME = "." + ModuleMapping.MAPPING_FILE_EXT
    }

    override val sourceToClassesMap = registerMap(SourceToJvmNameMap(SOURCE_TO_CLASSES.storageFile))
    override val dirtyOutputClassesMap = registerMap(DirtyClassesJvmNameMap(DIRTY_OUTPUT_CLASSES.storageFile))

    private val protoMap = registerMap(ProtoMap(PROTO_MAP.storageFile))
    private val constantsMap = registerMap(ConstantsMap(CONSTANTS_MAP.storageFile))
    private val packagePartMap = registerMap(PackagePartMap(PACKAGE_PARTS.storageFile))
    private val multifileFacadeToParts = registerMap(MultifileClassFacadeMap(MULTIFILE_CLASS_FACADES.storageFile))
    private val partToMultifileFacade = registerMap(MultifileClassPartMap(MULTIFILE_CLASS_PARTS.storageFile))
    private val inlineFunctionsMap = registerMap(InlineFunctionsMap(INLINE_FUNCTIONS.storageFile))
    // todo: try to use internal names only?
    private val internalNameToSource = registerMap(InternalNameToSourcesMap(INTERNAL_NAME_TO_SOURCE.storageFile))
    private val javaSourcesProtoMap = registerMap(JavaSourcesProtoMap(JAVA_SOURCES_PROTO_MAP.storageFile))

    private val outputDir by lazy(LazyThreadSafetyMode.NONE) { requireNotNull(targetOutputDir) { "Target is expected to have output directory" } }

    protected open fun debugLog(message: String) {}

    fun isTrackedFile(file: File) = sourceToClassesMap.contains(file)

    // used in gradle
    @Suppress("unused")
    fun classesBySources(sources: Iterable<File>): Iterable<JvmClassName> =
        sources.flatMap { sourceToClassesMap[it] }

    fun sourceInCache(file: File): Boolean =
        sourceToClassesMap.contains(file)

    fun sourcesByInternalName(internalName: String): Collection<File> =
        internalNameToSource[internalName]

    fun isMultifileFacade(className: JvmClassName): Boolean =
        className in multifileFacadeToParts

    override fun getClassFilePath(internalClassName: String): String {
        return toSystemIndependentName(File(outputDir, "$internalClassName.class").canonicalPath)
    }

    fun saveModuleMappingToCache(sourceFiles: Collection<File>, file: File) {
        val jvmClassName = JvmClassName.byInternalName(MODULE_MAPPING_FILE_NAME)
        protoMap.storeModuleMapping(jvmClassName, file.readBytes())
        dirtyOutputClassesMap.notDirty(jvmClassName)
        sourceFiles.forEach { sourceToClassesMap.add(it, jvmClassName) }
    }

    open fun saveFileToCache(generatedClass: GeneratedJvmClass, changesCollector: ChangesCollector) {
        val sourceFiles: Collection<File> = generatedClass.sourceFiles
        val kotlinClass: LocalFileKotlinClass = generatedClass.outputClass
        val className = kotlinClass.className

        dirtyOutputClassesMap.notDirty(className)
        sourceFiles.forEach {
            sourceToClassesMap.add(it, className)
        }

        internalNameToSource[className.internalName] = sourceFiles

        if (kotlinClass.classId.isLocal) return

        val header = kotlinClass.classHeader
        when (header.kind) {
            KotlinClassHeader.Kind.FILE_FACADE -> {
                assert(sourceFiles.size == 1) { "Package part from several source files: $sourceFiles" }
                packagePartMap.addPackagePart(className)

                protoMap.process(kotlinClass, changesCollector)
                constantsMap.process(kotlinClass, changesCollector)
                inlineFunctionsMap.process(kotlinClass, changesCollector)
            }
            KotlinClassHeader.Kind.MULTIFILE_CLASS -> {
                val partNames = kotlinClass.classHeader.data?.toList()
                    ?: throw AssertionError("Multifile class has no parts: ${kotlinClass.className}")
                multifileFacadeToParts[className] = partNames
                // When a class is replaced with a facade with the same name,
                // the class' proto wouldn't ever be deleted,
                // because we don't write proto for multifile facades.
                // As a workaround we can remove proto values for multifile facades.
                if (className in protoMap) {
                    changesCollector.collectSignature(className.fqNameForClassNameWithoutDollars, areSubclassesAffected = true)
                }
                protoMap.remove(className, changesCollector)
                classFqNameToSourceMap.remove(className.fqNameForClassNameWithoutDollars)
                internalNameToSource.remove(className.internalName)

                // TODO NO_CHANGES? (delegates only)
                constantsMap.process(kotlinClass, changesCollector)
                inlineFunctionsMap.process(kotlinClass, changesCollector)
            }
            KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
                assert(sourceFiles.size == 1) { "Multifile class part from several source files: $sourceFiles" }
                packagePartMap.addPackagePart(className)
                partToMultifileFacade.set(className.internalName, header.multifileClassName!!)

                protoMap.process(kotlinClass, changesCollector)
                constantsMap.process(kotlinClass, changesCollector)
                inlineFunctionsMap.process(kotlinClass, changesCollector)
            }
            KotlinClassHeader.Kind.CLASS -> {
                assert(sourceFiles.size == 1) { "Class is expected to have only one source file: $sourceFiles" }
                addToClassStorage(kotlinClass, sourceFiles.first())

                protoMap.process(kotlinClass, changesCollector)
                constantsMap.process(kotlinClass, changesCollector)
                inlineFunctionsMap.process(kotlinClass, changesCollector)
            }
        }
    }

    fun saveJavaClassProto(source: File, serializedJavaClass: SerializedJavaClass, collector: ChangesCollector) {
        val jvmClassName = JvmClassName.byClassId(serializedJavaClass.classId)
        javaSourcesProtoMap.process(jvmClassName, serializedJavaClass, collector)
        sourceToClassesMap.add(source, jvmClassName)
        val (proto, nameResolver) = serializedJavaClass.toProtoData()
        addToClassStorage(proto, nameResolver, source)

        dirtyOutputClassesMap.notDirty(jvmClassName)
    }

    fun getObsoleteJavaClasses(): Collection<ClassId> =
        dirtyOutputClassesMap.getDirtyOutputClasses()
            .mapNotNull {
                javaSourcesProtoMap[it]?.classId
            }

    fun isJavaClassToTrack(classId: ClassId): Boolean {
        val jvmClassName = JvmClassName.byClassId(classId)
        return dirtyOutputClassesMap.isDirty(jvmClassName) ||
                jvmClassName !in javaSourcesProtoMap
    }

    fun isJavaClassAlreadyInCache(classId: ClassId): Boolean {
        val jvmClassName = JvmClassName.byClassId(classId)
        return jvmClassName in javaSourcesProtoMap
    }

    override fun clearCacheForRemovedClasses(changesCollector: ChangesCollector) {
        val dirtyClasses = dirtyOutputClassesMap.getDirtyOutputClasses()

        val facadesWithRemovedParts = hashMapOf<JvmClassName, MutableSet<String>>()
        for (dirtyClass in dirtyClasses) {
            val facade = partToMultifileFacade.get(dirtyClass) ?: continue
            val facadeClassName = JvmClassName.byInternalName(facade)
            val removedParts = facadesWithRemovedParts.getOrPut(facadeClassName) { hashSetOf() }
            removedParts.add(dirtyClass.internalName)
        }

        for ((facade, removedParts) in facadesWithRemovedParts.entries) {
            val allParts = multifileFacadeToParts[facade] ?: continue
            val notRemovedParts = allParts.filter { it !in removedParts }

            if (notRemovedParts.isEmpty()) {
                multifileFacadeToParts.remove(facade)
            } else {
                multifileFacadeToParts[facade] = notRemovedParts
            }
        }

        dirtyClasses.forEach {
            protoMap.remove(it, changesCollector)
            packagePartMap.remove(it)
            multifileFacadeToParts.remove(it)
            partToMultifileFacade.remove(it)
            constantsMap.remove(it)
            inlineFunctionsMap.remove(it)
            internalNameToSource.remove(it.internalName)
            javaSourcesProtoMap.remove(it, changesCollector)
        }

        removeAllFromClassStorage(dirtyClasses.map { it.fqNameForClassNameWithoutDollars }, changesCollector)
        dirtyOutputClassesMap.clean()
    }

    override fun getObsoletePackageParts(): Collection<String> {
        val obsoletePackageParts = dirtyOutputClassesMap.getDirtyOutputClasses().filter(packagePartMap::isPackagePart)
        debugLog("Obsolete package parts: $obsoletePackageParts")
        return obsoletePackageParts.map { it.internalName }
    }

    override fun getPackagePartData(partInternalName: String): JvmPackagePartProto? {
        return protoMap[JvmClassName.byInternalName(partInternalName)]?.let { value ->
            JvmPackagePartProto(value.bytes, value.strings)
        }
    }

    override fun getObsoleteMultifileClasses(): Collection<String> {
        val obsoleteMultifileClasses = linkedSetOf<String>()
        for (dirtyClass in dirtyOutputClassesMap.getDirtyOutputClasses()) {
            val dirtyFacade = partToMultifileFacade.get(dirtyClass) ?: continue
            obsoleteMultifileClasses.add(dirtyFacade)
        }
        debugLog("Obsolete multifile class facades: $obsoleteMultifileClasses")
        return obsoleteMultifileClasses
    }

    override fun getStableMultifileFacadeParts(facadeInternalName: String): Collection<String>? {
        val jvmClassName = JvmClassName.byInternalName(facadeInternalName)
        val partNames = multifileFacadeToParts[jvmClassName] ?: return null
        return partNames.filter { !dirtyOutputClassesMap.isDirty(JvmClassName.byInternalName(it)) }
    }

    override fun getModuleMappingData(): ByteArray? {
        return protoMap[JvmClassName.byInternalName(MODULE_MAPPING_FILE_NAME)]?.bytes
    }

    override fun clean() {
        super.clean()
        localCacheVersionManager(targetDataRoot, IncrementalCompilation.isEnabledForJvm()).clean()
    }

    private inner class ProtoMap(storageFile: File) : BasicStringMap<ProtoMapValue>(storageFile, ProtoMapValueExternalizer) {

        fun process(kotlinClass: LocalFileKotlinClass, changesCollector: ChangesCollector) {
            return put(kotlinClass, changesCollector)
        }

        // A module mapping (.kotlin_module file) is stored in a cache,
        // because a corresponding file will be deleted on each round
        // (it is reported as output for each [package part?] source file).
        // If a mapping is not preserved, a resulting file will only contain data
        // from files compiled during last round.
        // However there is no need to compare old and new data in this case
        // (also that would fail with exception).
        fun storeModuleMapping(className: JvmClassName, bytes: ByteArray) {
            storage[className.internalName] = ProtoMapValue(isPackageFacade = false, bytes = bytes, strings = emptyArray())
        }

        private fun put(kotlinClass: LocalFileKotlinClass, changesCollector: ChangesCollector) {
            val header = kotlinClass.classHeader

            val key = kotlinClass.className.internalName
            val oldData = storage[key]
            val newData = ProtoMapValue(
                header.kind != KotlinClassHeader.Kind.CLASS,
                BitEncoding.decodeBytes(header.data!!),
                header.strings!!
            )
            storage[key] = newData

            val packageFqName = kotlinClass.className.packageFqName
            changesCollector.collectProtoChanges(oldData?.toProtoData(packageFqName), newData.toProtoData(packageFqName))
        }

        operator fun contains(className: JvmClassName): Boolean =
            className.internalName in storage

        operator fun get(className: JvmClassName): ProtoMapValue? =
            storage[className.internalName]

        fun remove(className: JvmClassName, changesCollector: ChangesCollector) {
            val key = className.internalName
            val oldValue = storage[key] ?: return
            if (key != MODULE_MAPPING_FILE_NAME) {
                changesCollector.collectProtoChanges(oldData = oldValue.toProtoData(className.packageFqName), newData = null)
            }
            storage.remove(key)
        }

        override fun dumpValue(value: ProtoMapValue): String {
            return (if (value.isPackageFacade) "1" else "0") + java.lang.Long.toHexString(value.bytes.md5())
        }
    }

    private inner class JavaSourcesProtoMap(storageFile: File) :
        BasicStringMap<SerializedJavaClass>(storageFile, JavaClassProtoMapValueExternalizer) {
        fun process(jvmClassName: JvmClassName, newData: SerializedJavaClass, changesCollector: ChangesCollector) {
            val key = jvmClassName.internalName
            val oldData = storage[key]
            storage[key] = newData

            changesCollector.collectProtoChanges(
                oldData?.toProtoData(), newData.toProtoData(),
                collectAllMembersForNewClass = true
            )
        }

        fun remove(className: JvmClassName, changesCollector: ChangesCollector) {
            val key = className.internalName
            val oldValue = storage[key] ?: return
            storage.remove(key)

            changesCollector.collectProtoChanges(oldValue.toProtoData(), newData = null)
        }

        operator fun get(className: JvmClassName): SerializedJavaClass? =
            storage[className.internalName]

        operator fun contains(className: JvmClassName): Boolean =
            className.internalName in storage

        override fun dumpValue(value: SerializedJavaClass): String =
            java.lang.Long.toHexString(value.proto.toByteArray().md5())
    }

    // todo: reuse code with InlineFunctionsMap?
    private inner class ConstantsMap(storageFile: File) : BasicStringMap<Map<String, Any>>(storageFile, ConstantsMapExternalizer) {
        private fun getConstantsMap(bytes: ByteArray): Map<String, Any> {
            val result = HashMap<String, Any>()

            ClassReader(bytes).accept(object : ClassVisitor(Opcodes.ASM5) {
                override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
                    val staticFinal = Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or Opcodes.ACC_PRIVATE
                    if (value != null && access and staticFinal == Opcodes.ACC_STATIC or Opcodes.ACC_FINAL) {
                        result[name] = value
                    }
                    return null
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

            return result
        }

        operator fun contains(className: JvmClassName): Boolean =
            className.internalName in storage

        fun process(kotlinClass: LocalFileKotlinClass, changesCollector: ChangesCollector) {
            val key = kotlinClass.className.internalName
            val oldMap = storage[key] ?: emptyMap()

            val newMap = getConstantsMap(kotlinClass.fileContents)
            if (newMap.isNotEmpty()) {
                storage[key] = newMap
            } else {
                storage.remove(key)
            }

            for (const in oldMap.keys + newMap.keys) {
                changesCollector.collectMemberIfValueWasChanged(kotlinClass.scopeFqName(), const, oldMap[const], newMap[const])
            }
        }

        fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: Map<String, Any>): String =
            value.dumpMap(Any::toString)
    }

    private inner class PackagePartMap(storageFile: File) : BasicStringMap<Boolean>(storageFile, BooleanDataDescriptor.INSTANCE) {
        fun addPackagePart(className: JvmClassName) {
            storage[className.internalName] = true
        }

        fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        fun isPackagePart(className: JvmClassName): Boolean =
            className.internalName in storage

        override fun dumpValue(value: Boolean) = ""
    }

    private inner class MultifileClassFacadeMap(storageFile: File) :
        BasicStringMap<Collection<String>>(storageFile, StringCollectionExternalizer) {
        operator fun set(className: JvmClassName, partNames: Collection<String>) {
            storage[className.internalName] = partNames
        }

        operator fun get(className: JvmClassName): Collection<String>? =
            storage[className.internalName]

        operator fun contains(className: JvmClassName): Boolean =
            className.internalName in storage

        fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: Collection<String>): String = value.dumpCollection()
    }

    private inner class MultifileClassPartMap(storageFile: File) :
        BasicStringMap<String>(storageFile, EnumeratorStringDescriptor.INSTANCE) {
        fun set(partName: String, facadeName: String) {
            storage[partName] = facadeName
        }

        fun get(partName: JvmClassName): String? =
            storage[partName.internalName]

        fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: String): String = value
    }

    inner class InternalNameToSourcesMap(storageFile: File) :
        BasicStringMap<Collection<String>>(storageFile, EnumeratorStringDescriptor(), PathCollectionExternalizer) {
        operator fun set(internalName: String, sourceFiles: Iterable<File>) {
            storage[internalName] = sourceFiles.map { it.canonicalPath }
        }

        operator fun get(internalName: String): Collection<File> =
            (storage[internalName] ?: emptyList()).map(::File)

        fun remove(internalName: String) {
            storage.remove(internalName)
        }

        override fun dumpValue(value: Collection<String>): String =
            value.dumpCollection()
    }

    private fun addToClassStorage(kotlinClass: LocalFileKotlinClass, srcFile: File) {
        val (nameResolver, proto) = JvmProtoBufUtil.readClassDataFrom(kotlinClass.classHeader.data!!, kotlinClass.classHeader.strings!!)
        addToClassStorage(proto, nameResolver, srcFile)
    }

    private inner class InlineFunctionsMap(storageFile: File) :
        BasicStringMap<Map<String, Long>>(storageFile, StringToLongMapExternalizer) {
        private fun getInlineFunctionsMap(header: KotlinClassHeader, bytes: ByteArray): Map<String, Long> {
            val inlineFunctions = inlineFunctionsJvmNames(header)
            if (inlineFunctions.isEmpty()) return emptyMap()

            val result = HashMap<String, Long>()

            ClassReader(bytes).accept(object : ClassVisitor(Opcodes.ASM5) {
                override fun visitMethod(
                    access: Int,
                    name: String,
                    desc: String,
                    signature: String?,
                    exceptions: Array<out String>?
                ): MethodVisitor? {
                    val dummyClassWriter = ClassWriter(Opcodes.ASM5)

                    return object : MethodVisitor(Opcodes.ASM5, dummyClassWriter.visitMethod(0, name, desc, null, exceptions)) {
                        override fun visitEnd() {
                            val jvmName = name + desc
                            if (jvmName !in inlineFunctions) return

                            val dummyBytes = dummyClassWriter.toByteArray()!!
                            val hash = dummyBytes.md5()
                            result[jvmName] = hash
                        }
                    }
                }

            }, 0)

            return result
        }

        fun process(kotlinClass: LocalFileKotlinClass, changesCollector: ChangesCollector) {
            val key = kotlinClass.className.internalName
            val oldMap = storage[key] ?: emptyMap()

            val newMap = getInlineFunctionsMap(kotlinClass.classHeader, kotlinClass.fileContents)
            if (newMap.isNotEmpty()) {
                storage[key] = newMap
            } else {
                storage.remove(key)
            }

            for (fn in oldMap.keys + newMap.keys) {
                changesCollector.collectMemberIfValueWasChanged(
                    kotlinClass.scopeFqName(),
                    functionNameBySignature(fn),
                    oldMap[fn],
                    newMap[fn]
                )
            }
        }

        // TODO get name in better way instead of using substringBefore
        private fun functionNameBySignature(signature: String): String =
            signature.substringBefore("(")

        fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: Map<String, Long>): String =
            value.dumpMap { java.lang.Long.toHexString(it) }
    }
}

private object PathCollectionExternalizer :
    CollectionExternalizer<String>(PathStringDescriptor, { THashSet(FileUtil.PATH_HASHING_STRATEGY) })

sealed class ChangeInfo(val fqName: FqName) {
    open class MembersChanged(fqName: FqName, val names: Collection<String>) : ChangeInfo(fqName) {
        override fun toStringProperties(): String = super.toStringProperties() + ", names = $names"
    }

    class Removed(fqName: FqName, names: Collection<String>) : MembersChanged(fqName, names)

    class SignatureChanged(fqName: FqName, val areSubclassesAffected: Boolean) : ChangeInfo(fqName)


    protected open fun toStringProperties(): String = "fqName = $fqName"

    override fun toString(): String {
        return this::class.java.simpleName + "(${toStringProperties()})"
    }
}

private fun LocalFileKotlinClass.scopeFqName() =
    when (classHeader.kind) {
        KotlinClassHeader.Kind.CLASS -> className.fqNameForClassNameWithoutDollars
        else -> className.packageFqName
    }

fun ByteArray.md5(): Long {
    val d = MessageDigest.getInstance("MD5").digest(this)!!
    return ((d[0].toLong() and 0xFFL)
            or ((d[1].toLong() and 0xFFL) shl 8)
            or ((d[2].toLong() and 0xFFL) shl 16)
            or ((d[3].toLong() and 0xFFL) shl 24)
            or ((d[4].toLong() and 0xFFL) shl 32)
            or ((d[5].toLong() and 0xFFL) shl 40)
            or ((d[6].toLong() and 0xFFL) shl 48)
            or ((d[7].toLong() and 0xFFL) shl 56)
            )
}

@TestOnly
fun <K : Comparable<K>, V> Map<K, V>.dumpMap(dumpValue: (V) -> String): String =
    buildString {
        append("{")
        for (key in keys.sorted()) {
            if (length != 1) {
                append(", ")
            }

            val value = get(key)?.let(dumpValue) ?: "null"
            append("$key -> $value")
        }
        append("}")
    }

@TestOnly
fun <T : Comparable<T>> Collection<T>.dumpCollection(): String =
    "[${sorted().joinToString(", ", transform = Any::toString)}]"
