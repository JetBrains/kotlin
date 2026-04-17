/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

/**
 * Encapsulates supertype-graph queries for Java source classes: direct supertypes and
 * transitively inherited inner class names.
 *
 * The graph is computed lazily from the AST, preferring already-cached [JavaClass] instances
 * (fast path, no I/O). When a class hasn't been cached yet, the owning file is re-parsed as a
 * fallback (slow path). Results are memoized in per-instance caches.
 *
 * This component intentionally does NOT own the source index or the class cache — it consults
 * them via callbacks passed to the constructor, so that a single authoritative copy lives in
 * [JavaClassFinderOverAstImpl].
 *
 * @param classCacheLookup returns the cached [JavaClass] for a given [ClassId], or `null` if
 *     it hasn't been parsed/cached yet.
 * @param filesForClassLookup returns the candidate source files that may contain the top-level
 *     class identified by the given [ClassId].
 * @param sameClassInSameFilePackage returns whether the given simple name exists as a top-level
 *     class in the given package in the source index (for same-package supertype resolution).
 * @param sourceFileReader reader used to fetch the text of a [VirtualFile] on the slow path.
 */
internal class JavaSupertypeGraph(
    private val classCacheLookup: (ClassId) -> JavaClass?,
    private val filesForClassLookup: (ClassId) -> List<VirtualFile>,
    private val sameClassInSameFilePackage: (FqName, String) -> Boolean,
    private val sourceFileReader: JavaSourceFileReader,
) {
    // Cache: ClassId -> list of supertype ClassIds (direct only)
    private val supertypeCache: MutableMap<ClassId, List<ClassId>> = ConcurrentHashMap()

    // Cache: ClassId -> Map<simpleName, Set<ClassId>> for inherited inner classes
    private val inheritedInnerClassesCache: MutableMap<ClassId, Map<String, Set<ClassId>>> = ConcurrentHashMap()

    /**
     * Returns the direct supertype [ClassId]s for a class.
     * Prefers the cached [JavaClass] instance to avoid re-parsing. Falls back to file parsing
     * only when the class hasn't been cached yet.
     * Resolves cross-package supertypes via imports from the file.
     */
    fun getDirectSupertypes(classId: ClassId): List<ClassId> {
        return supertypeCache.getOrPut(classId) {
            val packageFqName = classId.packageFqName

            // Fast path: use the cached JavaClassOverAst's AST node directly.
            // IMPORTANT: we read raw JAVA_CODE_REFERENCE text from the node, NOT classifierQualifiedName,
            // because the latter triggers resolution which can circle back into getDirectSupertypes
            // via findInnerClassFromSupertypes → collectInheritedInnerClasses.
            val cachedClass = classCacheLookup(classId)
            if (cachedClass is JavaClassOverAst) {
                val (simpleImports, starImports) = cachedClass.resolutionContext.getImports()
                return@getOrPut extractSupertypeRefsFromNode(cachedClass.node, packageFqName, simpleImports, starImports)
            }

            // Slow path: parse the file (should be rare after indexing improvements)
            val files = filesForClassLookup(classId)
            if (files.isEmpty()) return@getOrPut emptyList()

            val file = files.first()
            val source = sourceFileReader.readFileContent(file) ?: return@getOrPut emptyList()
            val builder = parseJavaToSyntaxTreeBuilder(source, 0)
            val root = buildSyntaxTree(builder, source)

            // Assuming this is a rare case, when we need to get import before `parseTopLevelClassFromFile`, which extracts imports too
            // TODO: check if this is rare enore
            val (simpleImports, starImports) = JavaResolutionContext.extractImports(root)
            val classNode = findClassInTree(root, classId) ?: return@getOrPut emptyList()
            extractSupertypeRefsFromNode(classNode, packageFqName, simpleImports, starImports)
        }
    }

    /**
     * Recursively collects all inner class names from the supertype hierarchy.
     * Returns Map<simpleName, Set<ClassId>> to detect ambiguities.
     */
    fun collectInheritedInnerClasses(classId: ClassId): Map<String, Set<ClassId>> {
        inheritedInnerClassesCache[classId]?.let { return it }

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
        val immutableResult: Map<String, Set<ClassId>> = result.mapValues { it.value.toSet() }
        inheritedInnerClassesCache[classId] = immutableResult
        return immutableResult
    }

    private fun getInnerClassNames(classId: ClassId): Set<String> {
        // Fast path: use the cached JavaClass (no file I/O, no parsing)
        val cachedClass = classCacheLookup(classId)
        if (cachedClass != null) {
            return cachedClass.innerClassNames.map { it.asString() }.toSet()
        }

        // Slow path: parse the file (should be rare after indexing improvements)
        val files = filesForClassLookup(classId)
        if (files.isEmpty()) return emptySet()

        val file = files.first()
        val source = sourceFileReader.readFileContent(file) ?: return emptySet()
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)

        val classNode = findClassInTree(root, classId) ?: return emptySet()

        return classNode.children
            .filter { it.type == JavaSyntaxElementType.CLASS }
            .mapNotNull { it.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.text }
            .toSet()
    }

    /**
     * Extracts supertype [ClassId]s from extends/implements clauses of an AST node.
     * Uses raw text from JAVA_CODE_REFERENCE nodes — no type resolution involved.
     */
    private fun extractSupertypeRefsFromNode(
        classNode: JavaSyntaxNode,
        packageFqName: FqName,
        simpleImports: Map<String, FqName> = emptyMap(),
        starImports: List<FqName> = emptyList(),
    ): List<ClassId> {
        val supertypes = mutableListOf<ClassId>()
        classNode.findChildByType(JavaSyntaxElementType.EXTENDS_LIST)
            ?.getChildrenByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE)
            ?.forEach { ref ->
                resolveSupertypeReference(ref.text, packageFqName, simpleImports, starImports)?.let {
                    supertypes.add(it)
                }
            }
        classNode.findChildByType(JavaSyntaxElementType.IMPLEMENTS_LIST)
            ?.getChildrenByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE)
            ?.forEach { ref ->
                resolveSupertypeReference(ref.text, packageFqName, simpleImports, starImports)?.let {
                    supertypes.add(it)
                }
            }
        return supertypes
    }

    private fun findClassInTree(root: JavaSyntaxNode, classId: ClassId): JavaSyntaxNode? {
        val segments = classId.relativeClassName.pathSegments().map { it.asString() }
        if (segments.isEmpty()) return null

        var currentNode: JavaSyntaxNode = root
        for (segment in segments) {
            val classNode = currentNode.getChildrenByType(JavaSyntaxElementType.CLASS).firstOrNull { node ->
                node.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.text == segment
            } ?: return null
            currentNode = classNode
        }
        return currentNode
    }

    private fun resolveSupertypeReference(
        ref: String,
        packageFqName: FqName,
        simpleImports: Map<String, FqName> = emptyMap(),
        starImports: List<FqName> = emptyList(),
    ): ClassId? {
        val simpleName = ref.substringBefore('<').trim()

        if (!simpleName.contains('.')) {
            // 1. Same-package lookup
            if (sameClassInSameFilePackage(packageFqName, simpleName)) {
                return ClassId(packageFqName, Name.identifier(simpleName))
            }

            // 2. Explicit import lookup (e.g., import base.FunctionDescriptor)
            val explicitFqName = simpleImports[simpleName]
            if (explicitFqName != null) {
                val importPkg = explicitFqName.parent()
                val importName = explicitFqName.shortName().asString()
                if (sameClassInSameFilePackage(importPkg, importName)) {
                    return ClassId(importPkg, Name.identifier(importName))
                }
            }

            // 3. Star import lookup (e.g., import base.*)
            for (starPkg in starImports) {
                if (sameClassInSameFilePackage(starPkg, simpleName)) {
                    return ClassId(starPkg, Name.identifier(simpleName))
                }
            }
        }

        return null
    }
}
