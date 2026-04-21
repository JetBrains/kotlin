/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Size threshold (in bytes) for eager vs. lightweight indexing.
 * Files at or below this size are parsed fully during per-package indexing,
 * and the resulting [JavaClass] instances are cached immediately (no re-parse on first access).
 * Files above this size are indexed via a lightweight line scanner that extracts only the package
 * name and top-level class names without invoking the parser.
 */
private const val SMALL_FILE_SIZE_THRESHOLD = 4096L

/**
 * A simple JavaClassFinder implementation over the direct Java AST parser used in this module.
 *
 * It scans provided [sourceRoots] for `.java` files, indexes packages and top-level class names,
 * and lazily parses files to produce [JavaClassOverAst] instances. Results are cached by [ClassId].
 *
 * Indexing is **lazy per-package**: instead of walking all source files at construction time,
 * the finder navigates to the directory corresponding to a package on demand using
 * [VirtualFile.findChild] and indexes only that directory's `.java` files. Each package is
 * indexed at most once (via [ConcurrentHashMap.computeIfAbsent]). Packages never queried by
 * the compiler are never scanned.
 *
 * Within a package directory, file indexing depends on size:
 * - **Small files** (≤ [SMALL_FILE_SIZE_THRESHOLD] bytes): parsed eagerly during package indexing;
 *   [JavaClass] instances are created and cached immediately, so the first [findClass] call is
 *   a cache hit with no additional parsing.
 * - **Large files**: indexed via lightweight line scanning ([extractFileInfoLightweight]) that
 *   extracts only the package name and top-level class names without invoking the parser.
 *   The full parse happens lazily on first [findClass] access, at which point **all** top-level
 *   classes in the file are cached to avoid re-parsing for sibling classes.
 */
class JavaClassFinderOverAstImpl(
    private val sourceRoots: List<VirtualFile>,
    private val debugLogFilePath: Path? = null,
    private val sourceFileReader: JavaSourceFileReader = DefaultJavaSourceFileReader,
) : JavaClassFinder {

    private data class FileEntry(
        val file: VirtualFile,
        val packageFqName: FqName,
        val topLevelClassNames: Set<String>,
        /** Pre-computed `file.name.removeSuffix(".java")` to avoid repeated allocation in [knownClassNamesInPackage]. */
        val fileBaseName: String = file.name.removeSuffix(".java"),
    )

    // Directory source roots for lazy per-package indexing.
    // File-type source roots (rare, test-only) are indexed eagerly in init via fileRootIndex.
    private val directoryRoots: List<VirtualFile>

    // Entries from file-type source roots, populated in init (single-threaded).
    // Immutable after init. Merged into the per-package index during ensurePackageIndexed.
    private val fileRootIndex: Map<FqName, Map<String, List<FileEntry>>>

    // Package → className → list of file entries.
    // Populated lazily per-package via ensurePackageIndexed / computeIfAbsent.
    // Inner maps are immutable after creation — built atomically inside computeIfAbsent.
    private val index: ConcurrentHashMap<FqName, Map<String, List<FileEntry>>> = ConcurrentHashMap()

    // class cache for already created JavaClassOverAst.
    // Note: ConcurrentHashMap disallows null values, so this map stores only *positive* hits;
    // negative results (ClassId definitely not in source) are tracked in [negativeClassCache] below.
    private val classCache: MutableMap<ClassId, JavaClass> = ConcurrentHashMap()

    // Negative cache for Step 2.5: avoids re-parsing candidate files for ClassIds that were
    // already looked up and found absent. The top-level "definitely not in source" check is
    // handled much earlier by [isClassInIndex] (pure index hit), so this set only really matters
    // for *inner* ClassIds whose top-level parent is in the index but that don't actually exist
    // (e.g. typo'd inner class names or FIR probing non-existent nested types during overload
    // resolution). Without it, every miss re-parses the top-level file.
    private val negativeClassCache: MutableSet<ClassId> = ConcurrentHashMap.newKeySet()

    // package cache
    private val packageCache: MutableMap<FqName, JavaPackage> = ConcurrentHashMap()

    // Package-level annotations from package-info.java files.
    // ConcurrentHashMap because different packages can be indexed concurrently by different threads.
    private val packageAnnotationNodes: ConcurrentHashMap<FqName, List<JavaAnnotation>> = ConcurrentHashMap()

    // Cache: package FqName → list of directories (one per source root that contains this package).
    // Populated via computeIfAbsent — each package resolved at most once.
    private val packageDirectoryCache: ConcurrentHashMap<FqName, List<VirtualFile>> = ConcurrentHashMap()

    // Supertype graph queries (direct supertypes, inherited inner classes).
    // Extracted into a focused collaborator in Step 1.6 of the refactoring plan.
    private val supertypeGraph = JavaSupertypeGraph(
        classCacheLookup = { classCache[it] },
        filesForClassLookup = { classId -> findFilesForClass(classId).map { it.file } },
        sameClassInSameFilePackage = { pkg, name -> ensurePackageIndexed(pkg).containsKey(name) },
        sourceFileReader = sourceFileReader,
    )

    private val debugLogFile: File? = debugLogFilePath?.toFile()

    init {
        // Classify source roots: directory roots use lazy per-package indexing,
        // file-type roots (rare, test-only) are indexed eagerly.
        val (fileRoots, dirRoots) = sourceRoots.partition { !it.isDirectory }
        directoryRoots = dirRoots

        val fileRootIndexBuilder = HashMap<FqName, MutableMap<String, MutableList<FileEntry>>>()
        for (fileRoot in fileRoots) {
            if (!fileRoot.name.endsWith(".java")) continue
            if (fileRoot.name == "package-info.java") {
                indexPackageInfo(fileRoot)
                continue
            }
            val entry = tryBuildFileEntry(fileRoot) ?: continue
            val classesByName = fileRootIndexBuilder.getOrPut(entry.packageFqName) { HashMap() }
            for (className in entry.topLevelClassNames) {
                classesByName.getOrPut(className) { mutableListOf() }.add(entry)
            }
        }
        fileRootIndex = fileRootIndexBuilder
    }

    // ---- Directory navigation ----

    /**
     * Returns the directories corresponding to [packageFqName] across all directory source roots.
     * Navigates via [VirtualFile.findChild] chains (e.g. `root/"com"/"example"` for `com.example`).
     * Results are cached — each package is resolved at most once.
     */
    private fun findPackageDirectories(packageFqName: FqName): List<VirtualFile> {
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

    // ---- Lazy per-package indexing ----

    /**
     * Ensures the given package has been indexed. Returns the package's class-name-to-file-entries
     * map. Indexing happens at most once per package (via [ConcurrentHashMap.computeIfAbsent]).
     *
     * The returned map is immutable — it is created inside the `computeIfAbsent` lambda and
     * never modified afterward.
     */
    private fun ensurePackageIndexed(packageFqName: FqName): Map<String, List<FileEntry>> {
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
     * Only files whose declared package matches [packageFqName] are included (files with
     * mismatched package/directory are skipped, matching javac behavior).
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
                    indexPackageInfo(file, packageFqName)
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

    // ---- Public API ----

    /**
     * Checks if a top-level class with the given ClassId is present in the source index.
     * Triggers lazy indexing for the class's package on first access.
     * Safe to call at any point, including during FIR type processing.
     */
    fun isClassInIndex(classId: ClassId): Boolean {
        val topLevelName = classId.relativeClassName.pathSegments().firstOrNull()?.asString() ?: return false
        return ensurePackageIndexed(classId.packageFqName).containsKey(topLevelName)
    }

    override fun findClass(request: JavaClassFinder.Request): JavaClass? {
        val classId = request.classId
        classCache[classId]?.let { return it }
        if (classId in negativeClassCache) return null

        val classes = findClasses(request)
        val result = classes.firstOrNull()
        if (result != null) {
            classCache[classId] = result
        } else {
            negativeClassCache.add(classId)
        }
        return result
    }

    override fun findClasses(request: JavaClassFinder.Request): List<JavaClass> {
        val classId = request.classId
        val packageFqName = classId.packageFqName
        val relative = classId.relativeClassName

        val segments = relative.pathSegments().map(Name::asString)
        if (segments.isEmpty()) return emptyList()
        val topLevelName = segments.first()
        val innerNames = segments.drop(1)

        val classesByName = ensurePackageIndexed(packageFqName)
        val candidates = classesByName[topLevelName] ?: return emptyList()

        val result = mutableListOf<JavaClass>()
        for (file in candidates) {
            val javaClass = parseTopLevelClassFromFile(file, topLevelName) ?: continue
            val resolved = if (innerNames.isEmpty()) {
                javaClass
            } else {
                var cur: JavaClass? = javaClass
                for (name in innerNames) {
                    cur = cur?.findInnerClass(Name.identifier(name))
                    if (cur == null) break
                }
                cur
            }
            if (resolved != null) result.add(resolved)
        }
        debugLogFile?.appendText("findClasses: ${request.classId} ->\n  ${result.joinToString("\n  ") { it.classId?.asFqNameString() ?: it.name.asString() }}\n")
        return result
    }

    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? {
        val classesByName = ensurePackageIndexed(fqName)
        // A package with no class files may still exist via `package-info.java` carrying only
        // package-level annotations — respect those so the annotations are not lost.
        if (classesByName.isEmpty() && packageAnnotationNodes[fqName].isNullOrEmpty()) return null
        // computeIfAbsent (not getOrPut) so concurrent callers share a single JavaPackageOverAst
        // instance instead of each creating their own and racing on put.
        return packageCache.computeIfAbsent(fqName) { JavaPackageOverAst(fqName, this) }
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String>? {
        val classesByName = ensurePackageIndexed(packageFqName)
        if (classesByName.isEmpty()) return emptySet()
        // Only return canonical class names (where the class name matches the file's basename).
        // Secondary classes (e.g., class B defined alongside class A in A.java) are accessible
        // when their ClassId is known directly (e.g., as return type from the same file's API)
        // but are NOT exposed as standalone same-package names. This matches PSI behavior.
        //
        // KT-4455: non-canonical Java classes are not visible as independent types from Kotlin.
        // Fixing this would mean removing the filter below so that secondary classes appear in
        // package-level name sets — but that is a deliberate Kotlin/JVM design choice, not a
        // java-direct bug, so the fix belongs in a separate task tracked by the issue.
        return buildSet {
            for ((name, fileEntries) in classesByName) {
                if (fileEntries.any { it.fileBaseName == name }) add(name)
            }
        }
    }

    override fun canComputeKnownClassNamesInPackage(): Boolean = true

    // ---- Package info ----

    /**
     * Indexes package-level annotations from a `package-info.java` file.
     *
     * @param expectedPackage When non-null, validates that the file's declared package matches.
     *   Used during directory-based lazy indexing to skip files with mismatched package/directory.
     *   When null (file-type source roots in init), any package is accepted.
     */
    private fun indexPackageInfo(file: VirtualFile, expectedPackage: FqName? = null) {
        val source = sourceFileReader.readFileContent(file) ?: return
        val tree = parseJavaToLightTree(source, 0)
        val root = tree.getRoot()

        val packageStmt = tree.findChildByType(root, JavaSyntaxElementType.PACKAGE_STATEMENT)
        val packageName = (packageStmt ?: root).let {
            tree.findChildByType(it, JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.let { ref -> tree.getText(ref).toString() }
        } ?: return
        val packageFqName = FqName(packageName)

        // Validate against expected directory-derived package if specified.
        if (expectedPackage != null && packageFqName != expectedPackage) return

        val resolutionContext = JavaResolutionContext.create(tree, classFinderProvider = { this })
        val annotations = mutableListOf<JavaAnnotation>()

        // Annotations are in PACKAGE_STATEMENT → MODIFIER_LIST → ANNOTATION (KMP parser structure).
        // Also check other plausible locations for robustness.
        packageStmt?.let { ps ->
            tree.findChildByType(ps, JavaSyntaxElementType.MODIFIER_LIST)?.let { ml ->
                tree.getChildrenByType(ml, JavaSyntaxElementType.ANNOTATION)
                    .mapTo(annotations) { JavaAnnotationOverAst(it, tree, resolutionContext) }
            }
            tree.getChildrenByType(ps, JavaSyntaxElementType.ANNOTATION)
                .mapTo(annotations) { JavaAnnotationOverAst(it, tree, resolutionContext) }
        }
        tree.getChildrenByType(root, JavaSyntaxElementType.ANNOTATION)
            .mapTo(annotations) { JavaAnnotationOverAst(it, tree, resolutionContext) }
        tree.findChildByType(root, JavaSyntaxElementType.MODIFIER_LIST)?.let { ml ->
            tree.getChildrenByType(ml, JavaSyntaxElementType.ANNOTATION)
                .mapTo(annotations) { JavaAnnotationOverAst(it, tree, resolutionContext) }
        }

        if (annotations.isNotEmpty()) {
            packageAnnotationNodes.merge(packageFqName, annotations.toList()) { existing, new -> existing + new }
        }
    }

    internal fun getPackageAnnotations(packageFqName: FqName): List<JavaAnnotation> {
        ensurePackageIndexed(packageFqName)
        return packageAnnotationNodes[packageFqName] ?: emptyList()
    }

    // ---- File entry building ----

    /**
     * Builds a [FileEntry] for the given file.
     *
     * @param expectedPackage When non-null, validates that the file's declared package matches.
     *   Files with mismatched packages return null (matching javac behavior, which requires
     *   directory structure to mirror package declarations).
     */
    private fun tryBuildFileEntry(file: VirtualFile, expectedPackage: FqName? = null): FileEntry? {
        val fileSize = file.length
        return if (fileSize <= SMALL_FILE_SIZE_THRESHOLD) {
            tryBuildFileEntryWithFullParse(file, expectedPackage)
        } else {
            tryBuildFileEntryLightweight(file, expectedPackage)
        }
    }

    /**
     * Small-file path: parse the file fully and cache all [JavaClassOverAst] instances
     * so that subsequent [findClass] calls are pure cache hits.
     */
    private fun tryBuildFileEntryWithFullParse(file: VirtualFile, expectedPackage: FqName? = null): FileEntry? {
        val source = sourceFileReader.readFileContent(file) ?: return null
        val tree = parseJavaToLightTree(source, 0)
        val root = tree.getRoot()

        val packageStmt = tree.findChildByType(root, JavaSyntaxElementType.PACKAGE_STATEMENT)
        val packageName = packageStmt?.let {
            tree.findChildByType(it, JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.let { ref -> tree.getText(ref).toString() }
        }
        val packageFqName = if (packageName != null) FqName(packageName) else FqName.ROOT

        // Validate against expected directory-derived package if specified.
        if (expectedPackage != null && packageFqName != expectedPackage) return null

        val classNames = tree.getChildrenByType(root, JavaSyntaxElementType.CLASS).mapNotNull { node ->
            tree.findChildByType(node, JavaSyntaxTokenType.IDENTIFIER)?.let { tree.getText(it).toString() }
        }.toSet()

        if (classNames.isEmpty()) return null

        val fileBaseName = file.name.removeSuffix(".java")
        if (!classNames.contains(fileBaseName)) return null

        // Eagerly create and cache all top-level JavaClass instances.
        val resolutionContext = JavaResolutionContext.create(tree, classFinderProvider = { this })
        for (className in classNames) {
            val classId = ClassId(packageFqName, FqName(className), isLocal = false)
            if (classId !in classCache) {
                val javaClass = resolutionContext.findLocalClass(Name.identifier(className))
                if (javaClass != null) {
                    classCache[classId] = javaClass
                }
            }
        }

        return FileEntry(file, packageFqName, classNames)
    }

    /**
     * Large-file path: extract package name and top-level class names by scanning
     * the file line by line without invoking the parser.
     * The full parse is deferred to [parseTopLevelClassFromFile] on first access.
     */
    private fun tryBuildFileEntryLightweight(file: VirtualFile, expectedPackage: FqName? = null): FileEntry? {
        val info = extractFileInfoLightweight(file, sourceFileReader) ?: return null
        val packageFqName = if (info.packageName != null) FqName(info.packageName) else FqName.ROOT

        // Validate against expected directory-derived package if specified.
        if (expectedPackage != null && packageFqName != expectedPackage) return null

        // Only create an entry when the file's base name matches at least one declared class.
        // This matches PSI's behavior per KT-4455: a file like "E.java" that only declares
        // class "F" (no class "E") is not indexed — the classes it contains are not accessible
        // to the compiler unless they appear as return/parameter types within the same .java file.
        val fileBaseName = file.name.removeSuffix(".java")
        if (!info.topLevelClassNames.contains(fileBaseName)) return null

        return FileEntry(file, packageFqName, info.topLevelClassNames)
    }

    private fun parseTopLevelClassFromFile(file: FileEntry, simpleName: String): JavaClassOverAst? {
        // Check cache first: this ensures that all lookups (direct or via inner-class navigation)
        // return the *same* JavaClassOverAst instance for a given top-level class. Without this,
        // findClass("a.x") and the intermediate "a.x" created during findClass("a.x.y") would
        // produce different JavaClassOverAst instances with different JavaTypeParameterOverAst
        // instances for the same type parameter (e.g. T). FIR matches Java type parameters by
        // object identity, so mismatched instances cause "ERROR CLASS: Unresolved name: T".
        val classId = ClassId(file.packageFqName, FqName(simpleName), isLocal = false)
        classCache[classId]?.let { return it as? JavaClassOverAst }

        val source = sourceFileReader.readFileContent(file.file) ?: return null
        val tree = parseJavaToLightTree(source, 0)
        val root = tree.getRoot()
        val resolutionContext = JavaResolutionContext.create(tree, classFinderProvider = { this })

        // Cache ALL top-level classes from this file to avoid re-parsing for sibling classes.
        val allClassNames = tree.getChildrenByType(root, JavaSyntaxElementType.CLASS).mapNotNull {
            tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() }
        }
        for (className in allClassNames) {
            val cid = ClassId(file.packageFqName, FqName(className), isLocal = false)
            if (cid !in classCache) {
                val javaClass = resolutionContext.findLocalClass(Name.identifier(className))
                if (javaClass != null) {
                    classCache[cid] = javaClass
                }
            }
        }

        return classCache[classId] as? JavaClassOverAst
    }

    // ---- Internal API used by JavaPackageOverAst ----

    internal fun classesInPackage(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<JavaClass> {
        val classesByName = ensurePackageIndexed(fqName)
        if (classesByName.isEmpty()) return emptyList()
        val result = mutableListOf<JavaClass>()
        for ((simpleName, files) in classesByName) {
            val name = Name.identifier(simpleName)
            if (!nameFilter(name)) continue
            for (file in files) {
                parseTopLevelClassFromFile(file, simpleName)?.let { result.add(it) }
            }
        }
        return result
    }

    /**
     * Returns the direct sub-packages of [fqName] by listing subdirectories in the source roots.
     * Does NOT trigger per-package indexing — uses directory structure directly, which is simpler
     * and faster than the previous approach of iterating all index keys with string prefix matching.
     */
    internal fun subPackagesOf(fqName: FqName): Collection<FqName> {
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

    // ---- Supertype graph delegation ----

    /**
     * Returns the direct supertype [ClassId]s for a class.
     * Delegates to [JavaSupertypeGraph].
     */
    internal fun getDirectSupertypes(classId: ClassId): List<ClassId> =
        supertypeGraph.getDirectSupertypes(classId)

    /**
     * Recursively collects all inner class names from the supertype hierarchy.
     * Delegates to [JavaSupertypeGraph].
     */
    internal fun collectInheritedInnerClasses(classId: ClassId): Map<String, Set<ClassId>> =
        supertypeGraph.collectInheritedInnerClasses(classId)

    private fun findFilesForClass(classId: ClassId): List<FileEntry> {
        val topLevelName = classId.relativeClassName.pathSegments().firstOrNull()?.asString()
            ?: return emptyList()
        val classesByName = ensurePackageIndexed(classId.packageFqName)
        return classesByName[topLevelName] ?: emptyList()
    }
}
