/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.DifferenceCalculatorForPackageFacade.Companion.getNonPrivateMembers
import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.incremental.PackagePartProtoData
import org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.incremental.md5
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

    fun snapshot(
        classpathEntry: File,
        granularity: ClassSnapshotGranularity,
        metrics: BuildMetricsReporter = DoNothingBuildMetricsReporter
    ): ClasspathEntrySnapshot {
        val classes = metrics.measure(BuildTime.LOAD_CLASSES) {
            DirectoryOrJarContentsReader
                .read(classpathEntry, DEFAULT_CLASS_FILTER)
                .map { (unixStyleRelativePath, contents) ->
                    ClassFileWithContents(ClassFile(classpathEntry, unixStyleRelativePath), contents)
                }
        }

        val snapshots = metrics.measure(BuildTime.SNAPSHOT_CLASSES) {
            ClassSnapshotter.snapshot(classes, granularity, metrics = metrics)
        }

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
        metrics: BuildMetricsReporter = DoNothingBuildMetricsReporter
    ): List<ClassSnapshot> {
        val classesInfo: List<BasicClassInfo> = metrics.measure(BuildTime.READ_CLASSES_BASIC_INFO) {
            classes.map { it.classInfo }
        }
        val inaccessibleClassesInfo: Set<BasicClassInfo> = metrics.measure(BuildTime.FIND_INACCESSIBLE_CLASSES) {
            findInaccessibleClasses(classesInfo)
        }
        return classes.map {
            when {
                it.classInfo in inaccessibleClassesInfo -> InaccessibleClassSnapshot
                it.classInfo.isKotlinClass -> metrics.measure(BuildTime.SNAPSHOT_KOTLIN_CLASSES) {
                    snapshotKotlinClass(it, granularity)
                }
                else -> metrics.measure(BuildTime.SNAPSHOT_JAVA_CLASSES) {
                    JavaClassSnapshotter.snapshot(it, granularity)
                }
            }
        }
    }

    /** Creates [KotlinClassSnapshot] of the given Kotlin class (the caller must ensure that the given class is a Kotlin class). */
    private fun snapshotKotlinClass(classFile: ClassFileWithContents, granularity: ClassSnapshotGranularity): KotlinClassSnapshot {
        val kotlinClassInfo =
            KotlinClassInfo.createFrom(classFile.classInfo.classId, classFile.classInfo.kotlinClassHeader!!, classFile.contents)
        val classId = kotlinClassInfo.classId
        val classAbiHash = KotlinClassInfoExternalizer.toByteArray(kotlinClassInfo).hashToLong()
        val classMemberLevelSnapshot = kotlinClassInfo.takeIf { granularity == CLASS_MEMBER_LEVEL }

        return when (kotlinClassInfo.classKind) {
            CLASS -> RegularKotlinClassSnapshot(
                classId, classAbiHash, classMemberLevelSnapshot,
                supertypes = classFile.classInfo.supertypes,
                companionObjectName = kotlinClassInfo.companionObject?.shortClassName?.identifier,
                constantsInCompanionObject = kotlinClassInfo.constantsInCompanionObject
            )
            FILE_FACADE, MULTIFILE_CLASS_PART -> PackageFacadeKotlinClassSnapshot(
                classId, classAbiHash, classMemberLevelSnapshot,
                packageMemberNames = (kotlinClassInfo.protoData as PackagePartProtoData).getNonPrivateMembers().toSet()
            )
            MULTIFILE_CLASS -> MultifileClassKotlinClassSnapshot(
                classId, classAbiHash, classMemberLevelSnapshot,
                constantNames = kotlinClassInfo.constantsMap.keys
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
    private fun findInaccessibleClasses(classesInfo: List<BasicClassInfo>): Set<BasicClassInfo> {
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

        return classesInfo.filterTo(mutableSetOf()) { it.isTransitivelyInaccessible() }
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
                    relativePathsToContents.getOrPut(unixStyleRelativePath) { zipInputStream.readBytes() }
                }
            }
        }
        return relativePathsToContents.toSortedMap().toMap(LinkedHashMap())
    }
}

internal fun ByteArray.hashToLong(): Long {
    // Note: md5 is 128-bit while Long is 64-bit.
    // Use md5 for now until we find a better 64-bit hash function.
    return md5()
}
