/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.isRegularFile

/**
 * Size threshold (in bytes) for eager vs. lightweight indexing.
 * Files at or below this size are parsed fully during [JavaClassFinderOverAstImpl.buildIndex],
 * and the resulting [JavaClass] instances are cached immediately (no re-parse on first access).
 * Files above this size are indexed via a lightweight line scanner that extracts only the package
 * name and top-level class names without invoking the parser.
 */
private const val SMALL_FILE_SIZE_THRESHOLD = 4096L

private val PACKAGE_REGEX = Regex("""\bpackage\s+([\w.]+)\s*;""")
private val DECLARATION_REGEX = Regex("""\b(class|interface|enum|record)\s+([A-Za-z_]\w*)""")

/**
 * Result of lightweight (no-parse) file scanning.
 */
internal data class LightweightFileInfo(
    val packageName: String?,
    val topLevelClassNames: Set<String>,
)

/**
 * Strips single-line (`//`) and block (`/* */`) comments from a line.
 * Tracks block comment state across lines.
 *
 * @return pair of (effective text with comments removed, whether still inside a block comment)
 */
private fun stripLineComments(line: String, inBlockComment: Boolean): Pair<String, Boolean> {
    val sb = StringBuilder()
    var inComment = inBlockComment
    var i = 0
    while (i < line.length) {
        if (inComment) {
            val endIdx = line.indexOf("*/", i)
            if (endIdx >= 0) {
                inComment = false
                i = endIdx + 2
            } else {
                return sb.toString() to true
            }
        } else {
            if (i + 1 < line.length) {
                if (line[i] == '/' && line[i + 1] == '/') {
                    return sb.toString() to false
                }
                if (line[i] == '/' && line[i + 1] == '*') {
                    inComment = true
                    i += 2
                    continue
                }
            }
            sb.append(line[i])
            i++
        }
    }
    return sb.toString() to inComment
}

/**
 * Extracts package name and top-level class/interface/enum/record names from a Java file
 * without invoking the parser. Scans the file line by line, stripping comments and tracking
 * brace depth to distinguish top-level declarations from nested ones.
 *
 * This is much cheaper than full parsing and is used for indexing large files.
 */
internal fun extractFileInfoLightweight(path: Path): LightweightFileInfo? {
    var packageName: String? = null
    val classNames = mutableSetOf<String>()
    var inBlockComment = false
    var braceDepth = 0

    Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader ->
        var rawLine = reader.readLine()
        while (rawLine != null) {
            val (effective, stillInComment) = stripLineComments(rawLine, inBlockComment)
            inBlockComment = stillInComment

            if (effective.isNotBlank()) {
                val depthBeforeLine = braceDepth
                for (ch in effective) {
                    when (ch) {
                        '{' -> braceDepth++
                        '}' -> braceDepth--
                    }
                }

                if (packageName == null && depthBeforeLine == 0) {
                    PACKAGE_REGEX.find(effective)?.let {
                        packageName = it.groupValues[1]
                    }
                }

                if (depthBeforeLine == 0) {
                    DECLARATION_REGEX.find(effective)?.let {
                        classNames.add(it.groupValues[2])
                    }
                }
            }

            rawLine = reader.readLine()
        }
    }

    if (classNames.isEmpty()) return null
    return LightweightFileInfo(packageName, classNames)
}

/**
 * A simple JavaClassFinder implementation over the direct Java AST parser used in this module.
 *
 * It scans provided [sourceRoots] for `.java` files, indexes packages and top-level class names,
 * and lazily parses files to produce [JavaClassOverAst] instances. Results are cached by [ClassId].
 *
 * Indexing strategy depends on file size:
 * - **Small files** (≤ [SMALL_FILE_SIZE_THRESHOLD] bytes): parsed eagerly during index build;
 *   [JavaClass] instances are created and cached immediately, so the first [findClass] call is
 *   a cache hit with no additional parsing.
 * - **Large files**: indexed via lightweight line scanning ([extractFileInfoLightweight]) that
 *   extracts only the package name and top-level class names without invoking the parser.
 *   The full parse happens lazily on first [findClass] access, at which point **all** top-level
 *   classes in the file are cached to avoid re-parsing for sibling classes.
 */
class JavaClassFinderOverAstImpl(
    private val sourceRoots: List<Path>,
    private val debugLogFilePath: Path? = null,
) : JavaClassFinder {

    private data class FileEntry(
        val path: Path,
        val packageFqName: FqName,
        val topLevelClassNames: Set<String>,
    )

    // package -> className -> list of files that declare such class
    private val index: MutableMap<FqName, MutableMap<String, MutableList<FileEntry>>> = ConcurrentHashMap()

    // class cache for already created JavaClassOverAst
    private val classCache: MutableMap<ClassId, JavaClass?> = ConcurrentHashMap()

    // package cache
    private val packageCache: MutableMap<FqName, JavaPackage> = ConcurrentHashMap()

    // Package-level annotations from package-info.java files
    private val packageAnnotationNodes: MutableMap<FqName, MutableList<JavaAnnotation>> = ConcurrentHashMap()

    // Cache: ClassId -> list of supertype ClassIds (direct only)
    private val supertypeCache: MutableMap<ClassId, List<ClassId>> = ConcurrentHashMap()

    private val debugLogFile: File? = debugLogFilePath?.toFile()

    init {
        buildIndex()
    }

    /**
     * Checks if a top-level class with the given ClassId is present in the source index.
     * Pure index lookup — no file I/O, no class instantiation.
     * Safe to call at any point, including during FIR type processing.
     */
    fun isClassInIndex(classId: ClassId): Boolean {
        val topLevelName = classId.relativeClassName.pathSegments().firstOrNull()?.asString() ?: return false
        return index[classId.packageFqName]?.containsKey(topLevelName) == true
    }

    override fun findClass(request: JavaClassFinder.Request): JavaClass? {
        val classId = request.classId
        classCache[classId]?.let { return it }

        val classes = findClasses(request)
        val result = classes.firstOrNull()
        if (result != null) {
            classCache[classId] = result
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

        val candidates = index[packageFqName]?.get(topLevelName) ?: return emptyList()

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
        debugLogFile?.appendText("findClasses: ${request.classId} ->\n  ${result.joinToString("\n  ") { it.classId?.asFqNameString() ?: it.name.asString() } }\n")
        return result
    }

    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? {
        if (!index.containsKey(fqName)) return null
        return packageCache.getOrPut(fqName) { JavaPackageOverAst(fqName, this) }
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String>? {
        val byName = index[packageFqName] ?: return emptySet()
        // TODO: this is the place there we can fix KT-4455, so it worth revisiting it later
        // Only return canonical class names (where the class name matches the file's basename).
        // Secondary classes (e.g., class B defined alongside class A in A.java) are accessible
        // when their ClassId is known directly (e.g., as return type from the same file's API)
        // but are NOT exposed as standalone same-package names. This matches PSI behavior for
        // KT-4455: non-canonical Java classes are not visible as independent types from Kotlin.
        return byName.entries
            .filter { (name, fileEntries) ->
                fileEntries.any { entry ->
                    entry.path.fileName.toString().removeSuffix(".java") == name
                }
            }
            .map { it.key }
            .toSet()
    }

    override fun canComputeKnownClassNamesInPackage(): Boolean = true

    private fun buildIndex() {
        for (root in sourceRoots) {
            if (!Files.exists(root)) continue
            Files.walk(root).use { stream ->
                stream.filter { it.isRegularFile() && it.fileName.toString().endsWith(".java") }
                    .forEach { path ->
                        // Special handling for package-info.java — extract package-level annotations
                        if (path.fileName.toString() == "package-info.java") {
                            indexPackageInfo(path)
                            return@forEach
                        }
                        val entry = tryBuildFileEntry(path) ?: return@forEach
                        val byName = index.getOrPut(entry.packageFqName) { ConcurrentHashMap() }
                        for (name in entry.topLevelClassNames) {
                            val list = byName.getOrPut(name) { mutableListOf() }
                            list.add(entry)
                        }
                    }
            }
        }
    }

    private fun indexPackageInfo(path: Path) {
        val source = tryReadFile(path) ?: return
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)

        val packageStmt = root.findChildByType("PACKAGE_STATEMENT")
        val packageName = (packageStmt ?: root).findChildByType("JAVA_CODE_REFERENCE")?.text ?: return
        val packageFqName = FqName(packageName)

        val resolutionContext = JavaResolutionContext.create(root, classFinderProvider = { this })
        val annotations = mutableListOf<JavaAnnotation>()

        // Annotations are in PACKAGE_STATEMENT → MODIFIER_LIST → ANNOTATION (KMP parser structure).
        // Also check other plausible locations for robustness.
        packageStmt?.findChildByType("MODIFIER_LIST")?.getChildrenByType("ANNOTATION")
            ?.mapTo(annotations) { JavaAnnotationOverAst(it, resolutionContext) }
        packageStmt?.getChildrenByType("ANNOTATION")
            ?.mapTo(annotations) { JavaAnnotationOverAst(it, resolutionContext) }
        root.getChildrenByType("ANNOTATION").mapTo(annotations) { JavaAnnotationOverAst(it, resolutionContext) }
        root.findChildByType("MODIFIER_LIST")?.getChildrenByType("ANNOTATION")
            ?.mapTo(annotations) { JavaAnnotationOverAst(it, resolutionContext) }

        if (annotations.isNotEmpty()) {
            packageAnnotationNodes.getOrPut(packageFqName) { mutableListOf() }.addAll(annotations)
        }
    }

    internal fun getPackageAnnotations(packageFqName: FqName): List<JavaAnnotation> =
        packageAnnotationNodes[packageFqName] ?: emptyList()

    private fun tryBuildFileEntry(path: Path): FileEntry? {
        val fileSize = try { Files.size(path) } catch (_: IOException) { return null }

        return if (fileSize <= SMALL_FILE_SIZE_THRESHOLD) {
            tryBuildFileEntryWithFullParse(path)
        } else {
            tryBuildFileEntryLightweight(path)
        }
    }

    /**
     * Small-file path: parse the file fully and cache all [JavaClassOverAst] instances
     * so that subsequent [findClass] calls are pure cache hits.
     */
    private fun tryBuildFileEntryWithFullParse(path: Path): FileEntry? {
        val source = tryReadFile(path) ?: return null
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)

        val packageStmt = root.findChildByType("PACKAGE_STATEMENT")
        val packageName = packageStmt?.findChildByType("JAVA_CODE_REFERENCE")?.text
        val packageFqName = if (packageName != null) FqName(packageName) else FqName.ROOT

        val classNames = root.getChildrenByType("CLASS").mapNotNull { node ->
            node.findChildByType("IDENTIFIER")?.text
        }.toSet()

        if (classNames.isEmpty()) return null

        val fileBaseName = path.fileName.toString().removeSuffix(".java")
        if (!classNames.contains(fileBaseName)) return null

        // Eagerly create and cache all top-level JavaClass instances.
        // Uses the resolution context's localClassProvider to ensure the same instances
        // are shared between the class cache and the context's local class cache
        // (FIR matches type parameters by object identity).
        val resolutionContext = JavaResolutionContext.create(root, classFinderProvider = { this })
        for (className in classNames) {
            val classId = ClassId(packageFqName, FqName(className), isLocal = false)
            if (classId !in classCache) {
                val javaClass = resolutionContext.findLocalClass(Name.identifier(className))
                if (javaClass != null) {
                    classCache[classId] = javaClass
                }
            }
        }

        return FileEntry(path, packageFqName, classNames)
    }

    /**
     * Large-file path: extract package name and top-level class names by scanning
     * the file line by line without invoking the parser.
     * The full parse is deferred to [parseTopLevelClassFromFile] on first access.
     */
    private fun tryBuildFileEntryLightweight(path: Path): FileEntry? {
        val info = extractFileInfoLightweight(path) ?: return null
        val packageFqName = if (info.packageName != null) FqName(info.packageName) else FqName.ROOT

        // Only create an entry when the file's base name matches at least one declared class.
        // This matches PSI's behavior per KT-4455: a file like "E.java" that only declares
        // class "F" (no class "E") is not indexed — the classes it contains are not accessible
        // to the compiler unless they appear as return/parameter types within the same .java file.
        val fileBaseName = path.fileName.toString().removeSuffix(".java")
        if (!info.topLevelClassNames.contains(fileBaseName)) return null

        return FileEntry(path, packageFqName, info.topLevelClassNames)
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

        val source = tryReadFile(file.path) ?: return null
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)
        val resolutionContext = JavaResolutionContext.create(root, classFinderProvider = { this })

        // Cache ALL top-level classes from this file to avoid re-parsing for sibling classes.
        // Uses findLocalClass to ensure consistent instances with the context's local class cache.
        val allClassNames = root.getChildrenByType("CLASS").mapNotNull {
            it.findChildByType("IDENTIFIER")?.text
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

    // TODO: check the uses; the io errors shoulbe probably propagated
    private fun tryReadFile(path: Path): CharSequence? = try {
        path.toFile().readText()
    } catch (_: IOException) {
        null
    }

    internal fun classesInPackage(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<JavaClass> {
        val byName = index[fqName] ?: return emptyList()
        val result = mutableListOf<JavaClass>()
        for ((simpleName, files) in byName) {
            val name = Name.identifier(simpleName)
            if (!nameFilter(name)) continue
            for (file in files) {
                parseTopLevelClassFromFile(file, simpleName)?.let { result.add(it) }
            }
        }
        return result
    }

    internal fun subPackagesOf(fqName: FqName): Collection<FqName> {
        if (fqName.isRoot) {
            // immediate top-level packages present in index
            val top = mutableSetOf<FqName>()
            for (pkg in index.keys) {
                val first = pkg.pathSegments().firstOrNull() ?: continue
                top.add(FqName(first.asString()))
            }
            return top
        }
        val prefix = fqName.asString() + "."
        val direct = mutableSetOf<FqName>()
        for (pkg in index.keys) {
            if (pkg.asString().startsWith(prefix) && pkg != fqName) {
                val tail = pkg.asString().removePrefix(prefix)
                val firstSegment = tail.substringBefore('.', missingDelimiterValue = tail)
                if (firstSegment.isNotEmpty()) direct.add(FqName(prefix + firstSegment))
            }
        }
        return direct
    }

    /**
     * Returns the direct supertype [ClassId]s for a class (only same-package source classes).
     * Prefers the cached [JavaClass] instance to avoid re-parsing. Falls back to file parsing
     * only when the class hasn't been cached yet.
     */
    internal fun getDirectSupertypes(classId: ClassId): List<ClassId> {
        return supertypeCache.getOrPut(classId) {
            val packageFqName = classId.packageFqName

            // Fast path: use the cached JavaClassOverAst's AST node directly.
            // IMPORTANT: we read raw JAVA_CODE_REFERENCE text from the node, NOT classifierQualifiedName,
            // because the latter triggers resolution which can circle back into getDirectSupertypes
            // via findInnerClassFromSupertypes → collectInheritedInnerClasses.
            val cachedClass = classCache[classId]
            if (cachedClass is JavaClassOverAst) {
                return@getOrPut extractSupertypeRefsFromNode(cachedClass.node, packageFqName)
            }

            // Slow path: parse the file (should be rare after indexing improvements)
            val files = findFilesForClass(classId)
            if (files.isEmpty()) return@getOrPut emptyList()

            val file = files.first()
            val source = tryReadFile(file.path) ?: return@getOrPut emptyList()
            val builder = parseJavaToSyntaxTreeBuilder(source, 0)
            val root = buildSyntaxTree(builder, source)

            val classNode = findClassInTree(root, classId) ?: return@getOrPut emptyList()
            extractSupertypeRefsFromNode(classNode, packageFqName)
        }
    }

    /**
     * Extracts supertype [ClassId]s from extends/implements clauses of an AST node.
     * Uses raw text from JAVA_CODE_REFERENCE nodes — no type resolution involved.
     */
    private fun extractSupertypeRefsFromNode(classNode: JavaSyntaxNode, packageFqName: FqName): List<ClassId> {
        val supertypes = mutableListOf<ClassId>()
        classNode.findChildByType("EXTENDS_LIST")
            ?.getChildrenByType("JAVA_CODE_REFERENCE")
            ?.forEach { ref ->
                resolveSupertypeReference(ref.text, packageFqName)?.let {
                    supertypes.add(it)
                }
            }
        classNode.findChildByType("IMPLEMENTS_LIST")
            ?.getChildrenByType("JAVA_CODE_REFERENCE")
            ?.forEach { ref ->
                resolveSupertypeReference(ref.text, packageFqName)?.let {
                    supertypes.add(it)
                }
            }
        return supertypes
    }

    /**
     * Recursively collects all inner class names from the supertype hierarchy.
     * Returns Map<simpleName, Set<ClassId>> to detect ambiguities.
     */
    internal fun collectInheritedInnerClasses(classId: ClassId): Map<String, Set<ClassId>> {
        val result = mutableMapOf<String, MutableSet<ClassId>>()
        val visited = mutableSetOf<ClassId>()

        // shadowedNames: inner class names declared by closer classes in the current inheritance path.
        // Per JLS 8.5, a member type declared in a subclass shadows same-named types from supertypes.
        // Example: if B extends A and both declare Inner, then from C extends B, B.Inner shadows A.Inner.
        // Only inner class names from UNRELATED paths that can't shadow each other indicate ambiguity.
        fun collectRecursive(current: ClassId, shadowedNames: Set<String>) {
            if (current in visited) return
            visited.add(current)

            val innerClasses = getInnerClassNames(current)
            for (innerName in innerClasses) {
                // Don't report names already declared by a closer class in this path (they're shadowed)
                if (innerName !in shadowedNames) {
                    val innerClassId = current.createNestedClassId(Name.identifier(innerName))
                    result.getOrPut(innerName) { mutableSetOf() }.add(innerClassId)
                }
            }

            // This class's inner class names shadow same-named types from its own supertypes
            val shadowedByThisClass = shadowedNames + innerClasses
            for (supertypeId in getDirectSupertypes(current)) {
                collectRecursive(supertypeId, shadowedByThisClass)
            }
        }

        collectRecursive(classId, emptySet())
        return result
    }

    private fun findFilesForClass(classId: ClassId): List<FileEntry> {
        val packageFqName = classId.packageFqName
        val topLevelName = classId.relativeClassName.pathSegments().firstOrNull()?.asString()
            ?: return emptyList()
        return index[packageFqName]?.get(topLevelName) ?: emptyList()
    }

    private fun findClassInTree(root: JavaSyntaxNode, classId: ClassId): JavaSyntaxNode? {
        val segments = classId.relativeClassName.pathSegments().map { it.asString() }
        if (segments.isEmpty()) return null

        var currentNode: JavaSyntaxNode = root
        for (segment in segments) {
            val classNode = currentNode.getChildrenByType("CLASS").firstOrNull { node ->
                node.findChildByType("IDENTIFIER")?.text == segment
            } ?: return null
            currentNode = classNode
        }
        return currentNode
    }

    private fun resolveSupertypeReference(ref: String, packageFqName: FqName): ClassId? {
        val simpleName = ref.substringBefore('<').trim()

        if (!simpleName.contains('.')) {
            val samePackageId = ClassId(packageFqName, Name.identifier(simpleName))
            if (index[packageFqName]?.containsKey(simpleName) == true) {
                return samePackageId
            }
        }

        return null
    }

    private fun getInnerClassNames(classId: ClassId): Set<String> {
        // Fast path: use the cached JavaClass (no file I/O, no parsing)
        val cachedClass = classCache[classId]
        if (cachedClass != null) {
            return cachedClass.innerClassNames.map { it.asString() }.toSet()
        }

        // Slow path: parse the file (should be rare after indexing improvements)
        val files = findFilesForClass(classId)
        if (files.isEmpty()) return emptySet()

        val file = files.first()
        val source = tryReadFile(file.path) ?: return emptySet()
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)

        val classNode = findClassInTree(root, classId) ?: return emptySet()

        return classNode.children
            .filter { it.type.toString() == "CLASS" }
            .mapNotNull { it.findChildByType("IDENTIFIER")?.text }
            .toSet()
    }
}
