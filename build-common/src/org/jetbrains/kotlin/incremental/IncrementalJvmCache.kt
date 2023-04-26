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

import com.intellij.openapi.util.io.FileUtil.toSystemIndependentName
import com.intellij.util.containers.CollectionFactory
import com.intellij.util.io.BooleanDataDescriptor
import com.intellij.util.io.EnumeratorStringDescriptor
import gnu.trove.THashSet
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.inline.InlineFunction
import org.jetbrains.kotlin.inline.InlineFunctionOrAccessor
import org.jetbrains.kotlin.inline.InlinePropertyAccessor
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File
import java.security.MessageDigest

const val KOTLIN_CACHE_DIRECTORY_NAME = "kotlin"

open class IncrementalJvmCache(
    targetDataRoot: File,
    icContext: IncrementalCompilationContext,
    targetOutputDir: File?,
) : AbstractIncrementalCache<JvmClassName>(
    workingDir = File(targetDataRoot, KOTLIN_CACHE_DIRECTORY_NAME),
    icContext,
), IncrementalCache {
    companion object {
        private const val PROTO_MAP = "proto"
        private const val FE_PROTO_MAP = "fe-proto"
        private const val CONSTANTS_MAP = "constants"
        private const val PACKAGE_PARTS = "package-parts"
        private const val MULTIFILE_CLASS_FACADES = "multifile-class-facades"
        private const val MULTIFILE_CLASS_PARTS = "multifile-class-parts"
        private const val INLINE_FUNCTIONS = "inline-functions"
        private const val INTERNAL_NAME_TO_SOURCE = "internal-name-to-source"
        private const val JAVA_SOURCES_PROTO_MAP = "java-sources-proto-map"

        private const val MODULE_MAPPING_FILE_NAME = "." + ModuleMapping.MAPPING_FILE_EXT
    }

    override val sourceToClassesMap = registerMap(SourceToJvmNameMap(SOURCE_TO_CLASSES.storageFile, icContext))
    override val dirtyOutputClassesMap = registerMap(DirtyClassesJvmNameMap(DIRTY_OUTPUT_CLASSES.storageFile, icContext))

    private val protoMap = registerMap(ProtoMap(PROTO_MAP.storageFile, icContext))
    private val feProtoMap = registerMap(ProtoMap(FE_PROTO_MAP.storageFile, icContext))
    private val constantsMap = registerMap(ConstantsMap(CONSTANTS_MAP.storageFile, icContext))
    private val packagePartMap = registerMap(PackagePartMap(PACKAGE_PARTS.storageFile, icContext))
    private val multifileFacadeToParts = registerMap(MultifileClassFacadeMap(MULTIFILE_CLASS_FACADES.storageFile, icContext))
    private val partToMultifileFacade = registerMap(MultifileClassPartMap(MULTIFILE_CLASS_PARTS.storageFile, icContext))
    private val inlineFunctionsMap = registerMap(InlineFunctionsMap(INLINE_FUNCTIONS.storageFile, icContext))
    // todo: try to use internal names only?
    private val internalNameToSource = registerMap(InternalNameToSourcesMap(INTERNAL_NAME_TO_SOURCE.storageFile, icContext))
    // gradle only
    private val javaSourcesProtoMap = registerMap(JavaSourcesProtoMap(JAVA_SOURCES_PROTO_MAP.storageFile, icContext))

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

    fun getAllPartsOfMultifileFacade(facade: JvmClassName): Collection<String>? {
        return multifileFacadeToParts[facade]
    }

    fun isMultifileFacade(className: JvmClassName): Boolean =
        className in multifileFacadeToParts

    override fun getClassFilePath(internalClassName: String): String {
        return toSystemIndependentName(File(outputDir, "$internalClassName.class").normalize().absolutePath)
    }

    fun saveModuleMappingToCache(sourceFiles: Collection<File>, file: File) {
        val jvmClassName = JvmClassName.byInternalName(MODULE_MAPPING_FILE_NAME)
        protoMap.storeModuleMapping(jvmClassName, file.readBytes())
        dirtyOutputClassesMap.notDirty(jvmClassName)
        sourceFiles.forEach { sourceToClassesMap.add(it, jvmClassName) }
    }

    open fun saveFileToCache(generatedClass: GeneratedJvmClass, changesCollector: ChangesCollector) {
        saveClassToCache(KotlinClassInfo.createFrom(generatedClass.outputClass), generatedClass.sourceFiles, changesCollector)
    }

    /**
     * Saves information about the given (Kotlin) class to this cache, and stores changes between this class and its previous version into
     * the given [ChangesCollector].
     *
     * @param kotlinClassInfo Information about a Kotlin class
     * @param sourceFiles The source files that the given class was generated from, or `null` if this information is not available
     * @param changesCollector A [ChangesCollector]
     */
    fun saveClassToCache(kotlinClassInfo: KotlinClassInfo, sourceFiles: List<File>?, changesCollector: ChangesCollector) {
        val className = kotlinClassInfo.className

        dirtyOutputClassesMap.notDirty(className)

        if (sourceFiles != null) {
            sourceFiles.forEach {
                sourceToClassesMap.add(it, className)
            }
            internalNameToSource[className.internalName] = sourceFiles
        }

        if (kotlinClassInfo.classId.isLocal) return

        when (kotlinClassInfo.classKind) {
            KotlinClassHeader.Kind.FILE_FACADE -> {
                if (sourceFiles != null) {
                    assert(sourceFiles.size == 1) { "Package part from several source files: $sourceFiles" }
                }
                packagePartMap.addPackagePart(className)

                protoMap.process(kotlinClassInfo, changesCollector)
                constantsMap.process(kotlinClassInfo, changesCollector)
                inlineFunctionsMap.process(kotlinClassInfo, changesCollector)
            }
            KotlinClassHeader.Kind.MULTIFILE_CLASS -> {
                val partNames = kotlinClassInfo.classHeaderData.toList()
                check(partNames.isNotEmpty()) { "Multifile class has no parts: $className" }
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
                classAttributesMap.remove(className.fqNameForClassNameWithoutDollars)
                internalNameToSource.remove(className.internalName)

                // TODO NO_CHANGES? (delegates only)
                constantsMap.process(kotlinClassInfo, changesCollector)
                inlineFunctionsMap.process(kotlinClassInfo, changesCollector)
            }
            KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
                if (sourceFiles != null) {
                    assert(sourceFiles.size == 1) { "Multifile class part from several source files: $sourceFiles" }
                }
                packagePartMap.addPackagePart(className)
                partToMultifileFacade.set(className.internalName, kotlinClassInfo.multifileClassName!!)

                protoMap.process(kotlinClassInfo, changesCollector)
                constantsMap.process(kotlinClassInfo, changesCollector)
                inlineFunctionsMap.process(kotlinClassInfo, changesCollector)
            }
            KotlinClassHeader.Kind.CLASS -> {
                addToClassStorage(kotlinClassInfo.protoData as ClassProtoData, sourceFiles?.let { sourceFiles.single() })

                protoMap.process(kotlinClassInfo, changesCollector)
                constantsMap.process(kotlinClassInfo, changesCollector)
                inlineFunctionsMap.process(kotlinClassInfo, changesCollector)
            }
            KotlinClassHeader.Kind.UNKNOWN, KotlinClassHeader.Kind.SYNTHETIC_CLASS -> {
            }
        }
    }

    fun saveFrontendClassToCache(
        classId: ClassId,
        classProto: ProtoBuf.Class,
        stringTable: JvmStringTable,
        sourceFiles: List<File>?,
        changesCollector: ChangesCollector
    ) {

        val className = JvmClassName.byClassId(classId)

        if (sourceFiles != null) {
            internalNameToSource[className.internalName] = sourceFiles
        }

        if (classId.isLocal) return

        val newProtoData = ClassProtoData(classProto, stringTable.toNameResolver())
        addToClassStorage(newProtoData, sourceFiles?.let { sourceFiles.single() })

        feProtoMap.putAndCollect(
            className,
            ProtoMapValue(
                false,
                JvmProtoBufUtil.writeDataBytes(stringTable, classProto),
                stringTable.strings.toTypedArray()
            ),
            newProtoData,
            changesCollector
        )
    }

    fun collectClassChangesByFeMetadata(
        className: JvmClassName, classProto: ProtoBuf.Class, stringTable: JvmStringTable, changesCollector: ChangesCollector
    ) {
        //class
        feProtoMap.check(className, classProto, stringTable, changesCollector)
    }

    fun saveJavaClassProto(source: File?, serializedJavaClass: SerializedJavaClass, collector: ChangesCollector) {
        val jvmClassName = JvmClassName.byClassId(serializedJavaClass.classId)
        javaSourcesProtoMap.process(jvmClassName, serializedJavaClass, collector)
        source?.let { sourceToClassesMap.add(source, jvmClassName) }
        addToClassStorage(serializedJavaClass.toProtoData(), source)
//        collector.addJavaProto(ClassProtoData(proto, nameResolver))
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
            feProtoMap.remove(it, changesCollector)
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

    private inner class ProtoMap(
        storageFile: File,
        icContext: IncrementalCompilationContext,
    ) : BasicStringMap<ProtoMapValue>(storageFile, ProtoMapValueExternalizer, icContext) {

        @Synchronized
        fun process(kotlinClassInfo: KotlinClassInfo, changesCollector: ChangesCollector) {
            return putAndCollect(
                kotlinClassInfo.className,
                kotlinClassInfo.protoMapValue,
                kotlinClassInfo.protoData,
                changesCollector
            )
        }

        // A module mapping (.kotlin_module file) is stored in a cache,
        // because a corresponding file will be deleted on each round
        // (it is reported as output for each [package part?] source file).
        // If a mapping is not preserved, a resulting file will only contain data
        // from files compiled during last round.
        // However there is no need to compare old and new data in this case
        // (also that would fail with exception).
        @Synchronized
        fun storeModuleMapping(className: JvmClassName, bytes: ByteArray) {
            storage[className.internalName] = ProtoMapValue(isPackageFacade = false, bytes = bytes, strings = emptyArray())
        }

        @Synchronized
        fun putAndCollect(
            className: JvmClassName,
            newMapValue: ProtoMapValue,
            newProtoData: ProtoData,
            changesCollector: ChangesCollector
        ) {
            val key = className.internalName
            val oldMapValue = storage[key]
            storage[key] = newMapValue

            changesCollector.collectProtoChanges(oldMapValue?.toProtoData(className.packageFqName), newProtoData, packageProtoKey = key)
        }

        fun check(
            className: JvmClassName, classProto: ProtoBuf.Class, stringTable: JvmStringTable, changesCollector: ChangesCollector
        ) {
            val key = className.internalName
            val oldProtoData = storage[key]?.toProtoData(className.packageFqName)
            val newProtoData = ClassProtoData(classProto, stringTable.toNameResolver())
            changesCollector.collectProtoChanges(oldProtoData, newProtoData, packageProtoKey = key)
        }

        operator fun contains(className: JvmClassName): Boolean =
            className.internalName in storage

        operator fun get(className: JvmClassName): ProtoMapValue? =
            storage[className.internalName]

        @Synchronized
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

    private inner class JavaSourcesProtoMap(
        storageFile: File,
        icContext: IncrementalCompilationContext,
    ) :
        BasicStringMap<SerializedJavaClass>(storageFile, JavaClassProtoMapValueExternalizer, icContext) {

        @Synchronized
        fun process(jvmClassName: JvmClassName, newData: SerializedJavaClass, changesCollector: ChangesCollector) {
            val key = jvmClassName.internalName
            val oldData = storage[key]
            storage[key] = newData

            changesCollector.collectProtoChanges(
                oldData?.toProtoData(), newData.toProtoData(),
                collectAllMembersForNewClass = true
            )
        }

        @Synchronized
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
    private inner class ConstantsMap(
        storageFile: File,
        icContext: IncrementalCompilationContext,
    ) :
        BasicStringMap<Map<String, Long>>(storageFile, MapExternalizer(StringExternalizer, LongExternalizer), icContext) {

        operator fun contains(className: JvmClassName): Boolean =
            className.internalName in storage

        @Synchronized
        fun process(kotlinClassInfo: KotlinClassInfo, changesCollector: ChangesCollector) {
            val key = kotlinClassInfo.className.internalName
            val oldMap = storage[key] ?: emptyMap()

            val newMap = kotlinClassInfo.extraInfo.constantSnapshots
            if (newMap.isNotEmpty()) {
                storage[key] = newMap
            } else {
                storage.remove(key)
            }

            val allConstants = oldMap.keys + newMap.keys
            if (allConstants.isEmpty()) return

            val scope = kotlinClassInfo.scopeFqName()
            for (const in allConstants) {
                changesCollector.collectMemberIfValueWasChanged(scope, const, oldMap[const], newMap[const])
            }

            // If a constant is defined in a companion object of class A, its name and type will be found in the Kotlin metadata of
            // `A$Companion.class`, but its value will only be found in the Java bytecode code of `A.class` (see
            // `org.jetbrains.kotlin.incremental.classpathDiff.ConstantsInCompanionObjectImpact` for more details).
            // Therefore, if the value of `CONSTANT` in `A.class` has changed, we will report that `A.CONSTANT` has changed in the code
            // above, and report that `A.Companion.CONSTANT` is impacted in the code below.
            kotlinClassInfo.companionObject?.let { companionObjectClassId ->
                // Note that `companionObjectClassId` is the companion object of the current class. Here we assume that the previous class
                // also has a companion object with the same name. If that is not the case, that change will be detected when comparing
                // protos, and the report below will be imprecise/redundant, but it's okay to over-approximate the result.
                val companionObjectFqName = companionObjectClassId.asSingleFqName()
                for (const in allConstants) {
                    changesCollector.collectMemberIfValueWasChanged(
                        scope = companionObjectFqName, name = const, oldMap[const], newMap[const]
                    )
                }
            }
        }

        @Synchronized
        fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: Map<String, Long>): String =
            value.dumpMap(Long::toString)
    }

    private inner class PackagePartMap(
        storageFile: File,
        icContext: IncrementalCompilationContext,
    ) : BasicStringMap<Boolean>(storageFile, BooleanDataDescriptor.INSTANCE, icContext) {
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

    private inner class MultifileClassFacadeMap(
        storageFile: File,
        icContext: IncrementalCompilationContext,
    ) :
        BasicStringMap<Collection<String>>(storageFile, StringCollectionExternalizer, icContext) {

        @Synchronized
        operator fun set(className: JvmClassName, partNames: Collection<String>) {
            storage[className.internalName] = partNames
        }

        operator fun get(className: JvmClassName): Collection<String>? =
            storage[className.internalName]

        operator fun contains(className: JvmClassName): Boolean =
            className.internalName in storage

        @Synchronized
        fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: Collection<String>): String = value.dumpCollection()
    }

    private inner class MultifileClassPartMap(
        storageFile: File,
        icContext: IncrementalCompilationContext,
    ) :
        BasicStringMap<String>(storageFile, EnumeratorStringDescriptor.INSTANCE, icContext) {

        @Synchronized
        fun set(partName: String, facadeName: String) {
            storage[partName] = facadeName
        }

        fun get(partName: JvmClassName): String? =
            storage[partName.internalName]

        @Synchronized
        fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: String): String = value
    }

    inner class InternalNameToSourcesMap(
        storageFile: File,
        icContext: IncrementalCompilationContext,
    ) : BasicStringMap<Collection<String>>(storageFile, EnumeratorStringDescriptor(), PathCollectionExternalizer, icContext) {
        operator fun set(internalName: String, sourceFiles: Collection<File>) {
            storage[internalName] = pathConverter.toPaths(sourceFiles)
        }

        operator fun get(internalName: String): Collection<File> =
            pathConverter.toFiles(storage[internalName] ?: emptyList())

        fun remove(internalName: String) {
            storage.remove(internalName)
        }

        override fun dumpValue(value: Collection<String>): String =
            value.dumpCollection()
    }

    private inner class InlineFunctionsMap(
        storageFile: File,
        icContext: IncrementalCompilationContext,
    ) :
        BasicStringMap<Map<InlineFunctionOrAccessor, Long>>(
            storageFile,
            MapExternalizer(InlineFunctionOrAccessorExternalizer, LongExternalizer),
            icContext
        ) {

        @Synchronized
        fun process(kotlinClassInfo: KotlinClassInfo, changesCollector: ChangesCollector) {
            val key = kotlinClassInfo.className.internalName
            val oldMap = storage[key] ?: emptyMap()

            val newMap = kotlinClassInfo.extraInfo.inlineFunctionOrAccessorSnapshots
            if (newMap.isNotEmpty()) {
                storage[key] = newMap
            } else {
                storage.remove(key)
            }

            // Note: If we detect a change in an inline function `foo` with @JvmName `fooJvmName`, we have two options:
            //   1. Report that function `foo` has changed
            //   2. Report that method `fooJvmName` has changed
            //
            // Similarly, if we detect a change in an inline property accessor with JvmName `getFoo` of property `foo`, we have two options:
            //   1. Report that property `foo` has changed
            //   2. Report that property accessor `getFoo` has changed
            //
            // The compiler is guaranteed to generate `LookupSymbol`s corresponding to option 1 when referencing inline functions/property
            // accessors, but it is not guaranteed to generate `LookupSymbol`s corresponding to option 2. (Currently the compiler seems to
            // support option 2 for *inline* functions/property accessors, but that may change.)
            //
            // In the following, we will choose option 1 as it is cleaner and safer.
            val scope = kotlinClassInfo.scopeFqName()
            (oldMap.keys + newMap.keys).forEach {
                val name = when (it) {
                    is InlineFunction -> it.kotlinFunctionName
                    is InlinePropertyAccessor -> it.propertyName
                }
                changesCollector.collectMemberIfValueWasChanged(scope, name, oldMap[it], newMap[it])
            }
        }

        @Synchronized
        fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: Map<InlineFunctionOrAccessor, Long>): String =
            value.mapKeys { it.key.jvmMethodSignature.asString() }.dumpMap { java.lang.Long.toHexString(it) }
    }

    private fun KotlinClassInfo.scopeFqName() = when (classKind) {
        KotlinClassHeader.Kind.CLASS -> classId.asSingleFqName()
        else -> classId.packageFqName
    }
}

private object PathCollectionExternalizer :
    CollectionExternalizer<String>(PathStringDescriptor, { THashSet(CollectionFactory.createFilePathSet()) })

sealed class ChangeInfo(val fqName: FqName) {
    open class MembersChanged(fqName: FqName, val names: Collection<String>) : ChangeInfo(fqName) {
        override fun toStringProperties(): String = super.toStringProperties() + ", names = $names"
    }

    class Removed(fqName: FqName, names: Collection<String>) : MembersChanged(fqName, names)

    class SignatureChanged(fqName: FqName, val areSubclassesAffected: Boolean) : ChangeInfo(fqName)

    class ParentsChanged(fqName: FqName, val parentsChanged: Collection<FqName>) : ChangeInfo(fqName)

    protected open fun toStringProperties(): String = "fqName = $fqName"

    override fun toString(): String {
        return this::class.java.simpleName + "(${toStringProperties()})"
    }
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
