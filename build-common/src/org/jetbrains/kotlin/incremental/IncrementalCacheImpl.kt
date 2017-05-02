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
import com.intellij.util.SmartList
import com.intellij.util.io.BooleanDataDescriptor
import com.intellij.util.io.EnumeratorStringDescriptor
import gnu.trove.THashSet
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.incremental.ChangeInfo.MembersChanged
import org.jetbrains.kotlin.incremental.ChangeInfo.Removed
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.inline.inlineFunctionsJvmNames
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.deserialization.supertypes
import org.jetbrains.kotlin.serialization.jvm.BitEncoding
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.security.MessageDigest
import java.util.*

val KOTLIN_CACHE_DIRECTORY_NAME = "kotlin"

open class IncrementalCacheImpl<Target>(
        private val targetDataRoot: File,
        targetOutputDir: File?,
        target: Target
) : BasicMapsOwner(), IncrementalCache {
    companion object {
        private val PROTO_MAP = "proto"
        private val CONSTANTS_MAP = "constants"
        private val PACKAGE_PARTS = "package-parts"
        private val MULTIFILE_CLASS_FACADES = "multifile-class-facades"
        private val MULTIFILE_CLASS_PARTS = "multifile-class-parts"
        private val SOURCE_TO_CLASSES = "source-to-classes"
        private val DIRTY_OUTPUT_CLASSES = "dirty-output-classes"
        private val INLINE_FUNCTIONS = "inline-functions"
        private val SUBTYPES = "subtypes"
        private val SUPERTYPES = "supertypes"
        private val CLASS_FQ_NAME_TO_SOURCE = "class-fq-name-to-source"
        private val INTERNAL_NAME_TO_SOURCE = "internal-name-to-source"

        private val MODULE_MAPPING_FILE_NAME = "." + ModuleMapping.MAPPING_FILE_EXT
    }

    private val baseDir = File(targetDataRoot, KOTLIN_CACHE_DIRECTORY_NAME)

    protected val String.storageFile: File
        get() = File(baseDir, this + "." + CACHE_EXTENSION)

    private val protoMap = registerMap(ProtoMap(PROTO_MAP.storageFile))
    private val constantsMap = registerMap(ConstantsMap(CONSTANTS_MAP.storageFile))
    private val packagePartMap = registerMap(PackagePartMap(PACKAGE_PARTS.storageFile))
    private val multifileFacadeToParts = registerMap(MultifileClassFacadeMap(MULTIFILE_CLASS_FACADES.storageFile))
    private val partToMultifileFacade = registerMap(MultifileClassPartMap(MULTIFILE_CLASS_PARTS.storageFile))
    private val sourceToClassesMap = registerMap(SourceToClassesMap(SOURCE_TO_CLASSES.storageFile))
    private val dirtyOutputClassesMap = registerMap(DirtyOutputClassesMap(DIRTY_OUTPUT_CLASSES.storageFile))
    private val inlineFunctionsMap = registerMap(InlineFunctionsMap(INLINE_FUNCTIONS.storageFile))
    private val subtypesMap = registerMap(SubtypesMap(SUBTYPES.storageFile))
    private val supertypesMap = registerMap(SupertypesMap(SUPERTYPES.storageFile))
    private val classFqNameToSourceMap = registerMap(ClassFqNameToSourceMap(CLASS_FQ_NAME_TO_SOURCE.storageFile))
    // todo: try to use internal names only?
    private val internalNameToSource = registerMap(InternalNameToSourcesMap(INTERNAL_NAME_TO_SOURCE.storageFile))

    private val dependents = arrayListOf<IncrementalCacheImpl<Target>>()
    private val outputDir by lazy(LazyThreadSafetyMode.NONE) { requireNotNull(targetOutputDir) { "Target is expected to have output directory: $target" } }

    val thisWithDependentCaches: Iterable<IncrementalCacheImpl<Target>> by lazy {
        val result = arrayListOf(this)
        result.addAll(dependents)
        result
    }

    protected open fun debugLog(message: String) {}

    fun addDependentCache(cache: IncrementalCacheImpl<Target>) {
        dependents.add(cache)
    }

    fun markOutputClassesDirty(removedAndCompiledSources: List<File>) {
        for (sourceFile in removedAndCompiledSources) {
            val classes = sourceToClassesMap[sourceFile]
            classes.forEach {
                dirtyOutputClassesMap.markDirty(it.internalName)
            }

            sourceToClassesMap.clearOutputsForSource(sourceFile)
        }
    }

    // used in gradle
    @Suppress("unused")
    fun classesBySources(sources: Iterable<File>): Iterable<JvmClassName> =
            sources.flatMap { sourceToClassesMap[it] }

    fun getSubtypesOf(className: FqName): Sequence<FqName> =
            subtypesMap[className].asSequence()

    fun getSourceFileIfClass(fqName: FqName): File? =
            classFqNameToSourceMap[fqName]

    fun sourcesByInternalName(internalName: String): Collection<File> =
            internalNameToSource[internalName]

    fun isMultifileFacade(className: JvmClassName): Boolean =
            className.internalName in multifileFacadeToParts

    override fun getClassFilePath(internalClassName: String): String {
        return toSystemIndependentName(File(outputDir, "$internalClassName.class").canonicalPath)
    }

    fun saveModuleMappingToCache(sourceFiles: Collection<File>, file: File): CompilationResult {
        val jvmClassName = JvmClassName.byInternalName(MODULE_MAPPING_FILE_NAME)
        protoMap.storeModuleMapping(jvmClassName, file.readBytes())
        dirtyOutputClassesMap.notDirty(MODULE_MAPPING_FILE_NAME)
        sourceFiles.forEach { sourceToClassesMap.add(it, jvmClassName) }
        return CompilationResult.NO_CHANGES
    }

    open fun saveFileToCache(generatedClass: GeneratedJvmClass<Target>): CompilationResult {
        val sourceFiles: Collection<File> = generatedClass.sourceFiles
        val kotlinClass: LocalFileKotlinClass = generatedClass.outputClass
        val className = kotlinClass.className

        dirtyOutputClassesMap.notDirty(className.internalName)
        sourceFiles.forEach {
            sourceToClassesMap.add(it, className)
        }

        if (IncrementalCompilation.isEnabled()) {
            internalNameToSource[className.internalName] = sourceFiles
        }

        if (kotlinClass.classId.isLocal) {
            return CompilationResult.NO_CHANGES
        }

        val header = kotlinClass.classHeader
        val changesInfo = when (header.kind) {
            KotlinClassHeader.Kind.FILE_FACADE -> {
                assert(sourceFiles.size == 1) { "Package part from several source files: $sourceFiles" }
                packagePartMap.addPackagePart(className)

                protoMap.process(kotlinClass, isPackage = true) +
                constantsMap.process(kotlinClass, isPackage = true) +
                inlineFunctionsMap.process(kotlinClass, isPackage = true)
            }
            KotlinClassHeader.Kind.MULTIFILE_CLASS -> {
                val partNames = kotlinClass.classHeader.data?.toList()
                                ?: throw AssertionError("Multifile class has no parts: ${kotlinClass.className}")
                multifileFacadeToParts[className] = partNames
                // When a class is replaced with a facade with the same name,
                // the class' proto wouldn't ever be deleted,
                // because we don't write proto for multifile facades.
                // As a workaround we can remove proto values for multifile facades.
                val additionalChangeInfo = if (className in protoMap) {
                    val info = ChangeInfo.SignatureChanged(className.fqNameForClassNameWithoutDollars, areSubclassesAffected = true)
                    CompilationResult(changes = sequenceOf(info))
                }
                else CompilationResult.NO_CHANGES
                protoMap.remove(className)
                classFqNameToSourceMap.remove(className.fqNameForClassNameWithoutDollars)
                internalNameToSource.remove(className.internalName)

                // TODO NO_CHANGES? (delegates only)
                constantsMap.process(kotlinClass, isPackage = true) +
                inlineFunctionsMap.process(kotlinClass, isPackage = true) +
                additionalChangeInfo
            }
            KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
                assert(sourceFiles.size == 1) { "Multifile class part from several source files: $sourceFiles" }
                packagePartMap.addPackagePart(className)
                partToMultifileFacade.set(className.internalName, header.multifileClassName!!)

                protoMap.process(kotlinClass, isPackage = true) +
                constantsMap.process(kotlinClass, isPackage = true) +
                inlineFunctionsMap.process(kotlinClass, isPackage = true)
            }
            KotlinClassHeader.Kind.CLASS -> {
                assert(sourceFiles.size == 1) { "Class is expected to have only one source file: $sourceFiles" }
                addToClassStorage(kotlinClass, sourceFiles.first())

                protoMap.process(kotlinClass, isPackage = false) +
                constantsMap.process(kotlinClass, isPackage = false) +
                inlineFunctionsMap.process(kotlinClass, isPackage = false)
            }
            else -> CompilationResult.NO_CHANGES
        }

        changesInfo.logIfSomethingChanged(className)
        return changesInfo
    }

    private fun CompilationResult.logIfSomethingChanged(className: JvmClassName) {
        if (this == CompilationResult.NO_CHANGES) return

        debugLog("$className is changed: $this")
    }

    private fun computeChanges(className: JvmClassName, createChangeInfo: (FqName, Collection<String>) -> ChangeInfo): List<ChangeInfo> {
        fun <T> T.getNonPrivateNames(nameResolver: NameResolver, vararg members: T.() -> List<MessageLite>): Set<String> =
                members.flatMap { this.it().filterNot { it.isPrivate }.names(nameResolver) }.toSet()

        if (className.internalName == MODULE_MAPPING_FILE_NAME) return emptyList()

        val mapValue = protoMap[className] ?: return emptyList()

        return when {
            mapValue.isPackageFacade -> {
                val packageData = JvmProtoBufUtil.readPackageDataFrom(mapValue.bytes, mapValue.strings)

                val memberNames =
                        packageData.packageProto.getNonPrivateNames(
                                packageData.nameResolver,
                                ProtoBuf.Package::getFunctionList,
                                ProtoBuf.Package::getPropertyList
                        )

                listOf(createChangeInfo(className.packageFqName, memberNames))
            }
            else -> {
                val classData = JvmProtoBufUtil.readClassDataFrom(mapValue.bytes, mapValue.strings)

                val classFqName = className.fqNameForClassNameWithoutDollars
                val kind = Flags.CLASS_KIND.get(classData.classProto.flags)

                if (kind == ProtoBuf.Class.Kind.COMPANION_OBJECT) {
                    val memberNames =
                            classData.classProto.getNonPrivateNames(
                                    classData.nameResolver,
                                    ProtoBuf.Class::getConstructorList,
                                    ProtoBuf.Class::getFunctionList,
                                    ProtoBuf.Class::getPropertyList
                            ) + classData.classProto.enumEntryList.map { classData.nameResolver.getString(it.name) }

                    val companionObjectChanged = createChangeInfo(classFqName.parent(), listOfNotNull(classFqName.shortName().asString()))
                    val companionObjectMembersChanged = createChangeInfo(classFqName, memberNames)

                    listOf(companionObjectMembersChanged, companionObjectChanged)
                }
                else {
                    listOf(ChangeInfo.SignatureChanged(classFqName, areSubclassesAffected = true))
                }
            }
        }
    }

    fun clearCacheForRemovedClasses(): CompilationResult {
        val dirtyClasses = dirtyOutputClassesMap
                                .getDirtyOutputClasses()
                                .map(JvmClassName::byInternalName)
                                .toList()

        val changes =
                if (IncrementalCompilation.isEnabled())
                    dirtyClasses.flatMap { computeChanges(it, ::Removed) }.asSequence()
                else
                    emptySequence<ChangeInfo>()

        val changesInfo = CompilationResult(changes = changes)

        val facadesWithRemovedParts = hashMapOf<JvmClassName, MutableSet<String>>()
        for (dirtyClass in dirtyClasses) {
            val facade = partToMultifileFacade.get(dirtyClass.internalName) ?: continue
            val facadeClassName = JvmClassName.byInternalName(facade)
            val removedParts = facadesWithRemovedParts.getOrPut(facadeClassName) { hashSetOf() }
            removedParts.add(dirtyClass.internalName)
        }

        for ((facade, removedParts) in facadesWithRemovedParts.entries) {
            val allParts = multifileFacadeToParts[facade.internalName] ?: continue
            val notRemovedParts = allParts.filter { it !in removedParts }

            if (notRemovedParts.isEmpty()) {
                multifileFacadeToParts.remove(facade)
            }
            else {
                multifileFacadeToParts[facade] = notRemovedParts
            }
        }

        dirtyClasses.forEach {
            protoMap.remove(it)
            packagePartMap.remove(it)
            multifileFacadeToParts.remove(it)
            partToMultifileFacade.remove(it)
            constantsMap.remove(it)
            inlineFunctionsMap.remove(it)
            internalNameToSource.remove(it.internalName)
        }

        removeAllFromClassStorage(dirtyClasses)

        dirtyOutputClassesMap.clean()
        return changesInfo
    }

    override fun getObsoletePackageParts(): Collection<String> {
        val obsoletePackageParts =
                dirtyOutputClassesMap.getDirtyOutputClasses().filter { packagePartMap.isPackagePart(JvmClassName.byInternalName(it)) }
        debugLog("Obsolete package parts: $obsoletePackageParts")
        return obsoletePackageParts
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
        val partNames = multifileFacadeToParts.get(facadeInternalName) ?: return null
        return partNames.filter { !dirtyOutputClassesMap.isDirty(it) }
    }

    override fun getModuleMappingData(): ByteArray? {
        return protoMap[JvmClassName.byInternalName(MODULE_MAPPING_FILE_NAME)]?.bytes
    }

    override fun clean() {
        super.clean()
        normalCacheVersion(targetDataRoot).clean()
    }

    private inner class ProtoMap(storageFile: File) : BasicStringMap<ProtoMapValue>(storageFile, ProtoMapValueExternalizer) {

        fun process(kotlinClass: LocalFileKotlinClass, isPackage: Boolean): CompilationResult {
            val header = kotlinClass.classHeader
            val bytes = BitEncoding.decodeBytes(header.data!!)
            return put(kotlinClass.className, bytes, header.strings!!, isPackage)
        }

        // A module mapping (.kotlin_module file) is stored in a cache,
        // because a corresponding file will be deleted on each round
        // (it is reported as output for each [package part?] source file).
        // If a mapping is not preserved, a resulting file will only contain data
        // from files compiled during last round.
        // However there is no need to compare old and new data in this case
        // (also that would fail with exception).
        fun storeModuleMapping(className: JvmClassName, bytes: ByteArray): CompilationResult {
            storage[className.internalName] = ProtoMapValue(isPackageFacade = false, bytes = bytes, strings = emptyArray())
            return CompilationResult()
        }

        private fun put(
                className: JvmClassName,
                bytes: ByteArray,
                strings: Array<String>,
                isPackage: Boolean
        ): CompilationResult {
            val key = className.internalName
            val oldData = storage[key]
            val data = ProtoMapValue(isPackage, bytes, strings)

            if (oldData == null ||
                !Arrays.equals(bytes, oldData.bytes) ||
                !Arrays.equals(strings, oldData.strings) ||
                isPackage != oldData.isPackageFacade
            ) {
                storage[key] = data
            }

            if (oldData == null) {
                val changes =
                        if (IncrementalCompilation.isEnabled())
                            computeChanges(className, ::MembersChanged).asSequence()
                        else
                            emptySequence<ChangeInfo>()

                return CompilationResult(changes = changes)
            }

            val difference = difference(oldData, data)
            val fqName = if (isPackage) className.packageFqName else className.fqNameForClassNameWithoutDollars
            val changeList = SmartList<ChangeInfo>()

            if (difference.isClassAffected) {
                changeList.add(ChangeInfo.SignatureChanged(fqName, difference.areSubclassesAffected))
            }

            if (difference.changedMembersNames.isNotEmpty()) {
                changeList.add(ChangeInfo.MembersChanged(fqName, difference.changedMembersNames))
            }

            return CompilationResult(changes = changeList.asSequence())
        }

        operator fun contains(className: JvmClassName): Boolean =
                className.internalName in storage

        operator fun get(className: JvmClassName): ProtoMapValue? =
                storage[className.internalName]

        fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: ProtoMapValue): String {
            return (if (value.isPackageFacade) "1" else "0") + java.lang.Long.toHexString(value.bytes.md5())
        }
    }

    private inner class ConstantsMap(storageFile: File) : BasicStringMap<Map<String, Any>>(storageFile, ConstantsMapExternalizer) {
        private fun getConstantsMap(bytes: ByteArray): Map<String, Any>? {
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

            return if (result.isEmpty()) null else result
        }

        operator fun contains(className: JvmClassName): Boolean =
                className.internalName in storage

        fun process(kotlinClass: LocalFileKotlinClass, isPackage: Boolean): CompilationResult {
            return put(kotlinClass.className, getConstantsMap(kotlinClass.fileContents), isPackage)
        }

        private fun put(className: JvmClassName, constantsMap: Map<String, Any>?, isPackage: Boolean): CompilationResult {
            val key = className.internalName

            val oldMap = storage[key]
            if (oldMap == constantsMap) return CompilationResult.NO_CHANGES

            if (constantsMap != null) {
                storage[key] = constantsMap
            }
            else {
                remove(className)
            }

            val changes =
                    if (!IncrementalCompilation.isEnabled() ||
                        constantsMap == null || constantsMap.isEmpty() ||
                        oldMap == null || oldMap.isEmpty()
                    ) {
                        emptySequence<ChangeInfo>()
                    }
                    else {
                        // we need only changed constants everything other should be covered by diff
                        val changedNames = oldMap.filter { constantsMap.containsKey(it.key) && constantsMap[it.key] != it.value }.map { it.key }

                        val fqName = if (isPackage) className.packageFqName else className.fqNameForClassNameWithoutDollars

                        sequenceOf(ChangeInfo.MembersChanged(fqName, changedNames))
                    }

            return CompilationResult(changes = changes)
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

    private inner class MultifileClassFacadeMap(storageFile: File) : BasicStringMap<Collection<String>>(storageFile, StringCollectionExternalizer) {
        operator fun set(facadeName: JvmClassName, partNames: Collection<String>) {
            storage[facadeName.internalName] = partNames
        }

        operator fun get(internalName: String): Collection<String>? = storage[internalName]

        operator fun contains(internalName: String): Boolean = internalName in storage

        fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: Collection<String>): String = value.dumpCollection()
    }

    private inner class MultifileClassPartMap(storageFile: File) : BasicStringMap<String>(storageFile, EnumeratorStringDescriptor.INSTANCE) {
        fun set(partName: String, facadeName: String) {
            storage[partName] = facadeName
        }

        fun get(partName: String): String? {
            return storage.get(partName)
        }

        fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: String): String = value
    }

    inner class SourceToClassesMap(storageFile: File) : BasicStringMap<Collection<String>>(storageFile, PathStringDescriptor, StringCollectionExternalizer) {
        fun clearOutputsForSource(sourceFile: File) {
            remove(sourceFile.absolutePath)
        }

        fun add(sourceFile: File, className: JvmClassName) {
            storage.append(sourceFile.absolutePath, className.internalName)
        }

        operator fun get(sourceFile: File): Collection<JvmClassName> =
                storage[sourceFile.absolutePath].orEmpty().map { JvmClassName.byInternalName(it) }

        override fun dumpValue(value: Collection<String>) = value.dumpCollection()

        private fun remove(path: String) {
            storage.remove(path)
        }
    }

    inner class ClassFqNameToSourceMap(storageFile: File) : BasicStringMap<String>(storageFile, EnumeratorStringDescriptor(), PathStringDescriptor) {
        operator fun set(fqName: FqName, sourceFile: File) {
            storage[fqName.asString()] = sourceFile.canonicalPath
        }

        operator fun get(fqName: FqName): File? =
                storage[fqName.asString()]?.let(::File)

        fun remove(fqName: FqName) {
            storage.remove(fqName.asString())
        }

        override fun dumpValue(value: String) = value
    }


    inner class InternalNameToSourcesMap(storageFile: File) : BasicStringMap<Collection<String>>(storageFile, EnumeratorStringDescriptor(), PathCollectionExternalizer) {
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
        if (!IncrementalCompilation.isEnabled()) return

        val classData = JvmProtoBufUtil.readClassDataFrom(kotlinClass.classHeader.data!!, kotlinClass.classHeader.strings!!)
        val supertypes = classData.classProto.supertypes(TypeTable(classData.classProto.typeTable))
        val parents = supertypes.map { classData.nameResolver.getClassId(it.className).asSingleFqName() }
                                .filter { it.asString() != "kotlin.Any" }
                                .toSet()
        val child = kotlinClass.classId.asSingleFqName()

        parents.forEach { subtypesMap.add(it, child) }

        val removedSupertypes = supertypesMap[child].filter { it !in parents }
        removedSupertypes.forEach { subtypesMap.removeValues(it, setOf(child)) }

        supertypesMap[child] = parents
        classFqNameToSourceMap[kotlinClass.className.fqNameForClassNameWithoutDollars] = srcFile
    }

    private fun removeAllFromClassStorage(removedClasses: Collection<JvmClassName>) {
        if (!IncrementalCompilation.isEnabled() || removedClasses.isEmpty()) return

        val removedFqNames = removedClasses.map { it.fqNameForClassNameWithoutDollars }.toSet()

        for (cache in thisWithDependentCaches) {
            val parentsFqNames = hashSetOf<FqName>()
            val childrenFqNames = hashSetOf<FqName>()

            for (removedFqName in removedFqNames) {
                parentsFqNames.addAll(cache.supertypesMap[removedFqName])
                childrenFqNames.addAll(cache.subtypesMap[removedFqName])

                cache.supertypesMap.remove(removedFqName)
                cache.subtypesMap.remove(removedFqName)
            }

            for (child in childrenFqNames) {
                cache.supertypesMap.removeValues(child, removedFqNames)
            }

            for (parent in parentsFqNames) {
                cache.subtypesMap.removeValues(parent, removedFqNames)
            }
        }

        removedFqNames.forEach { classFqNameToSourceMap.remove(it) }
    }

    private inner class DirtyOutputClassesMap(storageFile: File) : BasicStringMap<Boolean>(storageFile, BooleanDataDescriptor.INSTANCE) {
        fun markDirty(className: String) {
            storage[className] = true
        }

        fun notDirty(className: String) {
            storage.remove(className)
        }

        fun getDirtyOutputClasses(): Collection<String> =
                storage.keys

        fun isDirty(className: String): Boolean =
                storage.contains(className)

        override fun dumpValue(value: Boolean) = ""
    }

    private inner class InlineFunctionsMap(storageFile: File) : BasicStringMap<Map<String, Long>>(storageFile, StringToLongMapExternalizer) {
        private fun getInlineFunctionsMap(header: KotlinClassHeader, bytes: ByteArray): Map<String, Long> {
            val inlineFunctions = inlineFunctionsJvmNames(header)
            if (inlineFunctions.isEmpty()) return emptyMap()

            val result = HashMap<String, Long>()

            ClassReader(bytes).accept(object : ClassVisitor(Opcodes.ASM5) {
                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
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

        fun process(kotlinClass: LocalFileKotlinClass, isPackage: Boolean): CompilationResult {
            return put(kotlinClass.className, getInlineFunctionsMap(kotlinClass.classHeader, kotlinClass.fileContents), isPackage)
        }

        private fun put(className: JvmClassName, newMap: Map<String, Long>, isPackage: Boolean): CompilationResult {
            val internalName = className.internalName
            val oldMap = storage[internalName] ?: emptyMap()

            val added = hashSetOf<String>()
            val changed = hashSetOf<String>()
            val allFunctions = oldMap.keys + newMap.keys

            for (fn in allFunctions) {
                val oldHash = oldMap[fn]
                val newHash = newMap[fn]

                when {
                    oldHash == null -> added.add(fn)
                    oldHash != newHash -> changed.add(fn)
                }
            }

            when {
                newMap.isNotEmpty() -> storage[internalName] = newMap
                else -> storage.remove(internalName)
            }

            val changes =
                    if (IncrementalCompilation.isEnabled()) {
                        val fqName = if (isPackage) className.packageFqName else className.fqNameForClassNameWithoutDollars
                        // TODO get name in better way instead of using substringBefore
                        (added.asSequence() + changed.asSequence()).map { ChangeInfo.MembersChanged(fqName, listOf(it.substringBefore("("))) }
                    }
                    else {
                        emptySequence<ChangeInfo>()
                    }

            return CompilationResult(changes = changes)
        }

        fun remove(className: JvmClassName) {
            storage.remove(className.internalName)
        }

        override fun dumpValue(value: Map<String, Long>): String =
                value.dumpMap { java.lang.Long.toHexString(it) }
    }
}

private object PathCollectionExternalizer : CollectionExternalizer<String>(PathStringDescriptor, { THashSet(FileUtil.PATH_HASHING_STRATEGY) })

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

data class CompilationResult(
        val changes: Sequence<ChangeInfo> = emptySequence()
) {
    companion object {
        val NO_CHANGES: CompilationResult = CompilationResult()
    }

    operator fun plus(other: CompilationResult): CompilationResult =
            CompilationResult(changes + other.changes)
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
fun <K : Comparable<K>, V> Map<K, V>.dumpMap(dumpValue: (V)->String): String =
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

@TestOnly fun <T : Comparable<T>> Collection<T>.dumpCollection(): String =
        "[${sorted().joinToString(", ", transform = Any::toString)}]"
