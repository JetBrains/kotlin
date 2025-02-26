/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.incremental.classpathDiff.impl.*
import org.jetbrains.kotlin.konan.file.use
import java.io.Closeable
import java.io.File
import java.util.zip.ZipFile


/**
 * Computes a [ClasspathEntrySnapshot] of a classpath entry (directory or jar).
 *
 * It is relatively high up in the chain of snapshotting:
 * Classpath -> ClasspathEntry -> ClassList -> KotlinClass/JavaClass
 **/
object ClasspathEntrySnapshotter {

    data class Settings(
        val granularity: ClassSnapshotGranularity,
        val parseInlinedLocalClasses: Boolean
    )

    private val DEFAULT_CLASS_FILTER = { unixStyleRelativePath: String, isDirectory: Boolean ->
        !isDirectory
                && unixStyleRelativePath.endsWith(".class", ignoreCase = true)
                && !unixStyleRelativePath.equals("module-info.class", ignoreCase = true)
                && !unixStyleRelativePath.startsWith("meta-inf/", ignoreCase = true)
    }

    fun snapshot(
        classpathEntry: File,
        settings: Settings,
        metrics: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric> = DoNothingBuildMetricsReporter
    ): ClasspathEntrySnapshot {
        DirectoryOrJarReader.create(classpathEntry).use { directoryOrJarReader ->
            val classes = metrics.measure(GradleBuildTime.LOAD_CLASSES_PATHS_ONLY) {
                directoryOrJarReader.getUnixStyleRelativePaths(DEFAULT_CLASS_FILTER).map { unixStyleRelativePath ->
                    ClassFileWithContentsProvider(
                        classFile = ClassFile(classpathEntry, unixStyleRelativePath),
                        contentsProvider = { directoryOrJarReader.readBytes(unixStyleRelativePath) }
                    )
                }
            }
            val snapshots = metrics.measure(GradleBuildTime.SNAPSHOT_CLASSES) {
                val classListSnapshotter: ClassListSnapshotter = if (settings.parseInlinedLocalClasses) {
                    ClassListSnapshotterWithInlinedClassSupport(classes, settings, metrics)
                } else {
                    PlainClassListSnapshotter(classes, settings, metrics)
                }
                classListSnapshotter.snapshot()
            }
            return ClasspathEntrySnapshot(
                classSnapshots = classes.map { it.classFile.unixStyleRelativePath }.zip(snapshots).toMap(LinkedHashMap())
            )
        }
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
