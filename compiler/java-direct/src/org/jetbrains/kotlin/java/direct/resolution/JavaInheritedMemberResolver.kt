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
 * - [resolveInheritedInnerClassToClassId] returns a `ClassId` via a two-pass BFS:
 *   [walkJavaSourceSupertypes] walks Java-source supertypes through the AST / classFinder
 *   source index — independent of FIR's lazy phase machinery, so it stays correct even when
 *   the BFS is invoked while the supertype's own `SUPER_TYPES` resolution is on the call
 *   stack. [walkBinarySupertypes] walks Kotlin / binary supertypes through the
 *   per-origin dispatcher (see `RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`).
 *
 * The two passes are intentionally NOT merged: dropping [walkJavaSourceSupertypes] regresses
 * `compiler/testData/diagnostics/tests/j+k/collectionOverrides/mapMethodsImplementedInJava.kt`
 * (the AST walk reads supertype names from the source index without depending on FIR's
 * phase state). Collapsing the two passes remains conditional on routing the BFS through a
 * phase-aware adapter — see `RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md` for the
 * Stage 2b / Stage 5 rationale.
 *
 * The source-pass also deliberately does NOT subsume [findInnerClassFromSupertypes]: the BFS
 * yields a bare `ClassId`, but downstream FIR conversion needs an AST-side `JavaClass` to
 * recover outer-class type-argument substitutions for inherited inner classes.
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
        if (!visited.add(javaClass)) return null

        var foundInnerClass: JavaClass? = null

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
            (supertypeClass.findInnerClass(name) ?: findInnerClassFromSupertypes(name, supertypeClass, visited))?.let {
                if (foundInnerClass == null) foundInnerClass = it else return null
            }
        }

        if (foundInnerClass == null) {
            val javaClassOverAst = javaClass as? JavaClassOverAst
            if (javaClassOverAst != null && classFinder != null) {
                val containingClassId = fqNameInPackageToClassId(javaClassOverAst.fqName, packageFqName)
                val candidates = classFinder.collectInheritedInnerClasses(containingClassId)[name.asString()] ?: emptySet()
                if (candidates.size > 1) return null
                if (candidates.size == 1) return classFinder.findClass(JavaClassFinder.Request(candidates.first()))
            }
        }

        return foundInnerClass
    }

    /**
     * Try to resolve a simple name as an inner class inherited from supertypes.
     *
     * The per-origin [directSupertypeClassIds] dispatcher (see
     * `FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` §11) is used for binary supertypes;
     * Java-source supertypes use the class-finder source index directly. Both passes share
     * `visited` and use the `SupertypeClassId.SimpleName` probe pattern with ambiguity
     * detection.
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

        // Convert the initial supertypes (the containing-class-chain's direct supertypes,
        // expressed as `JavaClassifierType` AST entries) into `ClassId`s using the caller's
        // resolution context — these names live in the file currently being parsed.
        val initialIds = initialSupertypes.mapNotNull { st ->
            val name = st.presentableText.substringBefore('<').trim()
            if (name.isEmpty()) null else resolveWithoutInheritance(name, tryResolve)
        }
        var currentLevelIds: List<ClassId> = initialIds

        repeat(MAX_SUPERTYPE_DEPTH) {
            if (currentLevelIds.isEmpty()) return null
            val nextLevelIds = mutableListOf<ClassId>()

            for (supertypeClassId in currentLevelIds) {
                if (!visited.add(supertypeClassId)) continue

                val innerClassId = supertypeClassId.createNestedClassId(Name.identifier(simpleName))
                if (tryResolve(innerClassId)) {
                    if (foundClassId != null && foundClassId != innerClassId) return null
                    foundClassId = innerClassId
                }

                if (foundClassId == null) {
                    if (classFinder != null && classFinder.isClassInIndex(supertypeClassId)) {
                        // Java source class — descend via the per-class supertype graph,
                        // which resolves names using *that file's* imports (not the caller's).
                        // Using `javaClass.supertypes.presentableText` here would re-resolve
                        // each name through `resolveWithoutInheritance` (the caller's context),
                        // silently dropping any supertype the caller's file does not import.
                        nextLevelIds.addAll(classFinder.getDirectSupertypes(supertypeClassId))
                    } else {
                        // Non-source class (Kotlin / binary): deferred to walkBinarySupertypes.
                        nonSourceSupertypeIds.add(supertypeClassId)
                    }
                }
            }

            if (foundClassId != null) return foundClassId
            currentLevelIds = nextLevelIds
        }

        return null
    }

    /**
     * Deque-based BFS over the ClassIds of non-source (Kotlin / binary) supertypes collected by
     * [walkJavaSourceSupertypes]. Uses [directSupertypeClassIds] — the model's per-origin
     * dispatcher — to walk each one transitively; probes the same `parentClassId.SimpleName`
     * pattern; shares [visited] so cross-pass ambiguity is still detected.
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
        return null
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
