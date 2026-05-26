/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.util.findTopLevelClassNode
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.classId
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
 * - [JavaScopeForContext] — type parameter scoping and current scope class lookup
 * - [JavaInheritedMemberResolver] — supertype hierarchy traversal for inner classes
 */
class JavaResolutionContext private constructor(
    private val unitContext: CompilationUnitContext,
    private val scopeResolver: JavaScopeForContext,
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
        // Synchronise so concurrent readers don't both walk the outer chain and call
        // collectInheritedInnerClasses(...) per hop; the @Volatile fast-path above keeps the
        // hot read lock-free once the cache is populated.
        synchronized(inheritedInnerCache) {
            inheritedInnerCache.value?.let { return it }
            val merged = mutableMapOf<String, MutableSet<ClassId>>()
            var current: JavaClass? = scopeResolver.containingClass
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
    }

    /**
     * Finds a class by simple name in the AST-side scope. Delegates to
     * [JavaScopeForContext.findClassInCurrentScope]; see that method's KDoc for the five-step
     * ordering and for the post-Stage-4 role (AST classifier fast path only — no longer
     * in the `ClassId`-resolution path inside [resolveFromLocalScope]).
     */
    fun findClassInCurrentScope(name: Name): JavaClass? =
        scopeResolver.findClassInCurrentScope(name, unitContext.inheritedMemberResolver)

    /**
     * Searches the supertype hierarchy of [outerClassId] for an inherited nested class with
     * [nestedName]. Dispatches via [directSupertypeClassIds] and probes via [tryResolve]; cycles
     * are bounded by [JavaSupertypeLoopChecker].
     */
    private fun findInheritedNestedClass(
        outerClassId: ClassId,
        nestedName: String,
    ): ClassId? {
        // Read supertypes BEFORE the loop guard: [directSupertypeClassIds] shares the same
        // [JavaSupertypeLoopChecker] keyed by `classId` and would bail out if entered re-entrantly.
        val supers = directSupertypeClassIds(outerClassId)
        return unitContext.loopChecker.guarded(outerClassId, default = null) {
            for (supertypeId in supers) {
                val candidateId = supertypeId.createNestedClassId(Name.identifier(nestedName))
                if (tryResolve(candidateId)) return@guarded candidateId
                // Recurse into supertype's supertypes
                findInheritedNestedClass(supertypeId, nestedName)?.let { return@guarded it }
            }

            // Also check via the class finder for same-package Java source supertypes
            val finder = unitContext.classFinder
            if (finder != null) {
                val inheritedInners = finder.collectInheritedInnerClasses(outerClassId)
                val candidates = inheritedInners[nestedName]
                if (candidates != null && candidates.size == 1) {
                    val candidateClassId = candidates.first()
                    if (tryResolve(candidateClassId)) return@guarded candidateClassId
                }
            }

            null
        }
    }

    /**
     * Builtins-filtered class-existence probe: `true` if [classId] is known to the session's
     * symbol provider and not a Kotlin builtin (matching PSI behaviour for stdlib). Returns
     * `false` for sessions with no symbol provider — AST-only resolution paths (type parameters,
     * current scope classes, multi-part navigation) still work without it.
     */
    internal fun tryResolve(classId: ClassId): Boolean =
        unitContext.session.cycleSafeTryResolveClass(classId)

    /**
     * Whether [classId] denotes an annotation class whose declared `@Target` lists `TYPE_USE`
     * (Java) or `TYPE` (Kotlin). Used by [org.jetbrains.kotlin.java.direct.model.JavaTypeOverAst]
     * to pre-filter `memberAnnotations` so the FIR layer no longer needs the
     * `JavaTypeWithExternalAnnotationFiltering` callback bridge.
     *
     * Cached per session via [JavaModelTypeUseClassIdCache]; the underlying probe goes through
     * [cycleSafeClassLikeSymbol] so KT-74097-class cycles cannot fire here either.
     */
    internal fun isTypeUseAnnotationClass(classId: ClassId): Boolean =
        unitContext.session.isTypeUseAnnotationClass(classId)

    /**
     * Cross-language constant-field resolution used by
     * [org.jetbrains.kotlin.java.direct.model.JavaFieldOverAst]'s `initializerValue` to evaluate
     * qualified references such as `Foo.BAR` where `Foo` is a Kotlin class / facade. Returns
     * `null` when no `const val` is found.
     *
     * `currentPackage` defaults to this context's compilation-unit package, matching how the
     * Java field's `containingClass` lives in the same compilation unit.
     */
    internal fun resolveExternalFieldValue(classQualifier: String?, fieldName: String): Any? =
        unitContext.session.resolveExternalFieldValue(classQualifier, fieldName, packageFqName)

    /**
     * Const-vs-enum-entry disambiguation used by
     * [org.jetbrains.kotlin.java.direct.model.createAnnotationArgumentFromValue] for annotation
     * arguments that look syntactically like enum entries but may denote a Kotlin `const val`.
     * Returns `null` when the reference is a real enum entry or unresolvable.
     */
    internal fun resolveConstFieldValue(classId: ClassId, fieldName: Name): Any? =
        unitContext.session.resolveConstFieldValue(classId, fieldName)

    /**
     * Wraps [classId] in a [FirBackedJavaClassAdapter] backed by this context's session, or
     * `null` when the session has no [FirSymbolProvider] (parsing-level unit fixtures): the
     * adapter could not materialise its fields, and FIR-side `findClassIdByFqNameString`
     * handles such references instead.
     */
    internal fun classifierAdapterFor(classId: ClassId): JavaClass? {
        val session = unitContext.session
        return if (session.nullableSymbolProvider != null) FirBackedJavaClassAdapter(classId, session) else null
    }

    /**
     * Per-origin direct-supertype-`ClassId` dispatcher, guarded by [JavaSupertypeLoopChecker]
     * so direct (`A extends A`) and indirect (`A → B → A`) Java-side cycles terminate cleanly
     * with a logged edge.
     *
     *  1. **Source Java arm** — `classFinder.findClass(classId)` hits: walk `JavaClass.supertypes`
     *     directly (no FIR phase involved).
     *  2. **Binary Java arm** — FIR symbol is a [FirJavaClass]: read the pre-resolved
     *     [FirJavaClass.directSupertypeClassIds] cache (no enhancement triggered).
     *  3. **Kotlin / built-in / deserialized arm** — `lazyResolveToPhase(SUPER_TYPES)` is honest
     *     in compiler mode (the eager driver finishes these before Java member conversion runs).
     *     Cycles on this arm are bounded by FIR's `SupertypeComputationStatus.Computing` sentinel,
     *     not by the model-side checker.
     */
    @OptIn(SymbolInternals::class)
    internal fun directSupertypeClassIds(classId: ClassId): List<ClassId> =
        unitContext.loopChecker.guarded(classId, default = emptyList()) {
            // 1. Source Java arm — walk our own AST. Supertype names are syntactically
            // knowable; no FIR phase is involved.
            val finder = unitContext.classFinder
            if (finder != null && finder.isClassInIndex(classId)) {
                val javaClass = finder.findClass(JavaClassFinder.Request(classId))
                if (javaClass != null) {
                    return@guarded resolveSupertypeNames(javaClass)
                }
            }

            // 2. & 3. Look up the FIR symbol — the model's only handle for non-source-Java
            // classes (binary Java, Kotlin, deserialized).
            val symbol = unitContext.session.cycleSafeClassLikeSymbol(classId) ?: return@guarded emptyList()
            val firClass = symbol.fir as? FirRegularClass ?: return@guarded emptyList()

            // 2. Binary Java arm — read the pre-resolved cache on FirJavaClass; never
            // touches the lazy `superTypeRefs` enhancement.
            if (firClass is FirJavaClass) {
                return@guarded firClass.directSupertypeClassIds()
            }

            // 3. Kotlin / built-in / deserialized arm — lazyResolveToPhase is honest here.
            symbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
            firClass.superTypeRefs.mapNotNull { ref ->
                ((ref as? FirResolvedTypeRef)?.coneType as? ConeClassLikeType)?.lookupTag?.classId
            }
        }

    /**
     * Resolves the supertype names of a Java source [enclosing] class to a list of
     * direct-supertype [ClassId]s. Reads the materialised `classifier` field on each
     * [JavaClassifierType] in [JavaClass.supertypes] — under post-Step-4.5a injection that
     * field is reliable for every reference (cross-file too).
     */
    private fun resolveSupertypeNames(enclosing: JavaClass): List<ClassId> =
        enclosing.supertypes.mapNotNull { supertype ->
            (supertype.classifier as? JavaClass)?.classId
        }

    /** Returns type parameters with HIGH priority (method/class own params, win over inner class names). */
    fun findTypeParameter(name: String): JavaTypeParameter? = scopeResolver.findTypeParameter(name)

    /** Returns type parameters with LOW priority (outer class inherited params, shadowed by inner class names). */
    fun findInheritedTypeParameter(name: String): JavaTypeParameter? = scopeResolver.findInheritedTypeParameter(name)

    /**
     * Unified single-import lookup ([JavaImports.getSingleImport]): tries the single-type-import
     * bucket first, then the single-static-import bucket. Used by model-side consumers that
     * need a yes/no answer to "is there *any* single-import of this simple name?".
     *
     * The dispatcher inside [resolveSimpleNameToClassIdImpl] does not call this method — it
     * probes [JavaImports.simpleTypeImports] and [JavaImports.staticSingleImports] separately
     * so it can keep the JLS rank-4 ordering between them explicit, and so the static-single
     * arm can use the right `ClassId`-shape probe (outer class FqName -> nested-class shape).
     */
    fun getSimpleImport(simpleName: String): FqName? = unitContext.imports.getSingleImport(simpleName)

    /**
     * Static-only single-import lookup: returns the FqName of an `import static a.b.C.X;`
     * declaration if and only if [simpleName] was imported in that shape. Used by
     * [org.jetbrains.kotlin.java.direct.model.JavaEnumValueAnnotationArgumentOverAst] to
     * recover the implicit `Outer.member` enum-entry binding from a bare identifier.
     */
    fun getStaticImport(simpleName: String): FqName? = unitContext.imports.staticSingleImports[simpleName]

    /**
     * Returns the parsed imports (four-bucket [JavaImports]) from this context.
     * Used by [JavaClassFinderOverAstImpl.getDirectSupertypes] on the fast path
     * to avoid re-extracting imports from the AST root.
     */
    internal fun getImports(): JavaImports = unitContext.imports

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
        // Consults both single-type and single-static buckets via the unified accessor:
        // a static-single-import of a *type* is also resolvable as a Java class (JLS 6.4.1
        // rank 4, same as a single-type-import); a static-single-import of a method/field
        // simply fails the downstream `tryResolve`. Distinguishing the two costs nothing here
        // because the consumer only needs a yes/no on "could this be a class?".
        val importedFqn = unitContext.imports.getSingleImport(simpleName) ?: return false
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
        // Only type-import-on-demand makes sense for the `ClassId(pkg, simpleName)` shape:
        // static-import-on-demand (`import static a.b.C.*;`) holds an outer-class FqName, not
        // a package, and would need the nested-class shape — which this best-effort accessor
        // is not the place for. Callers that need static-star fallback go through the full
        // [resolve] dispatcher.
        val starPackage = unitContext.imports.typeStarImports.firstOrNull() ?: return null
        return ClassId(starPackage, Name.identifier(simpleName))
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
            // new holder — containingClass changed, aggregated inherited inner classes may differ
        )
    }

    /**
     * Resolve a type name to a [ClassId] using the model's own resolution data path.
     *
     * This method returns a [ClassId] directly, which unambiguously encodes the
     * package/class boundary. For example, `"a.b"` could mean either:
     * - `ClassId("a", "b")` — package `a`, class `b`
     * - `ClassId("", "a.b")` — root package, nested class `a.b`
     *
     * Using [ClassId] avoids the ambiguity that string-based resolution has.
     *
     * Probes the FIR symbol provider via [tryResolve] (builtins-filtered) and walks supertypes
     * via [directSupertypeClassIds]; both go through this context's [FirSession].
     */
    fun resolve(name: String): ClassId? {
        // Handle nested class references like "Map.Entry"
        if (name.contains('.')) {
            // Cache tryResolve results within this invocation. The recursive prefix splitting
            // in resolveQualifiedNameToClassId probes the same ClassIds many times (e.g., "com"
            // is tried as a class for each prefix of "com.google.protobuf.Foo"). The probe is
            // deterministic within a single resolve() call, so caching is safe.
            //
            // Only allocated for dotted names — simple names (the majority) go through
            // resolveSimpleNameToClassId directly, avoiding the HashMap allocation entirely.
            val cache = HashMap<ClassId, Boolean>()
            val cachedTryResolve: (ClassId) -> Boolean = { classId ->
                cache.getOrPut(classId) { tryResolve(classId) }
            }
            return resolveQualifiedNameToClassId(name, cachedTryResolve)
        }
        return resolveSimpleNameToClassId(name, ::tryResolve)
    }

    /**
     * Resolve a qualified (dot-separated) Java type name to a [ClassId].
     *
     * Per JLS 6.5.2, when a qualified name `Q.Id` could refer to either:
     * - A nested class `Id` of class `Q`, or
     * - A top-level class `Id` in package `Q`
     *
     * the nested-class interpretation takes priority, BUT only if `Q` actually resolves
     * to a class in the current scope. We try to resolve `Q` as a class first using
     * the normal resolution rules (same package, imports, etc.). If `Q` resolves to a class,
     * we try `Q.Id` as a nested class. If that fails or `Q` doesn't resolve, we fall back
     * to trying `Q.Id` as a fully qualified name via [probeFqnSplits].
     *
     * Thin wrapper around [resolveQualifiedNameToClassIdFromParts]: pre-splits [name] once
     * so recursive prefix probes don't pay [split]/[joinToString] over and over.
     */
    private fun resolveQualifiedNameToClassId(
        name: String,
        tryResolve: (ClassId) -> Boolean,
    ): ClassId? {
        return resolveQualifiedNameToClassIdFromParts(name.split('.'), tryResolve, checkInheritance = true)
    }

    /**
     * Unified internal workhorse for qualified-name resolution. Implements JLS 6.5.2
     * priority (nested-class interpretation first, when the outer is a class in scope)
     * and falls back to plain `package.Class` splits via [probeFqnSplits] when no
     * JLS 6.5.2 outer is in scope. Despite the historical "nested" naming used inside this
     * file, this is the entry point for *all* dotted Java type names — fully qualified ones
     * like `java.util.Map` reach `tryResolve` only through the [probeFqnSplits] tail.
     *
     * [checkInheritance] controls whether inherited-inner-class lookup is enabled (false → the
     * `WithoutInheritance` flavor used as a reentrance-safe fallback from
     * [resolveInheritedInnerClassToClassId]). Keeping a single implementation prevents the two
     * copies from drifting when one is updated.
     *
     * Operates on a pre-split parts list to avoid O(n²) [split] + [joinToString]
     * allocations on recursive calls.
     */
    private fun resolveQualifiedNameToClassIdFromParts(
        parts: List<String>,
        tryResolve: (ClassId) -> Boolean,
        checkInheritance: Boolean,
    ): ClassId? {
        // Try resolving increasing prefixes as outer classes using normal resolution rules.
        // This respects JLS 6.5.2: nested class takes priority when the outer class is in scope.
        for (i in 1 until parts.size) {
            val outerParts = parts.subList(0, i)
            val nestedParts = parts.subList(i, parts.size)

            val outerClassId = if (outerParts.size > 1) {
                resolveQualifiedNameToClassIdFromParts(outerParts, tryResolve, checkInheritance)
            } else {
                resolveSimpleNameToClassIdImpl(outerParts[0], tryResolve, checkInheritance = checkInheritance)
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
                if (checkInheritance && nestedParts.size == 1) {
                    val inherited = findInheritedNestedClass(outerClassId, nestedParts[0])
                    if (inherited != null) return inherited
                }
            }
        }

        // Re-entrance-safe finder fallback for the `Outer.Inner` shape: when the upper loop's
        // `findInheritedNestedClass(...)` was short-circuited because `outerClassId` is currently
        // mid-resolution on the supertype-loop-checker stack, the loop guard skips its own
        // `finder.collectInheritedInnerClasses(...)` tail. Re-run that probe here without the
        // loop guard. Limited to `parts.size == 2` because that is the exact shape
        // `collectInheritedInnerClasses` is keyed by (one outer `ClassId`, one inner simple
        // name) and because `parts[0]` is treated as a simple name here — multi-segment
        // package qualifiers like `java.util.Map.Entry` are intentionally handed off to
        // [probeFqnSplits] below.
        val finder = unitContext.classFinder
        if (checkInheritance && finder != null && parts.size == 2) {
            val outerClassId = resolveSimpleNameToClassIdImpl(parts[0], tryResolve, checkInheritance = true)
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
    ): ClassId? = resolveSimpleNameToClassIdImpl(simpleName, tryResolve, checkInheritance = true)

    /**
     * Unified workhorse for simple-name resolution.
     *
     * Tries the seven resolution steps in JLS 6.4.1 priority order:
     *
     *  1. Member type of the enclosing class — own and inherited inners (JLS 6.4.1).
     *  2. Top-level type declared in the **same compilation unit** (JLS 6.4.1).
     *  3a. Single-type imports (`import a.b.C;`, JLS 7.5.1) — always a type, rank 4.
     *  3b. Single-static imports (`import static a.b.C.X;`, JLS 7.5.3) — rank 4. The referent
     *      `X` may be a type, a method, or a field; only the type case contributes here, the
     *      other two drop out cleanly when `tryResolve` returns `false`. The probe uses the
     *      nested-class-aware [resolveAsClassId] split because the imported FqName always
     *      ends with the type's simple name and the prefix is the outer class.
     *  4. Same-package top-level type from another compilation unit (JLS 6.4.1).
     *  5. `java.lang.*` (JLS 7.3 — implicitly imported).
     *  6. Type-import-on-demand (`import a.b.*;`, JLS 7.5.2) — rank 6.
     *  7. Static-import-on-demand (`import static a.b.C.*;`, JLS 7.5.4) — rank 7 (strictly
     *     lower than rank 6 per JLS 6.4.1). The downstream probe uses the nested-class shape
     *     (outer-class FqName → outer ClassId → `outerClassId.createNestedClassId(name)`).
     *
     * [checkInheritance] gates the inheritance-aware steps; when `false`, only the simpler
     * reentrance-safe fallback paths are taken — Step 1 is skipped (the local-scope lookup
     * walks the FIR containing chain and inherited-inner BFS, both of which can re-enter
     * resolution), and the explicit/star steps fall back to their non-inheritance flavors
     * for nested-import FQNs.
     */
    private fun resolveSimpleNameToClassIdImpl(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        checkInheritance: Boolean,
    ): ClassId? {
        // JLS 6.4.1: member types of the enclosing class shadow single-type imports.
        if (checkInheritance) {
            resolveFromLocalScope(simpleName, tryResolve)?.let { return it }
        }
        // JLS 6.4.1: same-compilation-unit top-level types shadow single-type imports.
        resolveFromSameCompilationUnit(simpleName, tryResolve)?.let { return it }
        // JLS 7.5.1: single-type imports.
        resolveFromExplicitImport(simpleName, tryResolve, checkInheritance)?.let { return it }
        // JLS 7.5.3: single-static imports (rank 4, same as 7.5.1; tried after).
        resolveFromStaticSingleImport(simpleName, tryResolve)?.let { return it }
        // JLS 6.4.1: same-package top-level types from *other* compilation units are
        // shadowed by the import (Step 3), so this step runs after it.
        resolveFromSamePackage(simpleName, tryResolve)?.let { return it }
        // JLS 7.3: java.lang.* is implicitly imported.
        resolveFromJavaLang(simpleName, tryResolve)?.let { return it }
        // JLS 7.5.2: type-import-on-demand.
        resolveFromTypeStarImports(simpleName, tryResolve)?.let { return it }
        // JLS 7.5.4: static-import-on-demand (strictly lower rank than 7.5.2).
        return resolveFromStaticStarImports(simpleName, tryResolve, checkInheritance)
    }

    /**
     * Step 2: Top-level type declared in the **same compilation unit** as the resolving
     * reference. Per JLS 6.4.1, such a type shadows any single-type-import of the same
     * simple name.
     *
     * Driven by [JavaScopeForContext.sameFileTopLevelClassProvider], which is the only
     * source of truth for "is `simpleName` declared as a top-level class in *this* file?".
     * The bare `ClassId(packageFqName, simpleName)` probe used by [resolveFromSamePackage]
     * cannot distinguish same-file from cross-file because both share the same `ClassId`.
     */
    private fun resolveFromSameCompilationUnit(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
    ): ClassId? {
        scopeResolver.sameFileTopLevelClassProvider(Name.identifier(simpleName)) ?: return null
        val classId = ClassId(packageFqName, Name.identifier(simpleName))
        return if (tryResolve(classId)) classId else null
    }

    /**
     * Step 3a: Explicit single-type imports (JLS 7.5.1).
     *
     * Per JLS 6.4.1, single-type imports are *shadowed* by both member types of the
     * enclosing class (Step 1) and same-compilation-unit top-level types (Step 2), so this
     * step runs only after both of those have missed. Same-package types declared in
     * *other* compilation units, on the other hand, are shadowed *by* this step — they
     * appear at Step 4.
     */
    private fun resolveFromExplicitImport(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        checkInheritance: Boolean,
    ): ClassId? {
        val imported = unitContext.imports.simpleTypeImports[simpleName] ?: return null
        if (checkInheritance) {
            // Use resolveAsClassId to handle nested class FQNs like "a.x.b.b.b" where
            // ClassId.topLevel would incorrectly split as package="a.x.b.b", class="b".
            return resolveAsClassId(imported, tryResolve)
        }
        val classId = ClassId.topLevel(imported)
        return if (tryResolve(classId)) classId else null
    }

    /**
     * Step 3b: Single-static imports (JLS 7.5.3) — the type-only arm.
     *
     * `import static a.b.C.X;` brings `X` (a static member of `a.b.C`) into scope. For
     * classifier resolution, only the case where `X` is a *type* matters; the method and
     * field cases drop out cleanly when [tryResolve] returns `false`. The imported FqName
     * always ends with the type's simple name and the prefix is the outer class, so
     * [resolveAsClassId] does the right thing on its own — longest-package-first split,
     * which for `a.b.C.X` will probe `ClassId(a.b, C.X)` (success) before degenerate splits.
     *
     * Per JLS 6.4.1 this is also rank 4 — same as [resolveFromExplicitImport]. A same-simple
     * collision between a type single-import and a static-single-import of a type is
     * malformed Java in practice (`javac` flags the conflict), so the ordering rank-4-type
     * before rank-4-static is a no-op for well-formed code.
     */
    private fun resolveFromStaticSingleImport(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
    ): ClassId? {
        val imported = unitContext.imports.staticSingleImports[simpleName] ?: return null
        return resolveAsClassId(imported, tryResolve)
    }

    /**
     * Step 1: Current scope classes and inherited inner classes (JLS 6.4.1 / 6.5.2).
     *
     * Per JLS 6.4.1, member types of the enclosing class (own and inherited) shadow
     * single-type imports of the same simple name within the class body, so this step
     * runs *before* [resolveFromExplicitImport].
     *
     * Two-step shape:
     *
     * 1. **Containing-chain walk via FIR.** Iterate [getContainingClassIds] from innermost
     *    to outermost and probe `containingId.createNestedClassId(name)` via [tryResolve].
     *    Preserves the innermost-wins priority of JLS 6.3.
     * 2. **Inherited-inner walk via FIR.** Aggregated-map / two-pass BFS in
     *    [resolveInheritedInnerClassToClassId].
     *
     * Same-file top-level classes are NOT resolved here: they share their `ClassId` with
     * same-package cross-file classes, so [resolveFromSamePackage] picks them up at the
     * next step. The AST fast path remains in [JavaScopeForContext.findClassInCurrentScope] for the
     * AST classifier path ([JavaTypeOverAst.computeClassifier]).
     */
    private fun resolveFromLocalScope(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
    ): ClassId? {
        // 2a (Stage 4): Walk the containing chain via FIR `tryResolve`.
        // Equivalent to (and replacing) the previous `findClassInCurrentScope(name).fqName` lookup
        // that fanned out through `JavaScopeResolver.findClassInCurrentScope` steps 1, 2, 4 — those
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
                // allCandidates.isEmpty(): fall back to BFS via [directSupertypeClassIds].
                else -> {
                    val inheritedResult = resolveInheritedInnerClassToClassId(simpleName, tryResolve)
                    if (inheritedResult != null) return inheritedResult
                }
            }
        } else {
            // No class finder available — use the full BFS as fallback.
            val inheritedResult = resolveInheritedInnerClassToClassId(simpleName, tryResolve)
            if (inheritedResult != null) return inheritedResult
        }
        return null
    }

    /**
     * Step 4: Same-package top-level type from *another* compilation unit.
     *
     * Per JLS 6.4.1, single-type imports shadow top-level types declared in other
     * compilation units of the same package, so this step runs *after*
     * [resolveFromExplicitImport]. The probe `ClassId(packageFqName, simpleName)` also
     * matches same-file top-level types, but those are already short-circuited by Step 2
     * ([resolveFromSameCompilationUnit]), so reaching this step means "not declared in
     * this file".
     */
    private fun resolveFromSamePackage(simpleName: String, tryResolve: (ClassId) -> Boolean): ClassId? {
        val classId = ClassId(packageFqName, Name.identifier(simpleName))
        return if (tryResolve(classId)) classId else null
    }

    /** Step 5: `java.lang.*` — implicitly imported by every Java compilation unit. */
    private fun resolveFromJavaLang(simpleName: String, tryResolve: (ClassId) -> Boolean): ClassId? {
        val classId = ClassId(FqName("java.lang"), Name.identifier(simpleName))
        if (JavaToKotlinClassMap.mapJavaToKotlin(classId.asSingleFqName()) != null || tryResolve(classId)) {
            return classId
        }
        return null
    }

    /**
     * Step 6: Type-import-on-demand (`import a.b.*;`, JLS 7.5.2).
     *
     * Each entry is *nominally* a package FqName; the primary probe is
     * `ClassId(pkg, simpleName)`. However, the Kotlin compiler historically also accepts
     * `import a.D.*;` where `a.D` is a *class* (strictly illegal per JLS — must be
     * `import static a.D.*;`) and resolves nested types through it; the test suite
     * (`testImportThriceNestedClass`, `testNestedAndTopLevelClassClash`) relies on this
     * permissive behaviour. So when the package-shape probe misses, we additionally try
     * the class-level fallback: resolve the entry as a class via [resolveAsClassId], then
     * probe `outerClassId.createNestedClassId(simpleName)`. The strictly-static variant
     * (`import static a.b.C.*;`) is handled by [resolveFromStaticStarImports] at rank 7.
     *
     * Detects ambiguity across multiple star entries: if two distinct entries resolve
     * the same simple name to different ClassIds, returns `null` (JLS 7.5.2 calls this a
     * compile-time error; the dispatcher naturally falls through to Step 7).
     */
    private fun resolveFromTypeStarImports(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
    ): ClassId? {
        var foundClassId: ClassId? = null
        for (starPackage in unitContext.imports.typeStarImports) {
            val candidateClassId = ClassId(starPackage, Name.identifier(simpleName))
            if (tryResolve(candidateClassId)) {
                if (foundClassId != null && foundClassId != candidateClassId) return null // Ambiguous
                foundClassId = candidateClassId
            } else {
                // Class-level fallback (`import a.D.*` where `a.D` is a class): resolve the
                // entry as a ClassId and form the nested-class shape.
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

    /**
     * Step 7: Static-import-on-demand (`import static a.b.C.*;`, JLS 7.5.4).
     *
     * Strictly lower JLS shadowing rank than [resolveFromTypeStarImports] (rank 7 vs 6).
     * Each entry is the *outer class* FqName — not a package. The probe shape is:
     *
     *  1. Resolve the outer-class FqName via [resolveAsClassId] (longest-package-first split).
     *  2. Form `outerClassId.createNestedClassId(simpleName)` and probe via [tryResolve].
     *
     * Without [checkInheritance] (reentrance-safe fallback path) only direct nested-class
     * resolution is attempted; that matches the prior `checkInheritance == false` branch of
     * the merged star step.
     */
    private fun resolveFromStaticStarImports(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
        checkInheritance: Boolean,
    ): ClassId? {
        var foundClassId: ClassId? = null
        for (outerFqName in unitContext.imports.staticStarImports) {
            val outerClassId = resolveAsClassId(outerFqName, tryResolve) ?: continue
            val nestedClassId = outerClassId.createNestedClassId(Name.identifier(simpleName))
            if (tryResolve(nestedClassId)) {
                if (!checkInheritance) return nestedClassId
                if (foundClassId != null && foundClassId != nestedClassId) return null // Ambiguous
                foundClassId = nestedClassId
            }
        }
        return foundClassId
    }

    /**
     * Try to resolve a simple name as an inner class inherited from supertypes. The BFS reads
     * supertypes through the per-origin [directSupertypeClassIds] dispatcher.
     */
    private fun resolveInheritedInnerClassToClassId(
        simpleName: String,
        tryResolve: (ClassId) -> Boolean,
    ): ClassId? = unitContext.inheritedMemberResolver.resolveInheritedInnerClassToClassId(
        simpleName, tryResolve, ::directSupertypeClassIds, scopeResolver.containingClass,
        resolveWithoutInheritance = { name, resolve ->
            if (name.contains('.')) {
                resolveQualifiedNameToClassIdFromParts(name.split('.'), resolve, checkInheritance = false)
            } else {
                resolveSimpleNameToClassIdImpl(name, resolve, checkInheritance = false)
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
        var cls: JavaClass? = scopeResolver.containingClass
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
            session: FirSession,
            classFinder: LeanJavaClassFinder? = null,
        ): JavaResolutionContext {
            val root = tree.getRoot()
            val packageFqName = JavaImportResolver.extractPackageName(tree, root)
            val imports = JavaImportResolver.extractImports(tree, root)

            // Same-file top-level classes indexed lazily to avoid circular initialization.
            // ConcurrentHashMap + computeIfAbsent so that concurrent FIR resolution of
            // different members in the same file does not race on cache updates (and, critically,
            // does not produce two distinct JavaClassOverAst instances for the same top-level
            // class — FIR matches type parameters by object identity, so a split would cause
            // "ERROR CLASS: Unresolved name: T").
            var contextRef: JavaResolutionContext? = null
            val sameFileTopLevelClassCache = ConcurrentHashMap<Name, JavaClass>()

            val sameFileTopLevelClassProvider: (Name) -> JavaClass? = { name ->
                sameFileTopLevelClassCache[name] ?: findTopLevelClassNode(tree, root, name)?.let { classNode ->
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
            val scopeResolver = JavaScopeForContext(
                sameFileTopLevelClassProvider,
                containingClass = null,
            )

            val unitContext = CompilationUnitContext(
                packageFqName, imports,
                inheritedMemberResolver, classFinder,
                session = session,
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
        internal fun extractImports(tree: JavaLightTree, root: JavaLightNode): JavaImports =
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
