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
 * A directory or single-file source root with an optional `packagePrefix`.
 *
 * `packagePrefix` mirrors `org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot.packagePrefix`
 */
internal data class JavaSourceRootEntry(val root: VirtualFile, val packagePrefix: FqName) {
    companion object {
        fun fromRootsWithoutPrefix(roots: List<VirtualFile>): List<JavaSourceRootEntry> =
            roots.map { JavaSourceRootEntry(it, FqName.ROOT) }
    }
}

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
 *
 * Each source root may carry a `packagePrefix` (see [JavaSourceRootEntry]); when non-root, paths
 * under the root are treated as if they lived under the prefix package, matching PSI's
 * `JavaClassFinderImpl` behaviour for `<javaSourceRoots packagePrefix="...">` content roots.
 */
internal class JavaPackageIndexer(
    sourceRoots: List<JavaSourceRootEntry>,
    private val sourceFileReader: JavaSourceFileReader,
    private val packageInfoIndexer: JavaPackageInfoIndexer,
) {
    internal data class FileEntry(
        val file: VirtualFile,
        val packageFqName: FqName,
        val topLevelClassNames: Set<String>,
        val fileBaseName: String = file.name.removeSuffix(".java"),
    )

    // Directory source roots (with their packagePrefix) for lazy per-package indexing.
    private val directoryRoots: List<JavaSourceRootEntry>

    // Entries from file-type source roots, populated in init (single-threaded).
    // Immutable after init. Merged into the per-package index during ensurePackageIndexed.
    private val fileRootIndex: Map<FqName, Map<String, List<FileEntry>>>

    // Package → className → list of file entries.
    // Populated lazily per-package via ensurePackageIndexed / computeIfAbsent.
    // Inner maps are immutable after creation — built atomically inside computeIfAbsent.
    private val index: ConcurrentHashMap<FqName, Map<String, List<FileEntry>>> = ConcurrentHashMap()

    private val packageDirectoryCache: ConcurrentHashMap<FqName, List<VirtualFile>> = ConcurrentHashMap()

    init {
        val (fileRoots, dirRoots) = sourceRoots.partition { !it.root.isDirectory }
        directoryRoots = dirRoots

        val fileRootIndexBuilder = HashMap<FqName, MutableMap<String, MutableList<FileEntry>>>()
        for (fileRootEntry in fileRoots) {
            val fileRoot = fileRootEntry.root
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
        // not mirror the package.
        // Why: javac places `.class` files by declared package, not by source location, and Kotlin's
        // diagnostic test framework writes `// FILE: foo.java` blocks flat at the source root
        // regardless of `package` declaration. The lazy directory-walk below
        // (indexPackageFromDirectories) follows javac's directory-mirrors-package rule and would
        // skip such files. Files at the top level declaring the root package are still picked up
        // by the regular root-package walk, so we only register the non-root case here to avoid
        // duplicate entries.
        for (dirRootEntry in dirRoots) {
            val dirRoot = dirRootEntry.root
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
     * Honours each root's `packagePrefix`: if a root has prefix `com.intellij`, then a request for
     * package `com.intellij.foo` descends to `<root>/foo`, and the root contributes nothing for any
     * package that is not equal to or under that prefix.
     * Results are cached — each package is resolved at most once.
     */
    fun findPackageDirectories(packageFqName: FqName): List<VirtualFile> {
        if (packageFqName.isRoot) {
            // Only roots without a packagePrefix expose the unqualified root package — a prefixed
            // root's disk top-level lives in `<prefix>`, not in `<root-package>`.
            return directoryRoots.mapNotNull { if (it.packagePrefix.isRoot) it.root else null }
        }
        return packageDirectoryCache.computeIfAbsent(packageFqName) {
            val requestedSegments = it.pathSegments().map { s -> s.asString() }
            directoryRoots.mapNotNull { entry ->
                val prefix = entry.packagePrefix
                val relativeSegments: List<String> = when {
                    prefix.isRoot -> requestedSegments
                    !packageStartsWithOrEquals(it, prefix) -> return@mapNotNull null
                    else -> requestedSegments.drop(prefix.pathSegments().size)
                }
                var dir: VirtualFile = entry.root
                for (segment in relativeSegments) {
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
     * Returns `true` if [packageFqName] exists as a Java source package: either a directory
     * mirroring the package exists in some source root, or a known file-root file lives in this
     * package or any of its sub-packages.
     *
     * This recognises **ancestor** packages: e.g. a single file at
     * `priv/members/check/Foo.java` makes the intermediate packages `priv` and `priv.members`
     * valid even though those directories carry no `.java` files of their own. PSI's
     * [org.jetbrains.kotlin.load.java.JavaClassFinderImpl.findPackage] used to recognise these
     * the same way, so dotted FQN references (`priv.members.check.Foo()`) and star imports
     * (`import priv.members.check.*`) need this recognition for source parity once
     * [BinaryJavaClassFinder] is the binary half (the index-based binary finder only consults
     * binary roots and cannot see the source-only ancestor packages).
     *
     * Cheaper than [ensurePackageIndexed]: walks `findChild` chains and `fileRootIndex.keys`,
     * never reads file contents.
     */
    fun containsPackage(packageFqName: FqName): Boolean {
        if (packageFqName.isRoot) return true
        // A prefixed root makes its prefix package and every ancestor of the prefix valid:
        // e.g. a root with `packagePrefix=com.intellij` exposes `com`, `com.intellij`, and
        // `com.intellij.foo` (the latter when it has a matching subdirectory).
        for (entry in directoryRoots) {
            val prefix = entry.packagePrefix
            if (!prefix.isRoot && packageStartsWithOrEquals(prefix, packageFqName)) return true
        }
        if (findPackageDirectories(packageFqName).isNotEmpty()) return true
        // File-type source roots are rare; iterating `fileRootIndex.keys` is cheap.
        if (fileRootIndex.isNotEmpty()) {
            val prefix = packageFqName.asString()
            val prefixDot = "$prefix."
            for (knownPkg in fileRootIndex.keys) {
                val k = knownPkg.asString()
                if (k == prefix || k.startsWith(prefixDot)) return true
            }
        }
        return false
    }

    /**
     * Returns the direct sub-packages of [fqName] by listing subdirectories in the source roots.
     * Does NOT trigger per-package indexing — uses directory structure directly, which is simpler
     * and faster than iterating all index keys with string prefix matching.
     *
     * For prefixed roots, sub-packages may also come from the prefix itself: e.g. a root with
     * `packagePrefix=com.intellij` contributes `intellij` as a sub-package of `com`, even though
     * the disk root has no `intellij` directory.
     */
    fun subPackagesOf(fqName: FqName): Collection<FqName> {
        val result = mutableSetOf<FqName>()
        for (entry in directoryRoots) {
            val prefix = entry.packagePrefix
            when {
                prefix.isRoot -> {
                    // No prefix → ordinary directory walk relative to the requested package.
                    val dir = if (fqName.isRoot) entry.root
                    else findPackageDirectoryUnder(entry.root, fqName.pathSegments().map { it.asString() })
                    addSubdirsAsSubPackages(dir, fqName, result)
                }
                packageStartsWithOrEquals(prefix, fqName) -> {
                    // The requested package is an ancestor of (or equal to) the prefix → the
                    // prefix's next segment after `fqName` is a valid sub-package contributed by
                    // the prefix. When `fqName == prefix`, the disk children of `entry.root` are
                    // the actual sub-packages.
                    val prefixSegments = prefix.pathSegments()
                    val fqSegments = fqName.pathSegments()
                    if (prefixSegments.size > fqSegments.size) {
                        result.add(fqName.child(prefixSegments[fqSegments.size]))
                    } else {
                        // fqName == prefix; the disk children of root list the next segments.
                        addSubdirsAsSubPackages(entry.root, fqName, result)
                    }
                }
                packageStartsWithOrEquals(fqName, prefix) -> {
                    // The requested package is below the prefix → walk the disk after stripping
                    // the prefix segments.
                    val rel = fqName.pathSegments().drop(prefix.pathSegments().size).map { it.asString() }
                    val dir = findPackageDirectoryUnder(entry.root, rel)
                    addSubdirsAsSubPackages(dir, fqName, result)
                }
                // else: this root cannot contribute to `fqName`.
            }
        }
        return result
    }

    private fun findPackageDirectoryUnder(root: VirtualFile, relativeSegments: List<String>): VirtualFile? {
        var dir: VirtualFile = root
        for (segment in relativeSegments) {
            dir = dir.findChild(segment) ?: return null
            if (!dir.isDirectory) return null
        }
        return dir
    }

    private fun addSubdirsAsSubPackages(dir: VirtualFile?, fqName: FqName, result: MutableSet<FqName>) {
        if (dir == null) return
        val children = dir.children ?: return
        for (child in children) {
            if (child.isDirectory) {
                result.add(fqName.child(Name.identifier(child.name)))
            }
        }
    }

    private fun packageStartsWithOrEquals(child: FqName, maybeAncestor: FqName): Boolean =
        maybeAncestor.isRoot || child == maybeAncestor || child.startsWith(maybeAncestor)

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
