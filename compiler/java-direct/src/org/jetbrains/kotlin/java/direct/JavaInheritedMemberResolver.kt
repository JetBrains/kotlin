/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Resolves inherited inner classes from supertype hierarchies.
 *
 * Responsible for:
 * - Finding inner classes inherited from supertypes (JLS 6.5.2 — inherited member types)
 * - BFS traversal of supertype hierarchies for inner class resolution
 * - Aggregating inherited inner classes across the containing class chain
 * - Detecting ambiguities when multiple supertypes declare inner classes with the same name
 *
 * This class encapsulates the supertype-walking logic that was previously embedded in
 * [JavaResolutionContext], making it independently testable and reducing the context's complexity.
 */
internal class JavaInheritedMemberResolver(
    private val packageFqName: FqName,
    private val classFinderProvider: (() -> JavaClassFinderOverAstImpl)?,
    private val localClassProvider: (Name) -> JavaClass?,
) {

    /**
     * Searches for an inner class with the given name in the supertype hierarchy.
     * This implements JLS 6.5.2 - inherited member types are in scope.
     *
     * Returns null if multiple inner classes with the same name are found (ambiguity),
     * which will cause MISSING_DEPENDENCY_CLASS error as per javac behavior.
     *
     * Uses the classFinderProvider (if available) to detect cross-file ambiguities.
     * Falls back to local resolution for same-file supertypes.
     */
    fun findInnerClassFromSupertypes(name: Name, javaClass: JavaClass, visited: MutableSet<JavaClass>): JavaClass? {
        if (javaClass in visited) return null
        visited.add(javaClass)

        val allFound = mutableSetOf<JavaClass>()

        // First try local resolution (same-file supertypes)
        for (supertype in javaClass.supertypes) {
            val supertypeRef = supertype.presentableText.let { text ->
                val withoutGenerics = text.substringBefore('<').trim()
                withoutGenerics.substringBefore('.').trim()
            }

            if (supertypeRef.isEmpty()) continue

            val supertypeClass = localClassProvider(Name.identifier(supertypeRef)) ?: continue

            supertypeClass.findInnerClass(name)?.let { found ->
                allFound.add(found)
            }

            findInnerClassFromSupertypes(name, supertypeClass, visited)?.let { found ->
                allFound.add(found)
            }
        }

        // If local resolution found nothing, try cross-file detection
        if (allFound.isEmpty()) {
            val javaClassOverAst = javaClass as? JavaClassOverAst
            if (javaClassOverAst != null && classFinderProvider != null) {
                val fqName = javaClassOverAst.fqName
                if (fqName != null) {
                    val containingClassId = Companion.fqNameToClassId(fqName, packageFqName)
                    val classFinder = classFinderProvider.invoke()

                    val inheritedInners = classFinder.collectInheritedInnerClasses(containingClassId)
                    val candidates = inheritedInners[name.asString()] ?: emptySet()

                    if (candidates.size > 1) {
                        // Ambiguity detected across multiple supertypes
                        return null
                    }

                    if (candidates.size == 1) {
                        return classFinder.findClass(JavaClassFinder.Request(candidates.first()))
                    }
                }
            }
        }

        if (allFound.size > 1) return null
        return allFound.firstOrNull()
    }

    /**
     * Searches the supertype hierarchy of [outerClassId] for an inherited nested class with [nestedName].
     * Uses both the [getSupertypeClassIds] callback (for Kotlin/binary classes) and the class finder's
     * [JavaClassFinderOverAstImpl.collectInheritedInnerClasses] (for same-package Java source classes).
     */
    fun findInheritedNestedClass(
        outerClassId: ClassId,
        nestedName: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: (ClassId) -> List<ClassId>,
        visited: MutableSet<ClassId>,
    ): ClassId? {
        if (outerClassId in visited) return null
        visited.add(outerClassId)

        for (supertypeId in getSupertypeClassIds(outerClassId)) {
            val candidateId = supertypeId.createNestedClassId(Name.identifier(nestedName))
            if (tryResolve(candidateId)) return candidateId
            // Recurse into supertype's supertypes
            findInheritedNestedClass(supertypeId, nestedName, tryResolve, getSupertypeClassIds, visited)
                ?.let { return it }
        }

        // Also check via the class finder for same-package Java source supertypes
        val classFinder = classFinderProvider?.invoke()
        if (classFinder != null) {
            val inheritedInners = classFinder.collectInheritedInnerClasses(outerClassId)
            val candidates = inheritedInners[nestedName]
            if (candidates != null && candidates.size == 1) {
                val candidateClassId = candidates.first()
                if (tryResolve(candidateClassId)) return candidateClassId
            }
        }

        return null
    }

    /**
     * Computes the aggregated inherited inner classes for the entire containing class chain.
     * Maps simpleName -> Set<ClassId> across the containing class and all its outer classes.
     */
    fun computeAggregatedInheritedInnerClasses(containingClass: JavaClassOverAst): Map<String, Set<ClassId>>? {
        val classFinder = classFinderProvider?.invoke() ?: return null

        val merged = mutableMapOf<String, MutableSet<ClassId>>()
        var current: JavaClass? = containingClass
        while (current != null) {
            val jdClass = current as? JavaClassOverAst
            val fqn = jdClass?.fqName
            if (fqn != null) {
                val cid = Companion.fqNameToClassId(fqn, packageFqName)
                val inheritedInners = classFinder.collectInheritedInnerClasses(cid)
                for ((name, classIds) in inheritedInners) {
                    merged.getOrPut(name) { mutableSetOf() }.addAll(classIds)
                }
            }
            current = current.outerClass
        }
        return merged.mapValues { it.value.toSet() }
    }

    /**
     * Try to resolve a simple name as an inner class inherited from supertypes.
     * This handles cross-file inheritance (e.g. a Java class extending a Kotlin class with an
     * inner class).
     *
     * Runs two passes:
     *   1. [walkJavaSourceSupertypes] — BFS over [JavaClassifierType] supertypes from the Java
     *      model; fast for same-file Java source supertypes, fully self-contained (no FIR
     *      interaction). Non-source supertypes are recorded in `nonSourceSupertypeIds`.
     *   2. [walkBinarySupertypes] — BFS over the non-source (Kotlin / binary) supertype ClassIds
     *      using the FIR [getSupertypeClassIds] callback.
     *
     * Both passes share `visited` (to avoid re-probing the same `ClassId`) and use the
     * `SupertypeClassId.SimpleName` probe pattern with ambiguity detection.
     *
     * @param resolveWithoutInheritance function to resolve a name without checking inherited
     *        inner classes (to avoid infinite recursion back into this method).
     */
    fun resolveInheritedInnerClassToClassId(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)?,
        containingClassProvider: (() -> JavaClass?)?,
        resolveWithoutInheritance: (String, (ClassId) -> Boolean) -> ClassId?,
    ): ClassId? {
        val containingClass = containingClassProvider?.invoke() ?: return null

        // Collect direct supertypes from the containing class and its outer classes.
        val initialSupertypes = mutableListOf<JavaClassifierType>()
        var currentClass: JavaClass? = containingClass
        while (currentClass != null) {
            initialSupertypes.addAll(currentClass.supertypes)
            currentClass = currentClass.outerClass
        }
        val visited = mutableSetOf<ClassId>()
        val nonSourceSupertypeIds = mutableListOf<ClassId>()

        walkJavaSourceSupertypes(
            simpleName, initialSupertypes, tryResolve, resolveWithoutInheritance, visited, nonSourceSupertypeIds,
        )?.let { return it }

        if (getSupertypeClassIds == null || nonSourceSupertypeIds.isEmpty()) return null
        return walkBinarySupertypes(simpleName, nonSourceSupertypeIds, getSupertypeClassIds, tryResolve, visited)
    }

    /**
     * BFS over [JavaClassifierType] supertypes from the Java model, starting from
     * [initialSupertypes]. For each supertype, resolves its name via [resolveWithoutInheritance]
     * (the reentrance-safe variant — must NOT recurse back into
     * [resolveInheritedInnerClassToClassId]), probes `supertypeClassId.SimpleName` via
     * [tryResolve], and either queues the supertype's own supertypes (Java source classes) or
     * appends to [nonSourceSupertypeIds] (Kotlin / binary classes, handled by [walkBinarySupertypes]).
     *
     * Returns the found inner-class `ClassId` or `null` if nothing was found;
     * returns `null` early if ambiguity is detected (two different matches).
     */
    private fun walkJavaSourceSupertypes(
        simpleName: String,
        initialSupertypes: List<JavaClassifierType>,
        tryResolve: (ClassId) -> Boolean,
        resolveWithoutInheritance: (String, (ClassId) -> Boolean) -> ClassId?,
        visited: MutableSet<ClassId>,
        nonSourceSupertypeIds: MutableList<ClassId>,
    ): ClassId? {
        var foundClassId: ClassId? = null
        var currentLevel: List<JavaClassifierType> = initialSupertypes

        for (depth in 0 until MAX_SUPERTYPE_DEPTH) {
            if (currentLevel.isEmpty()) break
            val nextLevel = mutableListOf<JavaClassifierType>()

            for (supertype in currentLevel) {
                // Text-based resolution only (no resolve() calls) to avoid recursion back into
                // resolveInheritedInnerClassToClassId.
                val supertypeName = supertype.presentableText.substringBefore('<').trim()
                if (supertypeName.isEmpty()) continue

                val supertypeClassId = resolveWithoutInheritance(supertypeName, tryResolve) ?: continue
                if (!visited.add(supertypeClassId)) continue

                val innerClassId = supertypeClassId.createNestedClassId(Name.identifier(simpleName))
                if (tryResolve(innerClassId)) {
                    if (foundClassId != null && foundClassId != innerClassId) return null
                    foundClassId = innerClassId
                }

                if (foundClassId == null) {
                    val classFinder = classFinderProvider?.invoke()
                    if (classFinder != null && classFinder.isClassInIndex(supertypeClassId)) {
                        // Java source class: walk via class finder (safe, no FIR interaction).
                        val javaClass = classFinder.findClass(JavaClassFinder.Request(supertypeClassId))
                        if (javaClass != null) {
                            nextLevel.addAll(javaClass.supertypes)
                        }
                    } else {
                        // Non-source class (Kotlin / binary): deferred to walkBinarySupertypes.
                        nonSourceSupertypeIds.add(supertypeClassId)
                    }
                }
            }

            if (foundClassId != null) return foundClassId
            currentLevel = nextLevel
        }

        return foundClassId
    }

    /**
     * Deque-based BFS over the ClassIds of non-source (Kotlin / binary) supertypes collected by
     * [walkJavaSourceSupertypes]. Uses [getSupertypeClassIds] to walk each one transitively;
     * probes the same `parentClassId.SimpleName` pattern; shares [visited] so cross-pass
     * ambiguity is still detected.
     */
    private fun walkBinarySupertypes(
        simpleName: String,
        nonSourceSupertypeIds: List<ClassId>,
        getSupertypeClassIds: (ClassId) -> List<ClassId>,
        tryResolve: (ClassId) -> Boolean,
        visited: MutableSet<ClassId>,
    ): ClassId? {
        var foundClassId: ClassId? = null
        val queue = ArrayDeque(nonSourceSupertypeIds)
        var depth = 0
        while (queue.isNotEmpty() && depth < MAX_SUPERTYPE_DEPTH) {
            val batch = queue.toList()
            queue.clear()
            for (classId in batch) {
                for (parentClassId in getSupertypeClassIds(classId)) {
                    if (!visited.add(parentClassId)) continue

                    val innerClassId = parentClassId.createNestedClassId(Name.identifier(simpleName))
                    if (tryResolve(innerClassId)) {
                        if (foundClassId != null && foundClassId != innerClassId) return null
                        foundClassId = innerClassId
                    }
                    if (foundClassId == null) {
                        queue.add(parentClassId)
                    }
                }
            }
            if (foundClassId != null) return foundClassId
            depth++
        }
        return foundClassId
    }

    companion object {
        /**
         * Depth cap for supertype BFS in [resolveInheritedInnerClassToClassId]. Chosen to cover
         * typical collection / Throwable / Cloneable hierarchies without pathological looping on
         * cycles the `visited` set misses (there shouldn't be any, but the cap is cheap insurance).
         */
        private const val MAX_SUPERTYPE_DEPTH = 5

        /**
         * Converts a FqName to a ClassId using the package FqName.
         * Shared utility used by both this class and [JavaResolutionContext].
         */
        internal fun fqNameToClassId(fqName: FqName, packageFqName: FqName): ClassId {
            val fqnString = fqName.asString()
            val pkgString = packageFqName.asString()

            val className = if (pkgString.isEmpty()) {
                fqnString
            } else if (fqnString.startsWith(pkgString + ".")) {
                fqnString.substring(pkgString.length + 1)
            } else {
                fqnString
            }

            return ClassId(packageFqName, FqName(className), isLocal = false)
        }
    }
}
