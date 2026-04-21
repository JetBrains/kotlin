/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

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
     *
     * Uses [java.util.concurrent.ConcurrentHashMap.computeIfAbsent] (not `getOrPut`) so that
     * concurrent callers do not both re-parse the same file or re-extract from the same AST.
     */
    fun getDirectSupertypes(classId: ClassId): List<ClassId> {
        return supertypeCache.computeIfAbsent(classId) {
            val packageFqName = classId.packageFqName

            // Fast path: use the cached JavaClassOverAst's AST node directly.
            // IMPORTANT: we read raw JAVA_CODE_REFERENCE text from the node, NOT classifierQualifiedName,
            // because the latter triggers resolution which can circle back into getDirectSupertypes
            // via findInnerClassFromSupertypes → collectInheritedInnerClasses.
            val cachedClass = classCacheLookup(classId)
            if (cachedClass is JavaClassOverAst) {
                val (simpleImports, starImports) = cachedClass.resolutionContext.getImports()
                return@computeIfAbsent extractSupertypeRefsFromNode(
                    cachedClass.tree, cachedClass.node, packageFqName, simpleImports, starImports
                )
            }

            // Slow path: re-parse the file to extract supertype references.
            val files = filesForClassLookup(classId)
            if (files.isEmpty()) return@computeIfAbsent emptyList()

            val file = files.first()
            val source = sourceFileReader.readFileContent(file) ?: return@computeIfAbsent emptyList()
            val tree = parseJavaToLightTree(source, 0)
            val root = tree.getRoot()

            val (simpleImports, starImports) = JavaResolutionContext.extractImports(tree, root)
            val classNode = findClassInTree(tree, root, classId) ?: return@computeIfAbsent emptyList()
            extractSupertypeRefsFromNode(tree, classNode, packageFqName, simpleImports, starImports)
        }
    }

    /**
     * Recursively collects all inner class names from the supertype hierarchy.
     * Returns Map<simpleName, Set<ClassId>> to detect ambiguities.
     *
     * Uses [java.util.concurrent.ConcurrentHashMap.computeIfAbsent] so that concurrent callers
     * do not both perform the recursive supertype walk for the same class. Nested recursion
     * reads the cache via plain `get` (not `computeIfAbsent`) so it cannot self-deadlock.
     */
    fun collectInheritedInnerClasses(classId: ClassId): Map<String, Set<ClassId>> {
        return inheritedInnerClassesCache.computeIfAbsent(classId) {
            val result = mutableMapOf<String, MutableSet<ClassId>>()
            val visited = mutableSetOf<ClassId>()

            // shadowedNames: inner class names declared by closer classes in the current inheritance path.
            // Per JLS 8.5, a member type declared in a subclass shadows same-named types from supertypes.
            // Example: if B extends A and both declare Inner, then from C extends B, B.Inner shadows A.Inner.
            // Only inner class names from UNRELATED paths that can't shadow each other indicate ambiguity.
            fun collectRecursive(current: ClassId, shadowedNames: Set<String>) {
                if (current in visited) return

                // Cache short-circuit: a previously computed result for [current] already reflects
                // intra-subtree shadowing (closer classes shadowing farther supertypes). The only
                // extra filtering we need is the [shadowedNames] coming from the caller's path.
                // Safe in diamond inheritance: merging via getOrPut + addAll is idempotent for
                // ClassId sets, and the `visited` guard above only affects duplicate traversal of
                // the same ClassId within a single top-level call — which the cache makes redundant.
                inheritedInnerClassesCache[current]?.let { cached ->
                    visited.add(current)
                    for ((name, classIds) in cached) {
                        if (name !in shadowedNames) {
                            result.getOrPut(name) { mutableSetOf() }.addAll(classIds)
                        }
                    }
                    return
                }

                visited.add(current)

                val innerClasses = getInnerClassNames(current)
                for (innerName in innerClasses) {
                    if (innerName !in shadowedNames) {
                        val innerClassId = current.createNestedClassId(Name.identifier(innerName))
                        result.getOrPut(innerName) { mutableSetOf() }.add(innerClassId)
                    }
                }

                val shadowedByThisClass = shadowedNames + innerClasses
                for (supertypeId in getDirectSupertypes(current)) {
                    collectRecursive(supertypeId, shadowedByThisClass)
                }
            }

            collectRecursive(classId, emptySet())
            result.mapValues { it.value.toSet() }
        }
    }

    private fun getInnerClassNames(classId: ClassId): Set<String> {
        // Fast path: use the cached JavaClass (no file I/O, no parsing)
        val cachedClass = classCacheLookup(classId)
        if (cachedClass != null) {
            val inner = cachedClass.innerClassNames
            if (inner.isEmpty()) return emptySet()
            return inner.mapTo(HashSet(inner.size)) { it.asString() }
        }

        // Slow path: re-parse for inner class names.
        val files = filesForClassLookup(classId)
        if (files.isEmpty()) return emptySet()

        val file = files.first()
        val source = sourceFileReader.readFileContent(file) ?: return emptySet()
        val tree = parseJavaToLightTree(source, 0)
        val root = tree.getRoot()

        val classNode = findClassInTree(tree, root, classId) ?: return emptySet()

        return tree.getChildren(classNode)
            .filter { tree.getType(it) == JavaSyntaxElementType.CLASS }
            .mapNotNull { tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() } }
            .toSet()
    }

    /**
     * Extracts supertype [ClassId]s from extends/implements clauses of an AST node.
     * Uses raw text from JAVA_CODE_REFERENCE nodes — no type resolution involved.
     */
    private fun extractSupertypeRefsFromNode(
        tree: JavaLightTree,
        classNode: JavaLightNode,
        packageFqName: FqName,
        simpleImports: Map<String, FqName> = emptyMap(),
        starImports: List<FqName> = emptyList(),
    ): List<ClassId> {
        val supertypes = mutableListOf<ClassId>()
        tree.findChildByType(classNode, JavaSyntaxElementType.EXTENDS_LIST)?.let { el ->
            tree.getChildrenByType(el, JavaSyntaxElementType.JAVA_CODE_REFERENCE).forEach { ref ->
                resolveSupertypeReference(tree.getText(ref).toString(), packageFqName, simpleImports, starImports)?.let {
                    supertypes.add(it)
                }
            }
        }
        tree.findChildByType(classNode, JavaSyntaxElementType.IMPLEMENTS_LIST)?.let { il ->
            tree.getChildrenByType(il, JavaSyntaxElementType.JAVA_CODE_REFERENCE).forEach { ref ->
                resolveSupertypeReference(tree.getText(ref).toString(), packageFqName, simpleImports, starImports)?.let {
                    supertypes.add(it)
                }
            }
        }
        return supertypes
    }

    private fun findClassInTree(tree: JavaLightTree, root: JavaLightNode, classId: ClassId): JavaLightNode? {
        val segments = classId.relativeClassName.pathSegments().map { it.asString() }
        if (segments.isEmpty()) return null

        var currentNode: JavaLightNode = root
        for (segment in segments) {
            val classNode = tree.getChildrenByType(currentNode, JavaSyntaxElementType.CLASS).firstOrNull { node ->
                tree.findChildByType(node, JavaSyntaxTokenType.IDENTIFIER)?.let { tree.textEquals(it, segment) } == true
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
            if (sameClassInSameFilePackage(packageFqName, simpleName)) {
                return ClassId(packageFqName, Name.identifier(simpleName))
            }

            val explicitFqName = simpleImports[simpleName]
            if (explicitFqName != null) {
                val importPkg = explicitFqName.parent()
                val importName = explicitFqName.shortName().asString()
                if (sameClassInSameFilePackage(importPkg, importName)) {
                    return ClassId(importPkg, Name.identifier(importName))
                }
            }

            for (starPkg in starImports) {
                if (sameClassInSameFilePackage(starPkg, simpleName)) {
                    return ClassId(starPkg, Name.identifier(simpleName))
                }
            }
        }

        return null
    }
}
