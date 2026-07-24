/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct.util

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import org.jetbrains.kotlin.java.direct.JavaClassCache
import org.jetbrains.kotlin.java.direct.JavaPackageIndexer
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.parse.parseJavaToLightTree
import org.jetbrains.kotlin.java.direct.resolution.JavaImportResolver
import org.jetbrains.kotlin.java.direct.resolution.JavaImports
import org.jetbrains.kotlin.java.direct.resolution.getImports
import org.jetbrains.kotlin.load.java.structure.impl.splitCanonicalFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

/**
 * Encapsulates supertype-graph queries for Java source classes: direct supertypes and
 * transitively inherited inner class names.
 *
 * The graph is computed lazily from the AST, preferring already-cached [org.jetbrains.kotlin.load.java.structure.JavaClass] instances
 * (fast path, no I/O). When a class hasn't been cached yet, the owning file is reparsed as a
 * fallback (slow path). Results are memoized in per-instance caches.
 *
 * @param packageIndexer source index consulted for candidate files of a top-level class
 *     ([JavaPackageIndexer.findFilesForClass]) and for same-package top-level class existence
 *     ([JavaPackageIndexer.ensurePackageIndexed]).
 * @param classCache cache of already-parsed [org.jetbrains.kotlin.load.java.structure.JavaClass]
 *     instances (fast path, no I/O).
 * @param sourceFileReader reader used to fetch the text of a source file on the slow path.
 */
internal class JavaSupertypeGraph(
    private val packageIndexer: JavaPackageIndexer,
    private val classCache: JavaClassCache,
    private val sourceFileReader: JavaSourceFileReader,
) {
    // Cache: ClassId -> list of supertype ClassIds (direct only)
    private val supertypeCache: MutableMap<ClassId, List<ClassId>> = ConcurrentHashMap()

    // Cache: ClassId -> Map<simpleName, Set<ClassId>> for inherited inner classes
    private val inheritedInnerClassesCache: MutableMap<ClassId, Map<String, Set<ClassId>>> = ConcurrentHashMap()

    /**
     * Returns the direct supertype [ClassId]s for a class.
     */
    fun getDirectSupertypes(classId: ClassId): List<ClassId> {
        return supertypeCache.computeIfAbsent(classId) {
            val packageFqName = classId.packageFqName

            // Fast path: use the cached JavaClassOverAst's AST node directly.
            // IMPORTANT: we read raw JAVA_CODE_REFERENCE text from the node, NOT classifierQualifiedName,
            // because the latter triggers full classifier resolution, which could recurse back into
            // this same computation for another class in the hierarchy.
            val cachedClass = classCache[classId]
            if (cachedClass is JavaClassOverAst) {
                val imports = with(cachedClass.resolutionContext) { getImports() }
                return@computeIfAbsent extractSupertypeRefsFromNode(
                    cachedClass.tree, cachedClass.node, packageFqName, imports
                )
            }

            // Slow path: reparse the file to extract supertype references.
            val file = packageIndexer.findFilesForClass(classId).firstOrNull()?.file ?: return@computeIfAbsent emptyList()
            val source = sourceFileReader.readFileContent(file) ?: return@computeIfAbsent emptyList()
            val tree = parseJavaToLightTree(source, 0)
            val root = tree.getRoot()

            val imports = JavaImportResolver.extractImports(tree, root)
            val classNode = findClassInTree(tree, root, classId) ?: return@computeIfAbsent emptyList()
            extractSupertypeRefsFromNode(tree, classNode, packageFqName, imports)
        }
    }

    /**
     * Recursively collects all inner class names from the supertype hierarchy.
     * Returns Map<simpleName, Set<ClassId>> to detect ambiguities.
     *
     * Nested recursion reads the cache via plain `get` (not `computeIfAbsent`) so it cannot self-deadlock.
     */
    fun collectInheritedInnerClasses(classId: ClassId): Map<String, Set<ClassId>> {
        return inheritedInnerClassesCache.computeIfAbsent(classId) {
            val result = mutableMapOf<String, MutableSet<ClassId>>()
            val visited = mutableSetOf<ClassId>()

            // shadowedNames: inner class names declared by closer classes in the current path.
            // Per JLS 8.5 a member type declared in a subclass shadows same-named supertype ones;
            // only names from unrelated paths that cannot shadow each other indicate ambiguity.
            fun collectRecursive(current: ClassId, shadowedNames: Set<String>) {
                if (current in visited) return

                // Cache short-circuit: a previously computed result already reflects intra-subtree
                // shadowing; only the caller-path [shadowedNames] filter still needs applying.
                inheritedInnerClassesCache[current]?.let { cached ->
                    visited.add(current)
                    for ([name, classIds] in cached) {
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
            result
        }
    }

    private fun getInnerClassNames(classId: ClassId): Set<String> {
        // Fast path: use the cached JavaClass (no file I/O, no parsing)
        val cachedClass = classCache[classId]
        if (cachedClass != null) {
            val inner = cachedClass.innerClassNames
            if (inner.isEmpty()) return emptySet()
            return inner.mapTo(HashSet(inner.size)) { it.asString() }
        }

        // Slow path: reparse for inner class names.
        val file = packageIndexer.findFilesForClass(classId).firstOrNull()?.file ?: return emptySet()
        val source = sourceFileReader.readFileContent(file) ?: return emptySet()
        val tree = parseJavaToLightTree(source, 0)
        val root = tree.getRoot()

        val classNode = findClassInTree(tree, root, classId) ?: return emptySet()

        return tree.getChildren(classNode)
            .mapNotNullTo(mutableSetOf()) {
                if (tree.getType(it) == JavaSyntaxElementType.CLASS)
                    tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() }
                else null
            }
    }

    /**
     * Extracts supertype [ClassId]s from extends/implements clauses of an AST node.
     * Uses raw text from JAVA_CODE_REFERENCE nodes — no type resolution involved.
     */
    private fun extractSupertypeRefsFromNode(
        tree: JavaLightTree,
        classNode: JavaLightNode,
        packageFqName: FqName,
        imports: JavaImports = JavaImports.EMPTY,
    ): List<ClassId> {
        val supertypes = mutableListOf<ClassId>()
        tree.findChildByType(classNode, JavaSyntaxElementType.EXTENDS_LIST)?.let { el ->
            tree.getChildrenByType(el, JavaSyntaxElementType.JAVA_CODE_REFERENCE).forEach { ref ->
                supertypes.addAll(resolveSupertypeReference(tree.getText(ref).toString(), packageFqName, imports))
            }
        }
        tree.findChildByType(classNode, JavaSyntaxElementType.IMPLEMENTS_LIST)?.let { il ->
            tree.getChildrenByType(il, JavaSyntaxElementType.JAVA_CODE_REFERENCE).forEach { ref ->
                supertypes.addAll(resolveSupertypeReference(tree.getText(ref).toString(), packageFqName, imports))
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

    /**
     * Returns one or more *candidate* [ClassId]s for a supertype reference in extends/implements.
     * Candidates are not filtered against a FIR session at this layer; callers disambiguate via
     * their own `tryResolve`-style probes. Multiple candidates arise only for star imports and
     * ambiguous package/class splits.
     */
    private fun resolveSupertypeReference(
        ref: String,
        packageFqName: FqName,
        imports: JavaImports = JavaImports.EMPTY,
    ): List<ClassId> {
        // splitCanonicalFqName treats a '.' inside a generic argument list as part of that
        // segment, not a separator, so a reference with type arguments on a non-final segment
        // (`a.B<String>.C`) is correctly recognised as dotted and delegated below.
        val segments = ref.splitCanonicalFqName()
        val simpleName = segments.singleOrNull()?.substringBefore('<')?.trim()

        if (simpleName != null) {
            // Same-package source class — resolves to the in-module ClassId.
            if (packageIndexer.ensurePackageIndexed(packageFqName).containsKey(simpleName)) {
                return listOf(ClassId(packageFqName, Name.identifier(simpleName)))
            }

            // JLS 6.4.1 rank-4 type-then-static single imports. Emit all longest-package-first
            // splits so a nested-class explicit import such as `import a.b.C.D;` produces both
            // `ClassId(a.b, C.D)` and `ClassId(a.b.C, D)`.
            val explicitFqName = imports.simpleTypeImports[simpleName] ?: imports.staticSingleImports[simpleName]
            if (explicitFqName != null) {
                return fqNameSplitCandidates(explicitFqName)
            }

            // JLS 7.5.2 type-import-on-demand (rank 6) — entries are *packages*. Source
            // candidates first when present; otherwise binary candidates (one per star-import
            // package) are emitted so the caller can probe supertypes that live on the classpath.
            val candidates = mutableListOf<ClassId>()
            for (starPkg in imports.typeStarImports) {
                if (packageIndexer.ensurePackageIndexed(starPkg).containsKey(simpleName)) {
                    candidates.add(ClassId(starPkg, Name.identifier(simpleName)))
                }
            }
            if (candidates.isNotEmpty()) return candidates
            val typeStarCandidates = imports.typeStarImports.map { ClassId(it, Name.identifier(simpleName)) }
            if (typeStarCandidates.isNotEmpty()) return typeStarCandidates

            // JLS 7.5.4 static-import-on-demand (rank 7) — entries are *outer-class* FqNames;
            // the package/class split is unknown here, so emit every plausible split of
            // `outerFqName + simpleName`.
            if (imports.staticStarImports.isNotEmpty()) {
                val staticStarCandidates = mutableListOf<ClassId>()
                for (outerFqName in imports.staticStarImports) {
                    staticStarCandidates += fqNameSplitCandidates(outerFqName.child(Name.identifier(simpleName)))
                }
                if (staticStarCandidates.isNotEmpty()) return staticStarCandidates
            }
            return emptyList()
        }

        // Dotted form is delegated to JavaResolutionContext.resolve; this candidate-layer
        // shortcut covers same-package / explicit-import / star-import only.
        return emptyList()
    }

    // Emit candidate ClassIds for an imported FqName, longest-package-first (mirrors
    // resolveAsClassId in JavaTypeResolver.kt); the wrong split has no symbol-provider entry
    // and is dropped downstream.
    private fun fqNameSplitCandidates(fqName: FqName): List<ClassId> {
        val parts = fqName.pathSegments().map { it.asString() }
        if (parts.isEmpty()) return emptyList()
        val result = mutableListOf<ClassId>()
        for (classStartIndex in (parts.size - 1) downTo 0) {
            val pkg =
                if (classStartIndex == 0) FqName.ROOT
                else FqName.fromSegments(parts.subList(0, classStartIndex))
            val cls = FqName.fromSegments(parts.subList(classStartIndex, parts.size))
            result.add(ClassId(pkg, cls, isLocal = false))
        }
        return result
    }
}
