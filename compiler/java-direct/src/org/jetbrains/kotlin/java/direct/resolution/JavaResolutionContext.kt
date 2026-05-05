/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.iterator

/**
 * Resolution context for Java source files. Encapsulates all information
 * needed to resolve type references within a compilation unit.
 *
 * This is analogous to FIR scopes but simplified for Java's scoping rules.
 * The typeParametersInScope tracks type parameters visible at the current location
 * (from containing class and method declarations).
 *
 * Delegates to focused implementations:
 * - [CompilationUnitContext] — per-file immutable data (package, imports, class finder)
 * - [JavaImportResolver] — import extraction and package name parsing (stateless)
 * - [JavaScopeResolver] — type parameter scoping and local class lookup
 * - [JavaInheritedMemberResolver] — supertype hierarchy traversal for inner classes
 */
class JavaResolutionContext private constructor(
    private val unitContext: CompilationUnitContext,
    private val scopeResolver: JavaScopeResolver,
    private val containingClass: JavaClass? = null,
    /**
     * Lazily computed aggregated inherited inner classes for the entire containing class chain.
     * Maps simpleName -> Set<ClassId> across the containing class and all its outer classes.
     * Cached to avoid re-walking the outer class chain on every [resolveSimpleNameToClassId] call.
     * Shared by reference across contexts with the same containing class
     * (via [withTypeParameters] / [withInheritedTypeParameters]).
     */
    private val inheritedInnerCache: InheritedInnerCache =
        InheritedInnerCache(),
) {
    val packageFqName: FqName get() = unitContext.packageFqName

    private class InheritedInnerCache {
        @Volatile var value: Map<String, Set<ClassId>>? = null
    }

    /**
     * Aggregates inherited inner classes across the containing class's outer chain.
     * For each class in the chain, queries the class finder for transitively inherited
     * inner class names and merges them into a single map.
     */
    private fun getAggregatedInheritedInnerClasses(): Map<String, Set<ClassId>>? {
        inheritedInnerCache.value?.let { return it }
        val finder = unitContext.classFinder ?: return null
        val merged = mutableMapOf<String, MutableSet<ClassId>>()
        var current: JavaClass? = containingClass
        while (current != null) {
            val fqn = (current as? JavaClassOverAst)?.fqName
            if (fqn != null) {
                for ((name, classIds) in finder.collectInheritedInnerClasses(fqNameToClassId(fqn))) {
                    merged.getOrPut(name) { mutableSetOf() }.addAll(classIds)
                }
            }
            current = current.outerClass
        }
        val result = merged.mapValues { it.value.toSet() }
        inheritedInnerCache.value = result
        return result
    }

    /**
     * Finds a class by simple name in the AST-side scope. Delegates to
     * [JavaScopeResolver.findLocalClass]; see that method's KDoc for the five-step
     * ordering and for the post-Stage-4 role (AST classifier fast path only — no longer
     * in the `ClassId`-resolution path inside [resolveFromLocalScope]).
     */
    fun findLocalClass(name: Name): JavaClass? = scopeResolver.findLocalClass(name)

    /**
     * Searches the supertype hierarchy of [outerClassId] for an inherited nested class with [nestedName].
     * Uses both the [getSupertypeClassIds] callback (for Kotlin/binary classes) and the
     * [LeanJavaClassFinder.collectInheritedInnerClasses] (for same-package Java source classes).
     */
    private fun findInheritedNestedClass(
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
        val finder = unitContext.classFinder
        if (finder != null) {
            val inheritedInners = finder.collectInheritedInnerClasses(outerClassId)
            val candidates = inheritedInners[nestedName]
            if (candidates != null && candidates.size == 1) {
                val candidateClassId = candidates.first()
                if (tryResolve(candidateClassId)) return candidateClassId
            }
        }

        return null
    }

    /** Returns type parameters with HIGH priority (method/class own params, win over inner class names). */
    fun findTypeParameter(name: String): JavaTypeParameter? = scopeResolver.findTypeParameter(name)

    /** Returns type parameters with LOW priority (outer class inherited params, shadowed by inner class names). */
    fun findInheritedTypeParameter(name: String): JavaTypeParameter? = scopeResolver.findInheritedTypeParameter(name)

    fun getSimpleImport(simpleName: String): FqName? = unitContext.simpleImports[simpleName]

    /**
     * Returns the parsed imports (simple + star) from this context.
     * Used by [JavaClassFinderOverAstImpl.getDirectSupertypes] on the fast path
     * to avoid re-extracting imports from the AST root.
     */
    internal fun getImports(): Pair<Map<String, FqName>, List<FqName>> = Pair(unitContext.simpleImports, unitContext.starImports)

    /**
     * Returns true if the import target for [simpleName] is resolvable as a Java class.
     *
     * Checks whether the import target exists in the Java source index or is available
     * as a binary (compiled) Java class on the classpath. This matches PSI behavior where
     * only classes resolvable through PSI/classpath indexes are eagerly resolved.
     *
     * Kotlin classes (builtins, source classes without light classes) are NOT resolvable
     * through PSI indexes, so this returns false for them. FIR handles such classes
     * through its own symbol provider instead.
     *
     * Returns true (conservative) when no class finder is available.
     */
    fun isImportTargetAvailableAsJavaClass(simpleName: String): Boolean {
        val importedFqn = unitContext.simpleImports[simpleName] ?: return false
        val fqnStr = importedFqn.asString()
        // Imports from kotlin.* packages point to Kotlin classes, not Java classes.
        // PSI can't resolve Kotlin classes through its Java indexes (no light classes
        // in K2 mode), so they appear as unresolved types. Match this behavior by
        // not eagerly resolving kotlin.* imports.
        if (fqnStr.startsWith("kotlin.") || fqnStr == "kotlin") return false
        // All other imports (JDK, library, user-defined Java classes) are assumed
        // to be resolvable as Java classes
        return true
    }

    /**
     * Returns the first star import package that could contain a class with the given simple name.
     * Used for best-effort classId resolution when we can't call the symbol provider.
     */
    fun getFirstStarImportCandidate(simpleName: String): ClassId? {
        val starPackage = unitContext.starImports.firstOrNull() ?: return null
        return ClassId(starPackage, Name.identifier(simpleName))
    }

    /**
     * Returns true if a class with [simpleName] can be UNAMBIGUOUSLY found in the source index.
     * Checks: explicit imports, same-package, star imports (with ambiguity detection).
     *
     * Uses index-only lookup (no file I/O, no class instantiation) so it is safe to call
     * during FIR type processing without causing initialization order issues.
     *
     * Used by [JavaClassifierTypeOverAst.isTriviallyFlexibleHint] to make FIR produce compact
     * `T!` rendering (isTrivial=true) instead of `ft<T, T?>` for user-defined Java source classes,
     * matching the PSI behavior where all resolved Java classes are trivially flexible.
     *
     * Returns false for ambiguous cases (multiple star-import matches) to avoid false positives.
     */
    fun isUnambiguouslyCrossFileClass(simpleName: String): Boolean {
        val finder = unitContext.classFinder ?: return false
        // 1. Explicit single-type import takes highest priority (JLS 7.5.1) — always unambiguous
        unitContext.simpleImports[simpleName]?.let { importedFqn ->
            val fqnStr = importedFqn.asString()
            val classId = if (fqnStr.contains('.')) {
                val lastDot = fqnStr.lastIndexOf('.')
                ClassId(FqName(fqnStr.substring(0, lastDot)), FqName(fqnStr.substring(lastDot + 1)), isLocal = false)
            } else {
                ClassId.topLevel(FqName(fqnStr))
            }
            if (finder.isClassInIndex(classId)) return true
        }
        // 2. Same-package class — always unambiguous
        val samePackageClassId = if (packageFqName.isRoot) {
            ClassId.topLevel(FqName(simpleName))
        } else {
            ClassId(packageFqName, Name.identifier(simpleName))
        }
        if (finder.isClassInIndex(samePackageClassId)) return true
        // 3. Star imports — only if exactly one star import provides the class (no ambiguity)
        val starMatches = unitContext.starImports.count { starPackage ->
            finder.isClassInIndex(ClassId(starPackage, Name.identifier(simpleName)))
        }
        return starMatches == 1
    }

    /**
     * Creates a new context with additional OWN type parameters (high priority).
     * Used when entering a class or method that declares type parameters.
     * Own type params take priority over inner class names of the containing class.
     */
    fun withTypeParameters(typeParams: List<JavaTypeParameter>): JavaResolutionContext {
        if (typeParams.isEmpty()) return this
        return JavaResolutionContext(
            unitContext,
            scopeResolver.withTypeParameters(typeParams),
            containingClass,
            inheritedInnerCache, // share — containingClass unchanged
        )
    }

    /**
     * Creates a new context with INHERITED type parameters from an outer class (low priority).
     * Used for static nested types where outer class type params are visible but can be
     * shadowed by inner class names of the static nested type itself.
     */
    fun withInheritedTypeParameters(typeParams: List<JavaTypeParameter>): JavaResolutionContext {
        if (typeParams.isEmpty()) return this
        return JavaResolutionContext(
            unitContext,
            scopeResolver.withInheritedTypeParameters(typeParams),
            containingClass,
            inheritedInnerCache, // share — containingClass unchanged
        )
    }

    /**
     * Creates a new context for members of the given class.
     * Inner class references will be resolved against this class.
     */
    fun withContainingClass(newContainingClass: JavaClass): JavaResolutionContext {
        return JavaResolutionContext(
            unitContext,
            scopeResolver.withContainingClass(newContainingClass),
            containingClass = newContainingClass,
            // new holder — containingClass changed, aggregated inherited inner classes may differ
        )
    }

    /**
     * Resolve a type name to a ClassId using the callback for external resolution.
     * 
     * This method returns a ClassId directly, which unambiguously encodes the package/class
     * boundary. For example, "a.b" could mean either:
     * - ClassId("a", "b") - package "a", class "b"
     * - ClassId("", "a.b") - root package, nested class "a.b"
     * 
     * Using ClassId avoids the ambiguity that string-based resolution has.
     *
     * @param tryResolve callback that checks whether a [ClassId] resolves via the FIR symbol
     *        provider (returns `true` if found, `false` otherwise).
     * @param getSupertypeClassIds optional callback returning direct supertype [ClassId]s for
     *        already-resolved classes; used for inherited inner-class BFS through Kotlin /
     *        binary supertypes.
     * @param getClassLikeSymbol optional richer counterpart to [tryResolve]: when supplied,
     *        returns the resolved class-like symbol with its [JavaResolvedClassOrigin], or
     *        `null` if the [ClassId] does not resolve. Plumbed through Stage 1 of the
     *        resolver-unification refactoring (see
     *        `implDocs/RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`); not yet consumed
     *        beyond deriving the boolean "exists?" check, but exposes the origin information
     *        that the FIR caller already has so future stages can act on it.
     */
    fun resolve(
        name: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)? = null,
        getClassLikeSymbol: ((ClassId) -> JavaResolvedClassLikeSymbol?)? = null,
    ): ClassId? {
        // Stage 1 of resolver-unification: when [getClassLikeSymbol] is supplied, derive the
        // boolean "exists?" check from it so the boolean and the rich callback can never
        // disagree within a single invocation. When it isn't supplied, the existing
        // [tryResolve] is used unchanged. No current caller passes [getClassLikeSymbol], so
        // this is behavior-preserving today; the parameter is the API hook future stages
        // (which will read [JavaResolvedClassLikeSymbol.origin]) plug into.
        val effectiveTryResolve: (ClassId) -> Boolean = if (getClassLikeSymbol != null) {
            { classId -> getClassLikeSymbol(classId) != null }
        } else {
            tryResolve
        }

        // Handle nested class references like "Map.Entry"
        if (name.contains('.')) {
            // Cache tryResolve results within this invocation. The recursive prefix splitting
            // in resolveNestedClassToClassId probes the same ClassIds many times (e.g., "com"
            // is tried as a class for each prefix of "com.google.protobuf.Foo"). The callback
            // is deterministic within a single resolve() call, so caching is safe.
            //
            // Only allocated for dotted names — simple names (the majority) go through
            // resolveSimpleNameToClassId directly, avoiding the HashMap allocation entirely.
            val cache = HashMap<ClassId, Boolean>()
            val cachedTryResolve: (ClassId) -> Boolean = { classId ->
                cache.getOrPut(classId) { effectiveTryResolve(classId) }
            }
            return resolveNestedClassToClassId(name, cachedTryResolve, getSupertypeClassIds)
        }
        return resolveSimpleNameToClassId(name, effectiveTryResolve, getSupertypeClassIds)
    }

    /**
     * Resolve a nested class reference to ClassId.
     *
     * Per JLS 6.5.2, when a qualified name Q.Id could refer to either:
     * - A nested class Id of class Q, or
     * - A top-level class Id in package Q
     *
     * The nested class interpretation takes priority, BUT only if Q actually resolves
     * to a class in the current scope. We try to resolve Q as a class first using
     * the normal resolution rules (same package, imports, etc.). If Q resolves to a class,
     * we try Q.Id as a nested class. If that fails or Q doesn't resolve, we fall back
     * to trying Q.Id as a fully qualified name.
     */
    private fun resolveNestedClassToClassId(
        name: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)? = null,
    ): ClassId? {
        return resolveNestedClassToClassIdFromParts(name.split('.'), tryResolve, getSupertypeClassIds, checkInheritance = true)
    }

    /**
     * Unified internal workhorse for nested-class resolution.
     * [checkInheritance] controls whether inherited-inner-class lookup is enabled (false → the
     * `WithoutInheritance` flavor used as a reentrance-safe fallback from
     * [resolveInheritedInnerClassToClassId]). Keeping a single implementation prevents the two
     * copies from drifting when one is updated.
     *
     * Operates on a pre-split parts list to avoid O(n²) [split] + [joinToString]
     * allocations on recursive calls.
     */
    private fun resolveNestedClassToClassIdFromParts(
        parts: List<String>,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)?,
        checkInheritance: Boolean,
    ): ClassId? {
        // Try resolving increasing prefixes as outer classes using normal resolution rules.
        // This respects JLS 6.5.2: nested class takes priority when the outer class is in scope.
        for (i in 1 until parts.size) {
            val outerParts = parts.subList(0, i)
            val nestedParts = parts.subList(i, parts.size)

            val outerClassId = if (outerParts.size > 1) {
                resolveNestedClassToClassIdFromParts(outerParts, tryResolve, getSupertypeClassIds, checkInheritance)
            } else {
                resolveSimpleNameToClassIdImpl(outerParts[0], tryResolve, getSupertypeClassIds = null, checkInheritance = checkInheritance)
            }

            if (outerClassId != null) {
                val nestedClassName = FqName.fromSegments(
                    outerClassId.relativeClassName.pathSegments().map { it.asString() } + nestedParts
                )
                val nestedClassId = ClassId(outerClassId.packageFqName, nestedClassName, isLocal = false)
                if (tryResolve(nestedClassId)) return nestedClassId

                // Nested class not directly declared — search supertypes for inherited inner classes.
                // This handles cases like SimpleFunctionDescriptor.CopyBuilder where CopyBuilder is
                // declared in FunctionDescriptor (superinterface) but referenced via SimpleFunctionDescriptor.
                if (checkInheritance && nestedParts.size == 1 && getSupertypeClassIds != null) {
                    val inherited = findInheritedNestedClass(
                        outerClassId, nestedParts[0], tryResolve, getSupertypeClassIds, mutableSetOf()
                    )
                    if (inherited != null) return inherited
                }
            }
        }

        // Also try inherited inner class resolution via the aggregated map from the class finder.
        // Covers same-package source supertypes that the [getSupertypeClassIds] callback does not see.
        val finder = unitContext.classFinder
        if (checkInheritance && getSupertypeClassIds == null && finder != null && parts.size == 2) {
            val outerClassId = resolveSimpleNameToClassIdImpl(parts[0], tryResolve, getSupertypeClassIds = null, checkInheritance = true)
            if (outerClassId != null) {
                val inheritedInners = finder.collectInheritedInnerClasses(outerClassId)
                val candidates = inheritedInners[parts[1]]
                if (candidates != null && candidates.size == 1) {
                    val candidateClassId = candidates.first()
                    if (tryResolve(candidateClassId)) return candidateClassId
                }
            }
        }

        // Fall back: try as fully qualified name with different package/class splits
        // (longest package to shortest).
        return probeFqnSplits(parts, tryResolve)
    }

    /**
     * Resolve a simple (non-nested) type name to ClassId.
     */
    private fun resolveSimpleNameToClassId(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)? = null,
    ): ClassId? = resolveSimpleNameToClassIdImpl(simpleName, tryResolve, getSupertypeClassIds, checkInheritance = true)

    /**
     * Unified workhorse for simple-name resolution.
     *
     * Tries the five JLS resolution steps in priority order. [checkInheritance] gates the
     * inheritance-aware steps (local/inner class lookup and class-level star imports); when
     * `false`, only the simpler reentrance-safe fallback paths are taken.
     */
    private fun resolveSimpleNameToClassIdImpl(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)?,
        checkInheritance: Boolean,
    ): ClassId? {
        resolveFromExplicitImport(simpleName, tryResolve, checkInheritance)?.let { return it }
        if (checkInheritance) {
            resolveFromLocalScope(simpleName, tryResolve, getSupertypeClassIds)?.let { return it }
        }
        resolveFromSamePackage(simpleName, tryResolve)?.let { return it }
        resolveFromJavaLang(simpleName, tryResolve)?.let { return it }
        return resolveFromStarImports(simpleName, tryResolve, checkInheritance)
    }

    /** Step 1: Explicit single-type imports (JLS 7.5.1) — highest priority. */
    private fun resolveFromExplicitImport(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        checkInheritance: Boolean,
    ): ClassId? {
        val imported = unitContext.simpleImports[simpleName] ?: return null
        if (checkInheritance) {
            // Use resolveAsClassId to handle nested class FQNs like "a.x.b.b.b" where
            // ClassId.topLevel would incorrectly split as package="a.x.b.b", class="b".
            return resolveAsClassId(imported, tryResolve)
        }
        val classId = ClassId.topLevel(imported)
        return if (tryResolve(classId)) classId else null
    }

    /**
     * Step 2: Local/inner classes and inherited inner classes (JLS 6.5.2).
     *
     * **Stage 4 of the resolver-unification refactoring** (see
     * `implDocs/RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`). The previous AST-side
     * 2a path (`findLocalClass(name).fqName -> tryResolve(classId)`) — which threaded
     * through all five [JavaScopeResolver.findLocalClass] steps to produce a candidate
     * `ClassId` — is collapsed into the Stage-4 spec's two-step shape:
     *
     * 1. **Containing-chain walk via FIR.** Iterate [getContainingClassIds] from innermost
     *    to outermost and probe `containingId.createNestedClassId(name)` via [tryResolve].
     *    Subsumes [JavaScopeResolver.findLocalClass] steps 1, 2 and 4 (directly-declared
     *    inner classes anywhere up the containing chain).
     * 2. **Inherited-inner walk via FIR.** Existing aggregated-map / two-phase BFS
     *    ([resolveInheritedInnerClassToClassId]). Subsumes step 3 of the AST `findLocalClass`
     *    (inherited inner classes from same-file or cross-file supertypes — Java source via
     *    [LeanJavaClassFinder.collectInheritedInnerClasses], Kotlin/binary via
     *    [getSupertypeClassIds]).
     *
     * Step 5 of the AST `findLocalClass` (same-file top-level fast path) is **not**
     * reproduced here: same-file top-level classes share their `ClassId` with same-package
     * cross-file classes (`ClassId(packageFqName, simpleName)`), so they are picked up by
     * [resolveFromSamePackage] (the next step in [resolveSimpleNameToClassIdImpl]). The AST
     * fast path remains in [JavaScopeResolver.findLocalClass] for the AST classifier path
     * ([JavaTypeOverAst.computeClassifier]); the Stage-5 vision of letting the AST side
     * answer only "type parameter?" + "containingClassIds" is documented as a deferred
     * concern in [JavaScopeResolver.findLocalClass]'s KDoc.
     */
    private fun resolveFromLocalScope(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)?,
    ): ClassId? {
        // 2a (Stage 4): Walk the containing chain via FIR `tryResolve`.
        // Equivalent to (and replacing) the previous `findLocalClass(name).fqName` lookup
        // that fanned out through `JavaScopeResolver.findLocalClass` steps 1, 2, 4 — those
        // steps queried directly-declared inner classes on each level of the containing
        // chain syntactically, but the resulting `ClassId(packageFqName, ...)` is identical
        // to what the FIR symbol provider would resolve `containingId.createNestedClassId(name)`
        // to (FIR's `JvmSymbolProvider` -> `JavaClassFinderOverAstImpl` resolves it through
        // the same AST node when the inner is in source). The walk preserves the
        // innermost-wins priority ordering required by JLS 6.3.
        val nameId = Name.identifier(simpleName)
        for (containingId in getContainingClassIds()) {
            val candidate = containingId.createNestedClassId(nameId)
            if (tryResolve(candidate)) return candidate
        }

        // 2b. Inherited inner classes from supertypes (cross-file, e.g., Kotlin classes).
        // Use the aggregated inherited inner classes map (cached per context) for BOTH
        // ambiguity detection AND as a fast path.
        val aggregatedInherited = getAggregatedInheritedInnerClasses()
        if (aggregatedInherited != null) {
            val allCandidates = aggregatedInherited[simpleName] ?: emptySet()
            when {
                allCandidates.size > 1 -> return null // Ambiguously inherited – don't resolve
                allCandidates.size == 1 -> {
                    val candidateClassId = allCandidates.first()
                    if (tryResolve(candidateClassId)) return candidateClassId
                }
                // allCandidates.isEmpty(): fall back to BFS only when [getSupertypeClassIds]
                // is available, since the BFS needs it to walk supertypes through
                // Kotlin / binary classes (and, post-Stage-3, Java-source classes too).
                else -> {
                    if (getSupertypeClassIds != null) {
                        val inheritedResult = resolveInheritedInnerClassToClassId(simpleName, tryResolve, getSupertypeClassIds)
                        if (inheritedResult != null) return inheritedResult
                    }
                }
            }
        } else {
            // No class finder available — use the full BFS as fallback.
            val inheritedResult = resolveInheritedInnerClassToClassId(simpleName, tryResolve, getSupertypeClassIds)
            if (inheritedResult != null) return inheritedResult
        }
        return null
    }

    /** Step 3: Same package — class in the current compilation unit's package. */
    private fun resolveFromSamePackage(simpleName: String, tryResolve: (ClassId) -> Boolean): ClassId? {
        val classId = ClassId(packageFqName, Name.identifier(simpleName))
        return if (tryResolve(classId)) classId else null
    }

    /** Step 4: `java.lang.*` — implicitly imported by every Java compilation unit. */
    private fun resolveFromJavaLang(simpleName: String, tryResolve: (ClassId) -> Boolean): ClassId? {
        val classId = ClassId(FqName("java.lang"), Name.identifier(simpleName))
        if (JavaToKotlinClassMap.mapJavaToKotlin(classId.asSingleFqName()) != null || tryResolve(classId)) {
            return classId
        }
        return null
    }

    /**
     * Step 5: Star imports (JLS 7.5.2).
     *
     * When [checkInheritance] is true, also handles class-level star imports (`import a.D.*`)
     * and detects ambiguity across multiple star packages. Without it, a simple linear probe
     * suffices (used only on the reentrance-safe fallback path).
     */
    private fun resolveFromStarImports(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        checkInheritance: Boolean,
    ): ClassId? {
        if (checkInheritance) {
            var foundClassId: ClassId? = null
            for (starPackage in unitContext.starImports) {
                val candidateClassId = ClassId(starPackage, Name.identifier(simpleName))
                if (tryResolve(candidateClassId)) {
                    if (foundClassId != null && foundClassId != candidateClassId) return null // Ambiguous
                    foundClassId = candidateClassId
                } else {
                    // Try class-level star import: `import a.D.*` → resolve a.D as a class,
                    // then look for nested class [simpleName] within it.
                    val outerClassId = resolveAsClassId(starPackage, tryResolve)
                    if (outerClassId != null) {
                        val nestedClassId = outerClassId.createNestedClassId(Name.identifier(simpleName))
                        if (tryResolve(nestedClassId)) {
                            if (foundClassId != null && foundClassId != nestedClassId) return null // Ambiguous
                            foundClassId = nestedClassId
                        }
                    }
                }
            }
            return foundClassId
        }
        for (starPackage in unitContext.starImports) {
            val candidateClassId = ClassId(starPackage, Name.identifier(simpleName))
            if (tryResolve(candidateClassId)) return candidateClassId
        }
        return null
    }

    /**
     * Try to resolve a simple name as an inner class inherited from supertypes.
     * Delegates to [JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId].
     */
    private fun resolveInheritedInnerClassToClassId(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        getSupertypeClassIds: ((ClassId) -> List<ClassId>)? = null,
    ): ClassId? = unitContext.inheritedMemberResolver.resolveInheritedInnerClassToClassId(
        simpleName, tryResolve, getSupertypeClassIds, containingClass,
        resolveWithoutInheritance = { name, resolve ->
            if (name.contains('.')) {
                resolveNestedClassToClassIdFromParts(name.split('.'), resolve, getSupertypeClassIds = null, checkInheritance = false)
            } else {
                resolveSimpleNameToClassIdImpl(name, resolve, getSupertypeClassIds = null, checkInheritance = false)
            }
        }
    )

    /**
     * Resolves a FqName to a ClassId by trying all possible package/class splits,
     * using the tryResolve callback to validate each candidate.
     *
     * Unlike ClassId.topLevel which only tries the trivial split at the last dot,
     * this tries all splits from longest package to shortest, so "a.x.b.b.b" will
     * try ClassId(a.x.b.b, b), ClassId(a.x.b, b.b), ClassId(a.x, b.b.b), ClassId(a, x.b.b.b).
     *
     * Used for explicit imports with nested class FQNs and for class-level star import resolution.
     */
    private fun resolveAsClassId(fqName: FqName, tryResolve: (ClassId) -> Boolean): ClassId? {
        val parts = fqName.pathSegments()
        if (parts.isEmpty()) return null
        val stringParts = parts.map { it.asString() }
        for (classStartIndex in (parts.size - 1) downTo 0) {
            val pkg = if (classStartIndex == 0) FqName.ROOT
            else FqName.fromSegments(stringParts.subList(0, classStartIndex))
            val cls = FqName.fromSegments(stringParts.subList(classStartIndex, stringParts.size))
            val classId = ClassId(pkg, cls, false)
            if (tryResolve(classId)) return classId
        }
        return null
    }

    private fun fqNameToClassId(fqName: FqName): ClassId {
        return fqNameInPackageToClassId(fqName, packageFqName)
    }

    /**
     * Returns the ClassIds of the containing class chain, from innermost to outermost.
     * Used by java-direct types to expose the containing class hierarchy to FIR.
     */
    fun getContainingClassIds(): List<ClassId> {
        val result = mutableListOf<ClassId>()
        var cls: JavaClass? = containingClass
        while (cls != null) {
            val fqName = cls.fqName
            if (fqName != null) {
                result.add(fqNameToClassId(fqName))
            }
            cls = cls.outerClass
        }
        return result
    }

    companion object {
        internal fun create(
            tree: JavaLightTree,
            classFinder: LeanJavaClassFinder? = null,
        ): JavaResolutionContext {
            val root = tree.getRoot()
            val packageFqName = JavaImportResolver.extractPackageName(tree, root)
            val (simpleImports, starImports) = JavaImportResolver.extractImports(tree, root)

            // Same-file top-level classes indexed lazily to avoid circular initialization.
            // ConcurrentHashMap + computeIfAbsent so that concurrent FIR resolution of
            // different members in the same file does not race on cache updates (and, critically,
            // does not produce two distinct JavaClassOverAst instances for the same top-level
            // class — FIR matches type parameters by object identity, so a split would cause
            // "ERROR CLASS: Unresolved name: T").
            var contextRef: JavaResolutionContext? = null
            val sameFileTopLevelClassCache = ConcurrentHashMap<Name, JavaClass>()

            val sameFileTopLevelClassProvider: (Name) -> JavaClass? = { name ->
                sameFileTopLevelClassCache[name] ?: JavaImportResolver.findTopLevelClassNode(tree, root, name)?.let { classNode ->
                    // computeIfAbsent is atomic — if another thread wins, the loser's fresh
                    // JavaClassOverAst is discarded and we return the winner's instance.
                    // Returning null from the lambda (classNode missing) leaves the key unmapped.
                    sameFileTopLevelClassCache.computeIfAbsent(name) {
                        JavaClassOverAst(classNode, tree, contextRef!!, outerClass = null)
                    }
                }
            }

            val inheritedMemberResolver = JavaInheritedMemberResolver(
                packageFqName, classFinder, sameFileTopLevelClassProvider,
            )
            val scopeResolver = JavaScopeResolver(
                sameFileTopLevelClassProvider,
                containingClass = null,
                inheritedMemberResolver,
            )

            val unitContext = CompilationUnitContext(
                packageFqName, simpleImports, starImports,
                inheritedMemberResolver, classFinder,
            )
            return JavaResolutionContext(
                unitContext = unitContext,
                scopeResolver = scopeResolver,
            ).also { contextRef = it }
        }

        /**
         * Extracts imports from a root AST node.
         * Delegates to [JavaImportResolver.extractImports].
         */
        internal fun extractImports(tree: JavaLightTree, root: JavaLightNode): Pair<Map<String, FqName>, List<FqName>> =
            JavaImportResolver.extractImports(tree, root)
    }
}

/**
 * Probe every package/class split of [parts] from longest package prefix down to the root package,
 * returning the first [ClassId] accepted by [tryResolve].
 *
 * Mirrors the fallback branch of the session-aware `findClassId(fqn, session, accept)` in
 * `compiler/fir/fir-jvm/.../JavaTypeConversion.kt` (which additionally uses
 * `FirSymbolNamesProvider.getPackageNames()` to skip impossible packages on the fast path). We
 * keep a local copy here because `java-direct` must not depend on `fir-jvm`; the two probe loops
 * are intentionally identical so they can be kept in sync by inspection.
 */
private fun probeFqnSplits(parts: List<String>, tryResolve: (ClassId) -> Boolean): ClassId? {
    if (parts.isEmpty()) return null
    for (classStartIndex in (parts.size - 1) downTo 0) {
        val packageFqName = if (classStartIndex == 0) FqName.ROOT
        else FqName.fromSegments(parts.subList(0, classStartIndex))
        val relativeClassName = FqName.fromSegments(parts.subList(classStartIndex, parts.size))
        val candidate = ClassId(packageFqName, relativeClassName, isLocal = false)
        if (tryResolve(candidate)) return candidate
    }
    return null
}
