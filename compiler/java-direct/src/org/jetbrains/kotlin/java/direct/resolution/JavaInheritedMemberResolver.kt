/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
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
    private val classFinder: LeanJavaClassFinder?,
    private val sameFileTopLevelClassProvider: (Name) -> JavaClass?,
) {

    /**
     * Searches for an inner class with the given name in the supertype hierarchy.
     * This implements JLS 6.5.2 - inherited member types are in scope.
     *
     * Returns null if multiple inner classes with the same name are found (ambiguity),
     * which will cause the MISSING_DEPENDENCY_CLASS error as per javac behavior.
     *
     * Uses the [classFinder] (if available) to detect cross-file ambiguities.
     * Falls back to local resolution for same-file supertypes.
     */
    fun findInnerClassFromSupertypes(name: Name, javaClass: JavaClass, visited: MutableSet<JavaClass>): JavaClass? {
        if (javaClass in visited) return null
        visited.add(javaClass)

        val allFound = mutableSetOf<JavaClass>()

        // First, try local resolution (same-file supertypes)
        for (supertype in javaClass.supertypes) {
            val supertypeRef = supertype.presentableText.let { text ->
                val withoutGenerics = text.substringBefore('<').trim()
                withoutGenerics.substringBefore('.').trim()
            }

            if (supertypeRef.isEmpty()) continue

            val supertypeClass = sameFileTopLevelClassProvider(Name.identifier(supertypeRef)) ?: continue

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
            if (javaClassOverAst != null && classFinder != null) {
                val fqName = javaClassOverAst.fqName
                val containingClassId = fqNameInPackageToClassId(fqName, packageFqName)

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

        if (allFound.size > 1) return null
        return allFound.firstOrNull()
    }

    /**
     * Try to resolve a simple name as an inner class inherited from supertypes.
     * This handles cross-file inheritance (e.g., a Java class extending a Kotlin class with an
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
     * **Stage 2b deferral note** (resolver-unification — see
     * `implDocs/RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`). The merged plan
     * (Step 2) lists "drop Phase 1 in favour of Phase 2 alone" as part of the mechanical
     * Stage-2 work. In practice that drop cannot land in isolation: today
     * `JavaTypeConversion.getResolvedSupertypeClassIds` short-circuits to `emptyList()` for
     * `FirDeclarationOrigin.Java.Source` classes (the documented avoid-premature-lazy-resolution
     * filter), so Phase 2 alone cannot walk through Java-source supertypes. Phase 1's text →
     * `ClassId` resolution + source-index recursion is the only way to reach inner classes
     * inherited along a chain `JavaSource → JavaSource → ...`. Stage 3 of the unification
     * replaces that filter with `lazyResolveToPhase(SUPER_TYPES)`; once it lands,
     * [getSupertypeClassIds] becomes origin-agnostic and Phase 1 collapses cleanly into
     * Phase 2. Until then, Phase 1 stays.
     *
     * @param resolveWithoutInheritance function to resolve a name without checking inherited
     *        inner classes (to avoid infinite recursion back into this method).
     */
    fun resolveInheritedInnerClassToClassId(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)?,
        containingClass: JavaClass?,
        resolveWithoutInheritance: (String, (ClassId) -> Boolean) -> ClassId?,
    ): ClassId? {
        containingClass ?: return null

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
}

/**
 * Depth cap for supertype BFS in [JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId]. Chosen to cover
 * typical collection / Throwable / Cloneable hierarchies without pathological looping on
 * cycles the `visited` set misses (there shouldn't be any, but the cap is a cheap insurance).
 */
private const val MAX_SUPERTYPE_DEPTH = 5

internal fun fqNameInPackageToClassId(fqName: FqName, packageFqName: FqName): ClassId {
    val fqnString = fqName.asString()
    val pkgString = packageFqName.asString()

    val className = if (pkgString.isEmpty()) {
        fqnString
    } else if (fqnString.startsWith(pkgString) && fqnString.length > pkgString.length && fqnString[pkgString.length] == '.') {
        fqnString.substring(pkgString.length + 1)
    } else {
        fqnString
    }

    return ClassId(packageFqName, FqName(className), isLocal = false)
}
