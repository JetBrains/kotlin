/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.load.java.structure.impl.splitCanonicalFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Resolves inherited inner classes from supertype hierarchies (JLS 6.5.2 — inherited member
 * types are in scope).
 *
 * Two entry points serve different consumers:
 *
 * - [findInnerClassFromSupertypes] returns a [JavaClass] with its full AST-side outer-class
 *   chain, so the AST pipeline can thread outer-class type arguments through the chain — the
 *   substitution context FIR needs for inherited inner classes of generic outer classes.
 *   Cross-file Java-source supertypes are handled via the [classFinder]; binary Java and Kotlin
 *   supertypes are handled by a final tail that reuses [resolveInheritedInnerClassToClassId]
 *   (via the caller-supplied `resolveBinaryOrKotlinInherited` callback) and materializes the
 *   result through the caller-supplied `classifierAdapterFor` callback — see the parameter docs
 *   on [findInnerClassFromSupertypes] itself.
 *
 * - [resolveInheritedInnerClassToClassId] returns a `ClassId` via a two-pass BFS:
 *   [walkJavaSourceSupertypes] walks Java-source supertypes through the [classFinder] source
 *   index — independent of FIR's lazy phase machinery, so it stays correct even when invoked
 *   while the supertype's own `SUPER_TYPES` resolution is on the call stack;
 *   [walkBinarySupertypes] walks Kotlin / binary supertypes through the per-origin dispatcher.
 *
 * The passes are kept separate by design:
 * - the source pass must not depend on FIR phase state, unlike the binary pass;
 * - the source pass yields a bare `ClassId`, while [findInnerClassFromSupertypes] must also
 *   recover the AST-side `JavaClass` for outer-class type-argument substitution — which the
 *   binary/Kotlin tail achieves by materializing its `ClassId` result via `classifierAdapterFor`
 *   instead (a [FirBackedJavaClassAdapter] for binary/Kotlin, or the canonical source
 *   `JavaClassOverAst` if `classifierAdapterFor`'s routing finds the result is source-backed).
 */
internal class JavaInheritedMemberResolver(
    private val classFinder: LeanJavaClassFinder?,
    private val sameFileTopLevelClassProvider: (Name) -> JavaClass?,
) {

    /**
     * Searches for an inner class with the given name in the supertype hierarchy.
     *
     * Returns null if multiple inner classes with the same name are found (ambiguity),
     * matching `javac`'s `MISSING_DEPENDENCY_CLASS` error. Uses the [classFinder] (if
     * available) to detect cross-file ambiguities and to materialize the inherited
     * `JavaClass` for cross-file Java-source supertypes; falls back to
     * [sameFileTopLevelClassProvider] for same-file supertypes.
     *
     * @param resolveBinaryOrKotlinInherited the binary/Kotlin tail, tried only after the
     *        same-file and cross-file-source arms above have found nothing for [javaClass]
     *        itself: resolves an inherited inner class of [javaClass] to a `ClassId` by reusing
     *        [resolveInheritedInnerClassToClassId]'s generic ladder (already binary/Kotlin-aware
     *        via [directSupertypeClassIds]).
     * @param classifierAdapterFor materializes the `ClassId` found by
     *        [resolveBinaryOrKotlinInherited] back into a navigable [JavaClass] — routes
     *        source-backed results to their canonical [JavaClassOverAst], wraps binary/Kotlin
     *        results in a [FirBackedJavaClassAdapter].
     */
    fun findInnerClassFromSupertypes(
        name: Name,
        javaClass: JavaClassOverAst,
        visited: MutableSet<JavaClass>,
        resolveBinaryOrKotlinInherited: (JavaClass, Name) -> ClassId?,
        classifierAdapterFor: (ClassId) -> JavaClass?,
    ): JavaClass? {
        if (!visited.add(javaClass)) return null

        var foundInnerClass: JavaClass? = null

        // Same-file supertypes — local resolution by simple name. Cross-file source supertypes
        // are handled by the classFinder fallback below; binary/Kotlin supertypes by the tail.
        for (supertype in javaClass.supertypes) {
            val supertypeClass = resolveSameFileSupertype(supertype) ?: continue
            (
                supertypeClass.findInnerClass(name)
                    ?: findInnerClassFromSupertypes(name, supertypeClass, visited, resolveBinaryOrKotlinInherited, classifierAdapterFor)
                )?.let {
                if (foundInnerClass == null) foundInnerClass = it else return null
            }
        }

        if (foundInnerClass != null) return foundInnerClass

        val containingClassId = javaClass.classId ?: return null
        if (classFinder != null) {
            val candidates = classFinder.collectInheritedInnerClasses(containingClassId)[name.asString()]
            // A non-null `candidates` means the source-supertype hierarchy has an opinion on
            // [name] — including ambiguity (`size > 1`, `singleOrNull()` then `null`), which must
            // NOT fall through to the binary/Kotlin tail below (that would silently pick one of
            // the ambiguous candidates via a differently-shaped BFS, masking the ambiguity).
            // Only a `null` map entry (no source-side candidate at all) defers to the tail.
            if (candidates != null) {
                return candidates.singleOrNull()?.let { classFinder.findClass(JavaClassFinder.Request(it)) }
            }
        }

        // Binary/Kotlin tail — the same-file and cross-file-source arms above found nothing for
        // `javaClass`'s own supertypes; fall through to the generic ClassId ladder, which is
        // already binary/Kotlin-aware.
        val inheritedId = resolveBinaryOrKotlinInherited(javaClass, name) ?: return null
        return classifierAdapterFor(inheritedId)
    }

    /**
     * Resolves a same-file supertype reference to the [JavaClassOverAst] it denotes.
     *
     * Only AST-backed same-file classes are produced here: the outermost segment comes from
     * [sameFileTopLevelClassProvider] and each nested segment from [JavaClass.findInnerClass], both
     * of which yield [JavaClassOverAst] for same-file classes. The result is narrowed accordingly so
     * [findInnerClassFromSupertypes] keeps recursing only over AST classes.
     */
    private fun resolveSameFileSupertype(supertype: JavaClassifierType): JavaClassOverAst? {
        val segments = supertype.presentableText.splitCanonicalFqName().map { it.substringBefore('<').trim() }
        if (segments.isEmpty() || segments.any { it.isEmpty() }) return null
        var resolved = sameFileTopLevelClassProvider(Name.identifier(segments.first())) as? JavaClassOverAst ?: return null
        for (i in 1 until segments.size) {
            resolved = resolved.findInnerClass(Name.identifier(segments[i])) as? JavaClassOverAst ?: return null
        }
        return resolved
    }

    /**
     * Tries to resolve a simple name as an inner class inherited from supertypes. Binary
     * supertypes go through the per-origin [directSupertypeClassIds] dispatcher; Java-source
     * supertypes use the class-finder source index directly. Both passes share `visited` and
     * detect ambiguity.
     *
     * @param resolveWithoutInheritance resolves a name without checking inherited inner
     *        classes, to avoid infinite recursion back into this method.
     * @param includeOuterClasses when `true`, the search starts from the supertypes of
     *        [containingClass] *and* of every enclosing class; when `false`, only the supertypes
     *        of [containingClass] itself are searched. The per-level (`false`) flavor lets the
     *        caller interleave declared and inherited member types level by level, preserving the
     *        JLS 6.4.1 rule that an inner level's inherited member type shadows an outer level's
     *        declared one.
     */
    fun resolveInheritedInnerClassToClassId(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        directSupertypeClassIds: (ClassId) -> List<ClassId>,
        containingClass: JavaClass?,
        resolveWithoutInheritance: (String, (ClassId) -> Boolean) -> ClassId?,
        includeOuterClasses: Boolean = true,
    ): ClassId? {
        containingClass ?: return null

        // Collect direct supertypes from the containing class (and, when requested, its outer classes).
        val initialSupertypes = mutableListOf<JavaClassifierType>()
        var currentClass: JavaClass? = containingClass
        while (currentClass != null) {
            initialSupertypes.addAll(currentClass.supertypes)
            if (!includeOuterClasses) break
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
 * typical collection / Throwable / Cloneable hierarchies without pathological non-termination on
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
