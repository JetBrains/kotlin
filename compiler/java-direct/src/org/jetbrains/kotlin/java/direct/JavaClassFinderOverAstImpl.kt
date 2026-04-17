/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
 * Files at or below this size are parsed fully during [JavaClassFinderOverAstImpl.buildIndex],
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
    private val sourceRoots: List<VirtualFile>,
    private val debugLogFilePath: Path? = null,
    private val sourceFileReader: JavaSourceFileReader = DefaultJavaSourceFileReader,
) : JavaClassFinder {

    private data class FileEntry(
        val file: VirtualFile,
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

    // Supertype graph queries (direct supertypes, inherited inner classes).
    // Extracted into a focused collaborator in Step 1.6 of the refactoring plan.
    private val supertypeGraph = JavaSupertypeGraph(
        classCacheLookup = { classCache[it] },
        filesForClassLookup = { classId -> findFilesForClass(classId).map { it.file } },
        sameClassInSameFilePackage = { pkg, name -> index[pkg]?.containsKey(name) == true },
        sourceFileReader = sourceFileReader,
    )

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
        debugLogFile?.appendText("findClasses: ${request.classId} ->\n  ${result.joinToString("\n  ") { it.classId?.asFqNameString() ?: it.name.asString() }}\n")
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
                    entry.file.name.removeSuffix(".java") == name
                }
            }
            .map { it.key }
            .toSet()
    }

    override fun canComputeKnownClassNamesInPackage(): Boolean = true

    private fun buildIndex() {
        for (file in sourceFileReader.walkSourceRoots(sourceRoots)) {
            // Special handling for package-info.java — extract package-level annotations
            if (file.name == "package-info.java") {
                indexPackageInfo(file)
                continue
            }
            val entry = tryBuildFileEntry(file) ?: continue
            val byName = index.getOrPut(entry.packageFqName) { ConcurrentHashMap() }
            for (name in entry.topLevelClassNames) {
                val list = byName.getOrPut(name) { mutableListOf() }
                list.add(entry)
            }
        }
    }

    private fun indexPackageInfo(file: VirtualFile) {
        val source = sourceFileReader.readFileContent(file) ?: return
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)

        val packageStmt = root.findChildByType(JavaSyntaxElementType.PACKAGE_STATEMENT)
        val packageName = (packageStmt ?: root).findChildByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.text ?: return
        val packageFqName = FqName(packageName)

        val resolutionContext = JavaResolutionContext.create(root, classFinderProvider = { this })
        val annotations = mutableListOf<JavaAnnotation>()

        // Annotations are in PACKAGE_STATEMENT → MODIFIER_LIST → ANNOTATION (KMP parser structure).
        // Also check other plausible locations for robustness.
        packageStmt?.findChildByType(JavaSyntaxElementType.MODIFIER_LIST)?.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
            ?.mapTo(annotations) { JavaAnnotationOverAst(it, resolutionContext) }
        packageStmt?.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
            ?.mapTo(annotations) { JavaAnnotationOverAst(it, resolutionContext) }
        root.getChildrenByType(JavaSyntaxElementType.ANNOTATION).mapTo(annotations) { JavaAnnotationOverAst(it, resolutionContext) }
        root.findChildByType(JavaSyntaxElementType.MODIFIER_LIST)?.getChildrenByType(JavaSyntaxElementType.ANNOTATION)
            ?.mapTo(annotations) { JavaAnnotationOverAst(it, resolutionContext) }

        if (annotations.isNotEmpty()) {
            packageAnnotationNodes.getOrPut(packageFqName) { mutableListOf() }.addAll(annotations)
        }
    }

    internal fun getPackageAnnotations(packageFqName: FqName): List<JavaAnnotation> =
        packageAnnotationNodes[packageFqName] ?: emptyList()

    private fun tryBuildFileEntry(file: VirtualFile): FileEntry? {
        val fileSize = file.length
        return if (fileSize <= SMALL_FILE_SIZE_THRESHOLD) {
            tryBuildFileEntryWithFullParse(file)
        } else {
            tryBuildFileEntryLightweight(file)
        }
    }

    /**
     * Small-file path: parse the file fully and cache all [JavaClassOverAst] instances
     * so that subsequent [findClass] calls are pure cache hits.
     */
    private fun tryBuildFileEntryWithFullParse(file: VirtualFile): FileEntry? {
        val source = sourceFileReader.readFileContent(file) ?: return null
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)

        val packageStmt = root.findChildByType(JavaSyntaxElementType.PACKAGE_STATEMENT)
        val packageName = packageStmt?.findChildByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.text
        val packageFqName = if (packageName != null) FqName(packageName) else FqName.ROOT

        val classNames = root.getChildrenByType(JavaSyntaxElementType.CLASS).mapNotNull { node ->
            node.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.text
        }.toSet()

        if (classNames.isEmpty()) return null

        val fileBaseName = file.name.removeSuffix(".java")
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

        return FileEntry(file, packageFqName, classNames)
    }

    /**
     * Large-file path: extract package name and top-level class names by scanning
     * the file line by line without invoking the parser.
     * The full parse is deferred to [parseTopLevelClassFromFile] on first access.
     */
    private fun tryBuildFileEntryLightweight(file: VirtualFile): FileEntry? {
        val info = extractFileInfoLightweight(file, sourceFileReader) ?: return null
        val packageFqName = if (info.packageName != null) FqName(info.packageName) else FqName.ROOT

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
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)
        val resolutionContext = JavaResolutionContext.create(root, classFinderProvider = { this })

        // Cache ALL top-level classes from this file to avoid re-parsing for sibling classes.
        // Uses findLocalClass to ensure consistent instances with the context's local class cache.
        val allClassNames = root.getChildrenByType(JavaSyntaxElementType.CLASS).mapNotNull {
            it.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.text
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
        val packageFqName = classId.packageFqName
        val topLevelName = classId.relativeClassName.pathSegments().firstOrNull()?.asString()
            ?: return emptyList()
        return index[packageFqName]?.get(topLevelName) ?: emptyList()
    }
}
