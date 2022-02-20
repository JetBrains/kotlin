/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.ChangesCollector.Companion.getNonPrivateMemberNames
import org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshotGranularity.*
import org.jetbrains.kotlin.incremental.storage.toByteArray
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind.*
import org.jetbrains.kotlin.name.ClassId
import java.io.File
import java.util.zip.ZipInputStream

/** Computes a [ClasspathEntrySnapshot] of a classpath entry (directory or jar). */
object ClasspathEntrySnapshotter {

    private val DEFAULT_CLASS_FILTER = { unixStyleRelativePath: String, isDirectory: Boolean ->
        !isDirectory
                && unixStyleRelativePath.endsWith(".class", ignoreCase = true)
                && !unixStyleRelativePath.startsWith("meta-inf/", ignoreCase = true)
                && !unixStyleRelativePath.equals("module-info.class", ignoreCase = true)
    }

    fun snapshot(classpathEntry: File): ClasspathEntrySnapshot {
        val classes = DirectoryOrJarContentsReader
            .read(classpathEntry, DEFAULT_CLASS_FILTER)
            .map { (unixStyleRelativePath, contents) ->
                ClassFileWithContents(ClassFile(classpathEntry, unixStyleRelativePath), contents)
            }

        val snapshots = ClassSnapshotter.snapshot(classes)

        val relativePathsToSnapshotsMap = classes.map { it.classFile.unixStyleRelativePath }.zip(snapshots).toMap(LinkedHashMap())
        return ClasspathEntrySnapshot(relativePathsToSnapshotsMap)
    }
}

/** Creates [ClassSnapshot]s of classes. */
object ClassSnapshotter {

    /** Creates [ClassSnapshot]s of the given classes. */
    fun snapshot(
        classes: List<ClassFileWithContents>,
        granularity: ClassSnapshotGranularity = CLASS_MEMBER_LEVEL,
        includeDebugInfoInJavaSnapshot: Boolean = false
    ): List<ClassSnapshot> {
        // Find inaccessible classes first
        val classesInfo: List<BasicClassInfo> = classes.map { it.classInfo }
        val inaccessibleClassesInfo: Set<BasicClassInfo> = getInaccessibleClasses(classesInfo).toSet()

        return classes.map {
            when {
                it.classInfo in inaccessibleClassesInfo -> InaccessibleClassSnapshot
                it.classInfo.isKotlinClass -> snapshotKotlinClass(it, granularity)
                else -> JavaClassSnapshotter.snapshot(it, granularity, includeDebugInfoInJavaSnapshot)
            }
        }
    }

    /** Creates [KotlinClassSnapshot] of the given Kotlin class (the caller must ensure that the given class is a Kotlin class). */
    private fun snapshotKotlinClass(classFile: ClassFileWithContents, granularity: ClassSnapshotGranularity): KotlinClassSnapshot {
        val kotlinClassInfo =
            KotlinClassInfo.createFrom(classFile.classInfo.classId, classFile.classInfo.kotlinClassHeader!!, classFile.contents)
        val classId = kotlinClassInfo.classId
        val classAbiHash = KotlinClassInfoExternalizer.toByteArray(kotlinClassInfo).md5()
        val classMemberLevelSnapshot = kotlinClassInfo.takeIf { granularity == CLASS_MEMBER_LEVEL }

        return when (kotlinClassInfo.classKind) {
            CLASS -> RegularKotlinClassSnapshot(classId, classAbiHash, classMemberLevelSnapshot, classFile.classInfo.supertypes)
            FILE_FACADE, MULTIFILE_CLASS_PART -> PackageFacadeKotlinClassSnapshot(
                classId, classAbiHash, classMemberLevelSnapshot,
                packageMembers = PackageMemberSet(
                    mapOf(classId.packageFqName to (kotlinClassInfo.protoData as PackagePartProtoData).getNonPrivateMemberNames())
                )
            )
            MULTIFILE_CLASS -> MultifileClassKotlinClassSnapshot(
                classId, classAbiHash, classMemberLevelSnapshot,
                constants = PackageMemberSet(mapOf(classId.packageFqName to kotlinClassInfo.constantsMap.keys))
            )
            SYNTHETIC_CLASS -> error("Unexpected class $classId with class kind ${SYNTHETIC_CLASS.name} (synthetic classes should have been removed earlier)")
            UNKNOWN -> error("Can't handle class $classId with class kind ${UNKNOWN.name}")
        }
    }

    /**
     * Returns inaccessible classes, i.e. classes that can't be referenced from other source files (and therefore any changes in these
     * classes will not require recompilation of other source files).
     *
     * Examples include private, local, anonymous, and synthetic classes.
     *
     * If a class is inaccessible, its nested classes (if any) are also inaccessible.
     *
     * NOTE: If we do not have enough info to determine whether a class is inaccessible, we will assume that the class is accessible to be
     * safe.
     */
    private fun getInaccessibleClasses(classesInfo: List<BasicClassInfo>): List<BasicClassInfo> {
        fun BasicClassInfo.isInaccessible(): Boolean {
            return if (this.isKotlinClass) {
                when (this.kotlinClassHeader!!.kind) {
                    CLASS -> isPrivate || isLocal || isAnonymous || isSynthetic
                    SYNTHETIC_CLASS -> true
                    // We're not sure about the other kinds of Kotlin classes, so we assume it's accessible (see this method's kdoc)
                    else -> false
                }
            } else {
                isPrivate || isLocal || isAnonymous || isSynthetic
            }
        }

        val classIsInaccessible: MutableMap<BasicClassInfo, Boolean> = HashMap(classesInfo.size)
        val classIdToClassInfo: Map<ClassId, BasicClassInfo> = classesInfo.associateBy { it.classId }

        fun BasicClassInfo.isTransitivelyInaccessible(): Boolean {
            classIsInaccessible[this]?.let { return it }

            val inaccessible = if (isInaccessible()) {
                true
            } else classId.outerClassId?.let { outerClassId ->
                classIdToClassInfo[outerClassId]?.isTransitivelyInaccessible()
                // If we can't find the outer class from the given list of classes (which could happen with faulty jars), we assume that
                // the class is accessible (see this method's kdoc).
                    ?: false
            } ?: false

            return inaccessible.also {
                classIsInaccessible[this] = inaccessible
            }
        }

        return classesInfo.filter { it.isTransitivelyInaccessible() }
    }
}

/** Utility to read the contents of a directory or jar. */
private object DirectoryOrJarContentsReader {

    /**
     * Returns a map from Unix-style relative paths of entries to their contents. The paths are relative to the given container (directory
     * or jar).
     *
     * The map entries need to satisfy the given filter.
     *
     * The map entries are sorted based on their Unix-style relative paths (to ensure deterministic results across filesystems).
     *
     * Note: If a jar has duplicate entries after filtering, only the first one is retained.
     */
    fun read(
        directoryOrJar: File,
        entryFilter: ((unixStyleRelativePath: String, isDirectory: Boolean) -> Boolean)? = null
    ): LinkedHashMap<String, ByteArray> {
        return if (directoryOrJar.isDirectory) {
            readDirectory(directoryOrJar, entryFilter)
        } else {
            check(directoryOrJar.isFile && directoryOrJar.path.endsWith(".jar", ignoreCase = true))
            readJar(directoryOrJar, entryFilter)
        }
    }

    private fun readDirectory(
        directory: File,
        entryFilter: ((unixStyleRelativePath: String, isDirectory: Boolean) -> Boolean)? = null
    ): LinkedHashMap<String, ByteArray> {
        val relativePathsToContents = mutableMapOf<String, ByteArray>()
        directory.walk().forEach { file ->
            val unixStyleRelativePath = file.relativeTo(directory).invariantSeparatorsPath
            if (entryFilter == null || entryFilter(unixStyleRelativePath, file.isDirectory)) {
                relativePathsToContents[unixStyleRelativePath] = file.readBytes()
            }
        }
        return relativePathsToContents.toSortedMap().toMap(LinkedHashMap())
    }

    private fun readJar(
        jarFile: File,
        entryFilter: ((unixStyleRelativePath: String, isDirectory: Boolean) -> Boolean)? = null
    ): LinkedHashMap<String, ByteArray> {
        val relativePathsToContents = mutableMapOf<String, ByteArray>()
        ZipInputStream(jarFile.inputStream().buffered()).use { zipInputStream ->
            while (true) {
                val entry = zipInputStream.nextEntry ?: break
                val unixStyleRelativePath = entry.name
                if (entryFilter == null || entryFilter(unixStyleRelativePath, entry.isDirectory)) {
                    relativePathsToContents.computeIfAbsent(unixStyleRelativePath) { zipInputStream.readBytes() }
                }
            }
        }
        return relativePathsToContents.toSortedMap().toMap(LinkedHashMap())
    }
}
