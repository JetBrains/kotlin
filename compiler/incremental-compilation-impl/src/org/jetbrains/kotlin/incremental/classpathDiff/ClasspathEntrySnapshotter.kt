/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import com.google.common.io.ByteStreams
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.cli.jvm.modules.ModuleInfoClassSelection
import org.jetbrains.kotlin.cli.jvm.modules.ModuleInfoClassSelection.MODULE_INFO_CLASS
import org.jetbrains.kotlin.incremental.classpathDiff.impl.*
import org.jetbrains.kotlin.incremental.classpathDiff.impl.SingleClassSnapshotter.snapshotModuleInfoClass
import org.jetbrains.kotlin.name.FqName
import java.io.Closeable
import java.io.File
import java.util.jar.Manifest
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
                && !unixStyleRelativePath.equals(MODULE_INFO_CLASS, ignoreCase = true)
                && !unixStyleRelativePath.startsWith("meta-inf/", ignoreCase = true)
    }

    fun snapshot(
        classpathEntry: File,
        settings: Settings,
        metrics: BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric> = DoNothingBuildMetricsReporter
    ): ClasspathEntrySnapshot {
        DirectoryOrJarReader.create(classpathEntry).use { directoryOrJarReader ->
            val classes = metrics.measure(LOAD_CLASSES_PATHS_ONLY) {
                directoryOrJarReader.getUnixStyleRelativePaths(DEFAULT_CLASS_FILTER).map { unixStyleRelativePath ->
                    ClassFileWithContentsProvider(
                        classFile = ClassFile(classpathEntry, unixStyleRelativePath),
                        contentsProvider = { directoryOrJarReader.readBytes(unixStyleRelativePath) }
                    )
                }
            }
            val snapshots = metrics.measure(SNAPSHOT_CLASSES) {
                val classListSnapshotter: ClassListSnapshotter = if (settings.parseInlinedLocalClasses) {
                    ClassListSnapshotterWithInlinedClassSupport(classes, settings, metrics)
                } else {
                    PlainClassListSnapshotter(classes, settings, metrics)
                }
                classListSnapshotter.snapshot()
            }
            val classSnapshots = classes.map { it.classFile.unixStyleRelativePath }.zip(snapshots).toMap(LinkedHashMap())

            selectModuleInfoPath(directoryOrJarReader)?.let { moduleInfoPath ->
                val packages: Set<FqName> = snapshots.mapNotNullTo(mutableSetOf()) { (it as? AccessibleClassSnapshot)?.classId?.packageFqName }
                snapshotModuleInfoClass(directoryOrJarReader.readBytes(moduleInfoPath), packages)?.let { moduleInfoSnapshot ->
                    classSnapshots[moduleInfoPath] = moduleInfoSnapshot
                }
            }
            return ClasspathEntrySnapshot(classSnapshots)
        }
    }

    /**
     * Selects the `module-info.class` the compiler reads for this classpath entry, see [ModuleInfoClassSelection].
     */
    private fun selectModuleInfoPath(reader: DirectoryOrJarReader): String? {
        val candidates = reader.getUnixStyleRelativePaths { path, isDirectory ->
            !isDirectory && (path == MODULE_INFO_CLASS || path == MANIFEST_PATH || versionDirectoryName(path) != null)
        }
        return ModuleInfoClassSelection.select(
            rootModuleInfo = MODULE_INFO_CLASS.takeIf { it in candidates },
            isJar = reader.isJar,
            isMultiRelease = {
                MANIFEST_PATH in candidates && ModuleInfoClassSelection.isMultiReleaseJar(
                    reader.readBytes(MANIFEST_PATH).inputStream().use { Manifest(it) }.mainAttributes
                )
            },
            versionedModuleInfos = {
                candidates.mapNotNull { path -> versionDirectoryName(path)?.let { it to path } }.toMap()
            },
        )
    }

    /** Returns `<n>` for a `META-INF/versions/<n>/module-info.class` path, or `null` for any other path. */
    private fun versionDirectoryName(unixStyleRelativePath: String): String? {
        if (!unixStyleRelativePath.startsWith(VERSIONS_PREFIX) || !unixStyleRelativePath.endsWith("/$MODULE_INFO_CLASS")) return null
        return unixStyleRelativePath.substring(VERSIONS_PREFIX.length, unixStyleRelativePath.length - MODULE_INFO_CLASS.length - 1)
            .takeIf { '/' !in it }
    }

    private const val MANIFEST_PATH = "META-INF/MANIFEST.MF"
    private const val VERSIONS_PREFIX = "META-INF/versions/"
}

private sealed interface DirectoryOrJarReader : Closeable {

    val isJar: Boolean

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

    override val isJar: Boolean get() = false

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

    override val isJar: Boolean get() = true

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
        val entry = zipFile.getEntry(unixStyleRelativePath)
        return zipFile.getInputStream(entry).use { input ->
            if (entry.size == -1L) { // Allowed by spec, only achieved synthetically
                return@use input.readBytes()
            }
            val bytes = ByteArray(Math.toIntExact(entry.size))
            ByteStreams.readFully(input, bytes)
            bytes
        }
    }

    override fun close() {
        zipFile.close()
    }
}
