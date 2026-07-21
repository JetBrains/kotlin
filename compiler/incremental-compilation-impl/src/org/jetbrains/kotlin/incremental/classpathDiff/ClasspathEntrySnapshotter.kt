/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import com.google.common.io.ByteStreams
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.incremental.classpathDiff.impl.*
import org.jetbrains.kotlin.incremental.impl.hashToLong
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataOutputStream
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
        val parseInlinedLocalClasses: Boolean,
        val expandTypeAliases: Boolean,
    )

    private val DEFAULT_CLASS_FILTER = { unixStyleRelativePath: String, isDirectory: Boolean ->
        !isDirectory
                && unixStyleRelativePath.endsWith(".class", ignoreCase = true)
                && !unixStyleRelativePath.equals("module-info.class", ignoreCase = true)
                && !isPackageInfoClassPath(unixStyleRelativePath)
                && !unixStyleRelativePath.startsWith("meta-inf/", ignoreCase = true)
    }

    private fun isModuleInfoClassPath(unixStyleRelativePath: String): Boolean {
        val path = unixStyleRelativePath.lowercase()
        if (path == "module-info.class") return true
        if (!path.startsWith("meta-inf/versions/") || !path.endsWith("/module-info.class")) return false
        val version = path.removePrefix("meta-inf/versions/").removeSuffix("/module-info.class")
        return version.toIntOrNull() != null
    }

    private fun isPackageInfoClassPath(unixStyleRelativePath: String): Boolean {
        val path = unixStyleRelativePath.lowercase()
        return path == "package-info.class" || path.endsWith("/package-info.class")
    }

    private fun packageNameOfPackageInfo(unixStyleRelativePath: String): String {
        var path = unixStyleRelativePath
        if (path.startsWith("META-INF/versions/", ignoreCase = true)) {
            // Drop the "META-INF/versions/<n>/" prefix (three path segments) to get the logical package path.
            path = path.substringAfter('/').substringAfter('/').substringAfter('/', missingDelimiterValue = "")
        }
        return path.removeSuffix("package-info.class").removeSuffix("/").replace('/', '.')
    }

    /**
     * Paths must already be sorted (as [DirectoryOrJarReader.getUnixStyleRelativePaths] returns them) so the result is stable;
     * each path is folded in alongside its bytes, so relocating a file (e.g. between multi-release version dirs) also counts as a change.
     */
    private fun combinedContentHash(reader: DirectoryOrJarReader, paths: List<String>): Long? =
        paths.takeIf { it.isNotEmpty() }?.let {
            val buffer = ByteArrayOutputStream()
            DataOutputStream(buffer).use { out ->
                for (path in paths) {
                    out.writeUTF(path)
                    out.write(reader.readBytes(path))
                }
            }
            buffer.toByteArray().hashToLong()
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
            val moduleInfoHash = combinedContentHash(
                directoryOrJarReader,
                directoryOrJarReader.getUnixStyleRelativePaths { path, isDirectory -> !isDirectory && isModuleInfoClassPath(path) }
            )
            val packageInfoHashes = directoryOrJarReader
                .getUnixStyleRelativePaths { path, isDirectory -> !isDirectory && isPackageInfoClassPath(path) }
                .groupBy { packageNameOfPackageInfo(it) }
                .mapValues { combinedContentHash(directoryOrJarReader, it.value)!! }
            return ClasspathEntrySnapshot(
                classSnapshots = classes.map { it.classFile.unixStyleRelativePath }.zip(snapshots).toMap(LinkedHashMap()),
                moduleInfoHash = moduleInfoHash,
                packageInfoHashes = packageInfoHashes,
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
