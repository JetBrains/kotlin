/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.ClassNodeSnapshotter.snapshotClass
import org.jetbrains.kotlin.incremental.ClassNodeSnapshotter.snapshotClassExcludingMembers
import org.jetbrains.kotlin.incremental.ClassNodeSnapshotter.snapshotField
import org.jetbrains.kotlin.incremental.ClassNodeSnapshotter.snapshotMethod
import org.jetbrains.kotlin.incremental.ClassNodeSnapshotter.sortClassMembers
import org.jetbrains.kotlin.incremental.DifferenceCalculatorForPackageFacade.Companion.getNonPrivateMembers
import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.incremental.PackagePartProtoData
import org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.incremental.hashToLong
import org.jetbrains.kotlin.incremental.storage.toByteArray
import org.jetbrains.kotlin.konan.file.use
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind.*
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.io.Closeable
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/** Computes a [ClasspathEntrySnapshot] of a classpath entry (directory or jar). */
object ClasspathEntrySnapshotter {

    private val DEFAULT_CLASS_FILTER = { unixStyleRelativePath: String, isDirectory: Boolean ->
        !isDirectory
                && unixStyleRelativePath.endsWith(".class", ignoreCase = true)
                && !unixStyleRelativePath.equals("module-info.class", ignoreCase = true)
                && !unixStyleRelativePath.startsWith("meta-inf/", ignoreCase = true)
    }

    fun snapshot(
        classpathEntry: File,
        granularity: ClassSnapshotGranularity,
        metrics: BuildMetricsReporter = DoNothingBuildMetricsReporter
    ): ClasspathEntrySnapshot {
        DirectoryOrJarReader.create(classpathEntry).use { directoryOrJarReader ->
            val classes = metrics.measure(BuildTime.LOAD_CLASSES_PATHS_ONLY) {
                directoryOrJarReader.getUnixStyleRelativePaths(DEFAULT_CLASS_FILTER).map { unixStyleRelativePath ->
                    ClassFileWithContentsProvider(
                        classFile = ClassFile(classpathEntry, unixStyleRelativePath),
                        contentsProvider = { directoryOrJarReader.readBytes(unixStyleRelativePath) }
                    )
                }
            }
            val snapshots = metrics.measure(BuildTime.SNAPSHOT_CLASSES) {
                ClassSnapshotter.snapshot(classes, granularity, metrics)
            }
            return ClasspathEntrySnapshot(
                classSnapshots = classes.map { it.classFile.unixStyleRelativePath }.zip(snapshots).toMap(LinkedHashMap())
            )
        }
    }
}

/** Computes [ClassSnapshot]s of classes. */
object ClassSnapshotter {

    fun snapshot(
        classes: List<ClassFileWithContentsProvider>,
        granularity: ClassSnapshotGranularity,
        metrics: BuildMetricsReporter = DoNothingBuildMetricsReporter
    ): List<ClassSnapshot> {
        fun ClassFile.getClassName(): JvmClassName {
            check(unixStyleRelativePath.endsWith(".class", ignoreCase = true))
            return JvmClassName.byInternalName(unixStyleRelativePath.dropLast(".class".length))
        }

        val classNameToClassFileMap: Map<JvmClassName, ClassFileWithContentsProvider> = classes.associateBy { it.classFile.getClassName() }
        val classFileToSnapshotMap = mutableMapOf<ClassFileWithContentsProvider, ClassSnapshot>()

        fun snapshotClass(classFile: ClassFileWithContentsProvider): ClassSnapshot {
            return classFileToSnapshotMap.getOrPut(classFile) {
                val clazz = metrics.measure(BuildTime.LOAD_CONTENTS_OF_CLASSES) {
                    classFile.loadContents()
                }
                // Snapshot outer class first as we need this info to determine whether a class is transitively inaccessible (see below)
                val outerClassSnapshot = clazz.classInfo.classId.outerClassId?.let { outerClassId ->
                    val outerClassFile = classNameToClassFileMap[JvmClassName.byClassId(outerClassId)]
                    // It's possible that the outer class is not found in the given classes (it could happen with faulty jars)
                    outerClassFile?.let { snapshotClass(it) }
                }
                when {
                    // We don't need to snapshot (directly or transitively) inaccessible classes.
                    // A class is transitively inaccessible if its outer class is inaccessible.
                    clazz.classInfo.isInaccessible() || outerClassSnapshot is InaccessibleClassSnapshot -> {
                        InaccessibleClassSnapshot
                    }
                    clazz.classInfo.isKotlinClass -> metrics.measure(BuildTime.SNAPSHOT_KOTLIN_CLASSES) {
                        snapshotKotlinClass(clazz, granularity)
                    }
                    else -> metrics.measure(BuildTime.SNAPSHOT_JAVA_CLASSES) {
                        snapshotJavaClass(clazz, granularity)
                    }
                }
            }
        }

        return classes.map { snapshotClass(it) }
    }

    /**
     * Returns `true` if this class is inaccessible, and `false` otherwise (or if we don't know).
     *
     * A class is inaccessible if it can't be referenced from other source files (and therefore any changes in an inaccessible class will
     * not require recompilation of other source files).
     */
    private fun BasicClassInfo.isInaccessible(): Boolean {
        return when {
            isKotlinClass -> when (kotlinClassHeader!!.kind) {
                CLASS -> isPrivate || isLocal || isAnonymous || isSynthetic
                SYNTHETIC_CLASS -> true
                else -> false // We don't know about the other class kinds
            }
            else -> isPrivate || isLocal || isAnonymous || isSynthetic
        }
    }

    /** Computes a [KotlinClassSnapshot] of the given Kotlin class. */
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
                constantNames = kotlinClassInfo.extraInfo.constantSnapshots.keys
            )
            SYNTHETIC_CLASS -> error("Unexpected class $classId with class kind ${SYNTHETIC_CLASS.name} (synthetic classes should have been removed earlier)")
            UNKNOWN -> error("Can't handle class $classId with class kind ${UNKNOWN.name}")
        }
    }

    /** Computes a [JavaClassSnapshot] of the given Java class. */
    private fun snapshotJavaClass(classFile: ClassFileWithContents, granularity: ClassSnapshotGranularity): JavaClassSnapshot {
        // For incremental compilation, we only care about the ABI info of a class. There are 2 approaches:
        //   1. Collect ABI info directly
        //   2. Remove non-ABI info from the full class
        // Note that for incremental compilation to be correct, all ABI info must be collected exhaustively (now and in the future when
        // there are updates to Java/ASM), whereas it is acceptable if non-ABI info is not removed completely.
        // In the following, we will use the second approach as it is safer and easier.

        val classNode = ClassNode()
        val classReader = ClassReader(classFile.contents)

        // Note the `parsingOptions` passed to `classReader`:
        //   - Pass SKIP_CODE as method bodies are not important
        //   - Do not pass SKIP_DEBUG as debug info (e.g., method parameter names) may be important
        classReader.accept(classNode, ClassReader.SKIP_CODE)
        sortClassMembers(classNode)

        // Remove private fields and methods
        fun Int.isPrivate() = (this and Opcodes.ACC_PRIVATE) != 0
        classNode.fields.removeIf { it.access.isPrivate() }
        classNode.methods.removeIf { it.access.isPrivate() }

        // Snapshot the class
        val classMemberLevelSnapshot = if (granularity == CLASS_MEMBER_LEVEL) {
            JavaClassMemberLevelSnapshot(
                classAbiExcludingMembers = JavaElementSnapshot(classNode.name, snapshotClassExcludingMembers(classNode)),
                fieldsAbi = classNode.fields.map { JavaElementSnapshot(it.name, snapshotField(it)) },
                methodsAbi = classNode.methods.map { JavaElementSnapshot(it.name, snapshotMethod(it, classNode.version)) }
            )
        } else {
            null
        }
        val classAbiHash = if (granularity == CLASS_MEMBER_LEVEL) {
            JavaClassMemberLevelSnapshotExternalizer.toByteArray(classMemberLevelSnapshot!!).hashToLong()
        } else {
            snapshotClass(classNode)
        }

        return JavaClassSnapshot(
            classId = classFile.classInfo.classId,
            classAbiHash = classAbiHash,
            classMemberLevelSnapshot = classMemberLevelSnapshot,
            supertypes = classFile.classInfo.supertypes
        )
    }

}

private sealed interface DirectoryOrJarReader : Closeable {

    /**
     * Returns the Unix-style relative paths of all entries under the containing directory or jar which satisfy the given [filter].
     *
     * The paths are in Unix style and are sorted to ensure deterministic results across platforms.
     *
     * If a jar has duplicate entries, only unique paths are kept in the returned list (similar to the way the compiler selects the first
     * class if the classpath has duplicate classes).
     */
    fun getUnixStyleRelativePaths(filter: (unixStyleRelativePath: String, isDirectory: Boolean) -> Boolean): List<String>

    fun readBytes(unixStyleRelativePath: String): ByteArray

    companion object {

        fun create(directoryOrJar: File): DirectoryOrJarReader {
            return if (directoryOrJar.isDirectory) {
                DirectoryReader(directoryOrJar)
            } else {
                check(directoryOrJar.isFile && directoryOrJar.path.endsWith(".jar", ignoreCase = true))
                JarReader(directoryOrJar)
            }
        }
    }
}

private class DirectoryReader(private val directory: File) : DirectoryOrJarReader {

    override fun getUnixStyleRelativePaths(filter: (unixStyleRelativePath: String, isDirectory: Boolean) -> Boolean): List<String> {
        return directory.walk()
            .filter { filter.invoke(it.relativeTo(directory).invariantSeparatorsPath, it.isDirectory) }
            .map { it.relativeTo(directory).invariantSeparatorsPath }
            .sorted()
            .toList()
    }

    override fun readBytes(unixStyleRelativePath: String): ByteArray {
        return directory.resolve(unixStyleRelativePath).readBytes()
    }

    override fun close() {
        // Do nothing
    }
}

private class JarReader(jar: File) : DirectoryOrJarReader {

    // Use `java.util.zip.ZipFile` API to read jars (it matches what the compiler is using).
    // Note: Using `java.util.zip.ZipInputStream` API is slightly faster, but (1) it may fail on certain jars (e.g., KT-57767), and (2) it
    // doesn't support non-sequential access of the entries, so we would have to load and index all entries in memory to provide
    // non-sequential access, thereby increasing memory usage (KT-57757).
    // Another option is to use `java.nio.file.FileSystem` API, but it seems to be slower than the other two.
    private val zipFile = ZipFile(jar)

    override fun getUnixStyleRelativePaths(filter: (unixStyleRelativePath: String, isDirectory: Boolean) -> Boolean): List<String> {
        return zipFile.entries()
            .asSequence()
            .filter { filter.invoke(it.name, it.isDirectory) }
            .mapTo(sortedSetOf()) { it.name } // Map to `Set` to de-duplicate entries
            .toList()
    }

    override fun readBytes(unixStyleRelativePath: String): ByteArray {
        return zipFile.getInputStream(zipFile.getEntry(unixStyleRelativePath)).use {
            it.readBytes()
        }
    }

    override fun close() {
        zipFile.close()
    }
}
