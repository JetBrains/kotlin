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
 * Resolves inherited inner classes from supertype hierarchies, implementing JLS 6.5.2
 * (inherited member types in scope).
 *
 * Two entry points serve different consumers:
 *
 * - [findInnerClassFromSupertypes] returns a [JavaClass] (with its full AST-side outer-class
 *   chain) for inherited inner classes, including cross-file Java-source supertypes via the
 *   [classFinder]. Used by [JavaScopeResolver.findLocalClass] step 3 so that the rest of the
 *   AST pipeline (`JavaTypeOverAst.computeClassifier`, `JavaClassOverAst.findInnerClassInSupertypes`)
 *   can thread outer-class type arguments through the AST chain — the substitution context
 *   FIR needs for cases like
 *   `compiler/testData/diagnostics/tests/generics/innerClasses/j+k_complex.kt`.
 *
 * - [resolveInheritedInnerClassToClassId] returns a `ClassId` via a two-phase BFS:
 *   Phase 1 ([walkJavaSourceSupertypes]) walks Java-source supertypes through the AST /
 *   classFinder source index — independent of FIR's lazy phase machinery, so it stays
 *   correct even when the BFS is invoked while the supertype's own `SUPER_TYPES` resolution
 *   is on the call stack. Phase 2 ([walkBinarySupertypes]) walks Kotlin / binary supertypes
 *   through FIR's `getSupertypeClassIds` callback (which, after Stage 3 of the unification
 *   refactoring, uses `lazyResolveToPhase(SUPER_TYPES)`).
 *
 * **Stage 2b deferral note** (see `implDocs/RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`).
 * The merged plan's Step 2 lists "drop Phase 1 in favour of Phase 2 alone" as part of the
 * mechanical Stage-2 work — the spec assumed that once Stage 3 makes
 * `JavaTypeConversion.getResolvedSupertypeClassIds` origin-agnostic, Phase 2 alone can walk
 * Java-source supertypes too. In practice that drop regresses
 * `compiler/testData/diagnostics/tests/j+k/collectionOverrides/mapMethodsImplementedInJava.kt`:
 * resolving `Set<Entry<…>>` inside `Derived extends Base<String> implements Map<String, T>`
 * needs the BFS to find `Entry` on `Map` while `Base`'s `SUPER_TYPES` is on the resolution
 * stack. In compiler (non-LL-FIR) mode `lazyResolveToPhase(SUPER_TYPES)` is a no-op, so
 * `getResolvedSupertypeClassIds(Base)` may read `Base.superTypeRefs` *before* its
 * `SUPER_TYPES` phase has finished and produce empty / partial results. Phase 1's AST walk
 * reads supertype names directly from the source-index, doesn't depend on FIR's phase
 * state, and therefore stays correct. Stage 2b is consequently DEFERRED again (was already
 * deferred to "ride together with Stage 3" in the merged plan); collapsing the two phases
 * is a Stage 5 concern, conditional on routing the BFS through a phase-aware adapter that
 * forces the supertype's `SUPER_TYPES` from the *outermost* lazy entry.
 *
 * Stage 2b also deliberately does NOT subsume [findInnerClassFromSupertypes]: the BFS
 * yields a bare `ClassId`, but downstream FIR conversion needs an AST-side `JavaClass` to
 * recover outer-class type-argument substitutions for inherited inner classes (the
 * `j+k_complex.kt` post-mortem in the 2026-05-05 entry of `ITERATION_RESULTS.md` covers
 * this).
 */
internal class JavaInheritedMemberResolver(
    private val packageFqName: FqName,
    private val classFinder: LeanJavaClassFinder?,
    private val sameFileTopLevelClassProvider: (Name) -> JavaClass?,
) {

    /**
     * Searches for an inner class with the given name in the supertype hierarchy.
     * Implements JLS 6.5.2 — inherited member types are in scope.
     *
     * Returns null if multiple inner classes with the same name are found (ambiguity),
     * matching `javac`'s `MISSING_DEPENDENCY_CLASS` error. Uses the [classFinder] (if
     * available) to detect cross-file ambiguities and to materialize the inherited
     * `JavaClass` for cross-file Java-source supertypes; falls back to local resolution for
     * same-file supertypes via [sameFileTopLevelClassProvider].
     */
    fun findInnerClassFromSupertypes(name: Name, javaClass: JavaClass, visited: MutableSet<JavaClass>): JavaClass? {
        if (javaClass in visited) return null
        visited.add(javaClass)

        val allFound = mutableSetOf<JavaClass>()

        // Same-file supertypes — local resolution by simple name. Cross-file supertypes are
        // handled by the classFinder fallback below; that path is the one the
        // `j+k_complex.kt` trip-wire depends on.
        for (supertype in javaClass.supertypes) {
            val supertypeRef = supertype.presentableText.let { text ->
                val withoutGenerics = text.substringBefore('<').trim()
                withoutGenerics.substringBefore('.').trim()
            }
            if (supertypeRef.isEmpty()) continue
            val supertypeClass = sameFileTopLevelClassProvider(Name.identifier(supertypeRef)) ?: continue
            supertypeClass.findInnerClass(name)?.let { allFound.add(it) }
            findInnerClassFromSupertypes(name, supertypeClass, visited)?.let { allFound.add(it) }
        }

        if (allFound.isEmpty()) {
            val javaClassOverAst = javaClass as? JavaClassOverAst
            if (javaClassOverAst != null && classFinder != null) {
                val containingClassId = fqNameInPackageToClassId(javaClassOverAst.fqName, packageFqName)
                val candidates = classFinder.collectInheritedInnerClasses(containingClassId)[name.asString()] ?: emptySet()
                if (candidates.size > 1) return null
                if (candidates.size == 1) return classFinder.findClass(JavaClassFinder.Request(candidates.first()))
            }
        }

        if (allFound.size > 1) return null
        return allFound.firstOrNull()
    }

    /**
     * Try to resolve a simple name as an inner class inherited from supertypes.
     *
     * **Step 4.5a** (per [implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md] §11):
     * post-injection, the FIR-side `getSupertypeClassIds` callback is replaced by the
     * model's own per-origin [directSupertypeClassIds] dispatcher (an injected
     * `(ClassId) -> List<ClassId>` member of [JavaResolutionContext], wrapped in
     * [JavaSupertypeLoopChecker] cycle bounds). Phase 1 (source-only walk via the AST
     * class finder) remains as a fast path inside the loop because it avoids a FIR
     * round-trip for same-package source supertypes; Phase 2 then asks the dispatcher
     * for any supertype that the source index could not resolve directly.
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
        directSupertypeClassIds: (ClassId) -> List<ClassId>,
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

        if (nonSourceSupertypeIds.isEmpty()) return null
        return walkBinarySupertypes(simpleName, nonSourceSupertypeIds, directSupertypeClassIds, tryResolve, visited)
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
     * [walkJavaSourceSupertypes]. Uses [directSupertypeClassIds] (the model's per-origin
     * dispatcher per [implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md] §6) to walk each
     * one transitively; probes the same `parentClassId.SimpleName` pattern; shares [visited]
     * so cross-pass ambiguity is still detected.
     *
     * Step 4.5a replaces the FIR-side `getSupertypeClassIds` callback with the dispatcher,
     * which itself routes per-origin to AST data (Java source / binary) or
     * `lazyResolveToPhase(SUPER_TYPES) + superTypeRefs` (Kotlin / built-in / deserialized).
     */
    private fun walkBinarySupertypes(
        simpleName: String,
        nonSourceSupertypeIds: List<ClassId>,
        directSupertypeClassIds: (ClassId) -> List<ClassId>,
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
                for (parentClassId in directSupertypeClassIds(classId)) {
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
