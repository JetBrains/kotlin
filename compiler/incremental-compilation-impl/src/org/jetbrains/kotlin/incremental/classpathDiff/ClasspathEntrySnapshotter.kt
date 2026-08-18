/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.incremental.classpathDiff.impl.*
import java.io.Closeable
import java.io.File
import java.util.zip.ZipEntry
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
        val parseInlinedLocalClasses: Boolean,
        val expandTypeAliases: Boolean,
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
        metrics: BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric> = DoNothingBuildMetricsReporter
    ): ClasspathEntrySnapshot {
        DirectoryOrJarReader.create(classpathEntry).use { directoryOrJarReader ->
            val classes = metrics.measure(LOAD_CLASSES_PATHS_ONLY) {
                directoryOrJarReader.getClasses(classpathEntry, DEFAULT_CLASS_FILTER)
            }
            val snapshots = metrics.measure(SNAPSHOT_CLASSES) {
                val classListSnapshotter: ClassListSnapshotter = if (settings.parseInlinedLocalClasses) {
                    ClassListSnapshotterWithInlinedClassSupport(classes, settings, metrics)
                } else {
                    PlainClassListSnapshotter(classes, settings, metrics)
                }
                classListSnapshotter.snapshot()
            }
            val classSnapshots = LinkedHashMap<String, ClassSnapshot>(mapCapacity(classes.size))
            classes.indices.forEach { index ->
                classSnapshots[classes[index].classFile.unixStyleRelativePath] = snapshots[index]
            }
            return ClasspathEntrySnapshot(classSnapshots)
        }
    }

    private fun mapCapacity(expectedSize: Int): Int = when {
        expectedSize < 3 -> expectedSize + 1
        expectedSize < Int.MAX_VALUE / 2 -> (expectedSize / 0.75f + 1).toInt()
        else -> Int.MAX_VALUE
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
    fun getClasses(
        classpathEntry: File,
        filter: (unixStyleRelativePath: String, isDirectory: Boolean) -> Boolean
    ): List<ClassFileWithContentsProvider>

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

    override fun getClasses(
        classpathEntry: File,
        filter: (unixStyleRelativePath: String, isDirectory: Boolean) -> Boolean
    ): List<ClassFileWithContentsProvider> {
        return directory.walk()
            .mapNotNull { file ->
                val relativePath = file.relativeTo(directory).invariantSeparatorsPath
                if (filter.invoke(relativePath, file.isDirectory)) {
                    ClassFileWithContentsProvider(ClassFile(classpathEntry, relativePath), file::readBytes)
                } else {
                    null
                }
            }
            .sortedBy { it.classFile.unixStyleRelativePath }
            .toList()
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

    override fun getClasses(
        classpathEntry: File,
        filter: (unixStyleRelativePath: String, isDirectory: Boolean) -> Boolean
    ): List<ClassFileWithContentsProvider> {
        val entries = ArrayList<ZipEntry>(zipFile.size())
        val entriesEnumeration = zipFile.entries()
        while (entriesEnumeration.hasMoreElements()) {
            val entry = entriesEnumeration.nextElement()
            if (filter.invoke(entry.name, entry.isDirectory)) {
                entries.add(entry)
            }
        }
        // The sort is stable, so retaining the last duplicate matches ZipFile.getEntry(path) behavior used previously.
        entries.sortBy { it.name }

        return buildList(entries.size) {
            for (index in entries.indices) {
                val entry = entries[index]
                if (index == entries.lastIndex || entry.name != entries[index + 1].name) {
                    add(
                        ClassFileWithContentsProvider(
                            ClassFile(classpathEntry, entry.name),
                            contentsProvider = { readBytes(entry) }
                        )
                    )
                }
            }
        }
    }

    private fun readBytes(entry: ZipEntry): ByteArray = zipFile.getInputStream(entry).use { it.readBytes() }

    override fun close() {
        zipFile.close()
    }
}
