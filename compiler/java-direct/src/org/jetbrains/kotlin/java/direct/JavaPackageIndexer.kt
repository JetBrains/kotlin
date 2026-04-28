/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.java.direct.util.JavaSourceFileReader
import org.jetbrains.kotlin.java.direct.util.extractFileInfoLightweight
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

/**
 * Indexes `.java` files on source roots into a `package → className → files` map.
 *
 * Indexing is **lazy per-package**: [ensurePackageIndexed] navigates to the directory corresponding
 * to a package on demand via [VirtualFile.findChild] and scans only that directory's `.java` files.
 * Each package is indexed at most once (via [ConcurrentHashMap.computeIfAbsent]). File-type source
 * roots (considered rare) are indexed eagerly in the constructor.
 *
 * Files are classified via lightweight line scanning ([extractFileInfoLightweight]) that extracts
 * only the package name and top-level class names without invoking the parser. The full parse is
 * deferred to [JavaClassCache].
 *
 * `package-info.java` files are forwarded to [packageInfoIndexer] during the directory walk.
 */
internal class JavaPackageIndexer(
    sourceRoots: List<VirtualFile>,
    private val sourceFileReader: JavaSourceFileReader,
    private val packageInfoIndexer: JavaPackageInfoIndexer,
) {
    internal data class FileEntry(
        val file: VirtualFile,
        val packageFqName: FqName,
        val topLevelClassNames: Set<String>,
        val fileBaseName: String = file.name.removeSuffix(".java"),
    )

    // Directory source roots for lazy per-package indexing.
    private val directoryRoots: List<VirtualFile>

    // Entries from file-type source roots, populated in init (single-threaded).
    // Immutable after init. Merged into the per-package index during ensurePackageIndexed.
    private val fileRootIndex: Map<FqName, Map<String, List<FileEntry>>>

    // Package → className → list of file entries.
    // Populated lazily per-package via ensurePackageIndexed / computeIfAbsent.
    // Inner maps are immutable after creation — built atomically inside computeIfAbsent.
    private val index: ConcurrentHashMap<FqName, Map<String, List<FileEntry>>> = ConcurrentHashMap()

    private val packageDirectoryCache: ConcurrentHashMap<FqName, List<VirtualFile>> = ConcurrentHashMap()

    init {
        val (fileRoots, dirRoots) = sourceRoots.partition { !it.isDirectory }
        directoryRoots = dirRoots

        val fileRootIndexBuilder = HashMap<FqName, MutableMap<String, MutableList<FileEntry>>>()
        for (fileRoot in fileRoots) {
            if (!fileRoot.name.endsWith(".java")) continue
            if (fileRoot.name == "package-info.java") {
                packageInfoIndexer.indexPackageInfo(fileRoot, expectedPackage = null)
                continue
            }
            val entry = tryBuildFileEntry(fileRoot) ?: continue
            val classesByName = fileRootIndexBuilder.getOrPut(entry.packageFqName) { HashMap() }
            for (className in entry.topLevelClassNames) {
                classesByName.getOrPut(className) { mutableListOf() }.add(entry)
            }
        }

        // Top-level `.java` files of each directory root that declare a non-root package: register
        // them under their declared package so they're discoverable even when the disk path does
        // not mirror the package. This covers the test infrastructure case without implementing
        // full scan for cases when file path does not match package structure.
        for (dirRoot in dirRoots) {
            for (file in dirRoot.children ?: continue) {
                if (file.isDirectory) continue
                if (!file.name.endsWith(".java")) continue
                if (file.name == "package-info.java") continue
                val entry = tryBuildFileEntry(file) ?: continue
                if (entry.packageFqName.isRoot) continue
                val classesByName = fileRootIndexBuilder.getOrPut(entry.packageFqName) { HashMap() }
                for (className in entry.topLevelClassNames) {
                    classesByName.getOrPut(className) { mutableListOf() }.add(entry)
                }
            }
        }

        fileRootIndex = fileRootIndexBuilder
    }

    /**
     * Returns the directories corresponding to [packageFqName] across all directory source roots.
     * Navigates via [VirtualFile.findChild] chains (e.g. `root/"com"/"example"` for `com.example`).
     * Results are cached — each package is resolved at most once.
     */
    fun findPackageDirectories(packageFqName: FqName): List<VirtualFile> {
        if (packageFqName.isRoot) return directoryRoots
        return packageDirectoryCache.computeIfAbsent(packageFqName) {
            val segments = it.pathSegments().map { s -> s.asString() }
            directoryRoots.mapNotNull { root ->
                var dir: VirtualFile = root
                for (segment in segments) {
                    dir = dir.findChild(segment) ?: return@mapNotNull null
                    if (!dir.isDirectory) return@mapNotNull null
                }
                dir
            }
        }
    }

    /**
     * Ensures the given package has been indexed. Returns the package's class-name-to-file-entries
     * map. Indexing happens at most once per package (via [ConcurrentHashMap.computeIfAbsent]).
     */
    fun ensurePackageIndexed(packageFqName: FqName): Map<String, List<FileEntry>> {
        return index.computeIfAbsent(packageFqName) { fqName ->
            val dirEntries = indexPackageFromDirectories(fqName)
            val fileEntries = fileRootIndex[fqName]
            when {
                fileEntries == null -> dirEntries
                dirEntries.isEmpty() -> fileEntries
                else -> {
                    // Merge directory-scanned entries with file-root entries (rare edge case)
                    val merged = HashMap(dirEntries)
                    for ((className, entries) in fileEntries) {
                        merged.merge(className, entries) { a, b -> a + b }
                    }
                    merged
                }
            }
        }
    }

    /**
     * Indexes a single package by scanning its directory in each source root.
     * Files with mismatched package/directory are skipped, matching javac behavior.
     */
    private fun indexPackageFromDirectories(packageFqName: FqName): Map<String, List<FileEntry>> {
        val dirs = findPackageDirectories(packageFqName)
        if (dirs.isEmpty()) return emptyMap()

        val classesByName = HashMap<String, MutableList<FileEntry>>()

        for (dir in dirs) {
            val children = dir.children ?: continue
            for (file in children) {
                if (file.isDirectory) continue
                if (!file.name.endsWith(".java")) continue

                if (file.name == "package-info.java") {
                    packageInfoIndexer.indexPackageInfo(file, packageFqName)
                    continue
                }

                val entry = tryBuildFileEntry(file, packageFqName) ?: continue
                for (className in entry.topLevelClassNames) {
                    classesByName.getOrPut(className) { mutableListOf() }.add(entry)
                }
            }
        }

        return if (classesByName.isEmpty()) emptyMap() else classesByName
    }

    /**
     * Canonical class names (matching the file's basename) — PSI behavior per KT-4455; secondary
     * classes are still reachable when referenced by their [ClassId].
     */
    fun knownClassNamesInPackage(packageFqName: FqName): Set<String> {
        val classesByName = ensurePackageIndexed(packageFqName)
        if (classesByName.isEmpty()) return emptySet()
        return buildSet {
            for ((name, fileEntries) in classesByName) {
                if (fileEntries.any { it.fileBaseName == name }) add(name)
            }
        }
    }

    fun findFilesForClass(classId: ClassId): List<FileEntry> {
        val topLevelName = classId.relativeClassName.pathSegments().firstOrNull()?.asString()
            ?: return emptyList()
        val classesByName = ensurePackageIndexed(classId.packageFqName)
        return classesByName[topLevelName] ?: emptyList()
    }

    /**
     * Returns the direct sub-packages of [fqName] by listing subdirectories in the source roots.
     * Does NOT trigger per-package indexing — uses directory structure directly, which is simpler
     * and faster than iterating all index keys with string prefix matching.
     */
    fun subPackagesOf(fqName: FqName): Collection<FqName> {
        val dirs = if (fqName.isRoot) directoryRoots else findPackageDirectories(fqName)
        val result = mutableSetOf<FqName>()
        for (dir in dirs) {
            val children = dir.children ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    result.add(fqName.child(Name.identifier(child.name)))
                }
            }
        }
        return result
    }

    /**
     * Builds a [FileEntry] via lightweight line scanning; the full parse is deferred to
     * [JavaClassCache]. When [expectedPackage] is non-null, files whose declared package does not
     * match are skipped (matching javac's directory-mirrors-package rule).
     */
    private fun tryBuildFileEntry(file: VirtualFile, expectedPackage: FqName? = null): FileEntry? {
        val info = extractFileInfoLightweight(file, sourceFileReader) ?: return null
        val packageFqName = if (info.packageName != null) FqName(info.packageName) else FqName.ROOT

        if (expectedPackage != null && packageFqName != expectedPackage) return null

        // Only create an entry when the file's base name matches at least one declared class.
        // PSI behavior per KT-4455: a file like "E.java" that only declares class "F" (no class
        // "E") is not indexed — the classes it contains are not accessible to the compiler unless
        // they appear as return/parameter types within the same .java file.
        val fileBaseName = file.name.removeSuffix(".java")
        if (!info.topLevelClassNames.contains(fileBaseName)) return null

        return FileEntry(file, packageFqName, info.topLevelClassNames)
    }
}
