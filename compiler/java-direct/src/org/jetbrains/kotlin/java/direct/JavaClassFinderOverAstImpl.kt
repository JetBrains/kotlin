/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.isRegularFile

/**
 * A simple JavaClassFinder implementation over the direct Java AST parser used in this module.
 *
 * It scans provided [sourceRoots] for `.java` files, indexes packages and top-level class names,
 * and lazily parses files to produce [JavaClassOverAst] instances. Results are cached by [ClassId].
 */
class JavaClassFinderOverAstImpl(
    private val sourceRoots: List<Path>,
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

    // Cache: ClassId -> list of supertype ClassIds (direct only)
    private val supertypeCache: MutableMap<ClassId, List<ClassId>> = ConcurrentHashMap()

    init {
        buildIndex()
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
        return result
    }

    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? {
        if (!index.containsKey(fqName)) return null
        return packageCache.getOrPut(fqName) { JavaPackageOverAst(fqName, this) }
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String>? {
        val byName = index[packageFqName]
        if (byName != null) return byName.keys
        return emptySet()
    }

    override fun canComputeKnownClassNamesInPackage(): Boolean = true

    private fun buildIndex() {
        for (root in sourceRoots) {
            if (!Files.exists(root)) continue
            Files.walk(root).use { stream ->
                stream.filter { it.isRegularFile() && it.fileName.toString().endsWith(".java") }
                    .forEach { path ->
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

    private fun tryBuildFileEntry(path: Path): FileEntry? {
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
        return FileEntry(path, packageFqName, classNames)
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
        val node = root.getChildrenByType("CLASS").firstOrNull { n ->
            n.findChildByType("IDENTIFIER")?.text == simpleName
        } ?: return null
        return JavaClassOverAst(node, resolutionContext, outerClass = null).also {
            classCache[classId] = it
        }
    }

    private fun tryReadFile(path: Path): CharSequence? = try {
        Files.newByteChannel(path, StandardOpenOption.READ).use { ch ->
            val bytes = ByteArray(ch.size().toInt())
            Files.readAllBytes(path)
        }
        // simpler approach
        Files.readString(path, StandardCharsets.UTF_8)
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
     * Parses just enough of a class to extract its direct supertypes.
     * This is lightweight - only reads extends/implements clauses.
     */
    internal fun getDirectSupertypes(classId: ClassId): List<ClassId> {
        return supertypeCache.getOrPut(classId) {
            val files = findFilesForClass(classId)
            if (files.isEmpty()) return@getOrPut emptyList()

            val file = files.first()
            val source = tryReadFile(file.path) ?: return@getOrPut emptyList()
            val builder = parseJavaToSyntaxTreeBuilder(source, 0)
            val root = buildSyntaxTree(builder, source)

            val packageFqName = extractPackageName(root)
            val classNode = findClassInTree(root, classId) ?: return@getOrPut emptyList()

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

            supertypes
        }
    }

    /**
     * Recursively collects all inner class names from the supertype hierarchy.
     * Returns Map<simpleName, Set<ClassId>> to detect ambiguities.
     */
    internal fun collectInheritedInnerClasses(classId: ClassId): Map<String, Set<ClassId>> {
        val result = mutableMapOf<String, MutableSet<ClassId>>()
        val visited = mutableSetOf<ClassId>()

        fun collectRecursive(current: ClassId) {
            if (current in visited) return
            visited.add(current)

            val innerClasses = getInnerClassNames(current)
            for (innerName in innerClasses) {
                val innerClassId = current.createNestedClassId(Name.identifier(innerName))
                result.getOrPut(innerName) { mutableSetOf() }.add(innerClassId)
            }

            for (supertypeId in getDirectSupertypes(current)) {
                collectRecursive(supertypeId)
            }
        }

        collectRecursive(classId)
        return result
    }

    private fun findFilesForClass(classId: ClassId): List<FileEntry> {
        val packageFqName = classId.packageFqName
        val topLevelName = classId.relativeClassName.pathSegments().firstOrNull()?.asString()
            ?: return emptyList()
        return index[packageFqName]?.get(topLevelName) ?: emptyList()
    }

    private fun extractPackageName(root: JavaSyntaxNode): FqName {
        val packageStmt = root.findChildByType("PACKAGE_STATEMENT")
        val packageName = packageStmt?.findChildByType("JAVA_CODE_REFERENCE")?.text
        return if (packageName != null) FqName(packageName) else FqName.ROOT
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
