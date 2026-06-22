/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.java.direct.model.FirBackedJavaClassifierType
import org.jetbrains.kotlin.java.direct.model.firBackedJavaType
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Stateless type-reference resolution for Java source files, operating upon the given
 * [JavaResolutionContext].
 *
 * This is the resolution **engine** — the JLS 6.4.1 simple-name dispatcher, the JLS 6.5.2
 * qualified-name dispatcher, supertype-`ClassId` walking, and the session-backed probes. The
 * data it reads ([JavaFileContext], [JavaScopeContext]) lives on the [JavaResolutionContext]; the
 * scope/type-parameter lookups live in [JavaScopeResolver]; the supertype hierarchy traversal
 * lives in [JavaInheritedMemberResolver].
 */

/**
 * Resolve a type name to a [ClassId] using the model's own resolution data path.
 *
 * This returns a [ClassId] directly, which unambiguously encodes the package/class boundary.
 * For example, `"a.b"` could mean either `ClassId("a", "b")` (package `a`, class `b`) or
 * `ClassId("", "a.b")` (root package, nested class `a.b`); using [ClassId] avoids the ambiguity
 * that string-based resolution has.
 *
 * Probes the FIR symbol provider via [tryResolve] (builtins-filtered) and walks supertypes via
 * [directSupertypeClassIds]; both go through this context's `FirSession`.
 */
context(c: JavaResolutionContext)
internal fun resolve(name: String): ClassId? {
    // Handle nested class references like "Map.Entry".
    if (name.contains('.')) {
        // Cache tryResolve results within this invocation. The recursive prefix splitting in
        // resolveQualifiedNameToClassIdFromParts probes the same ClassIds many times (e.g., "com"
        // is tried as a class for each prefix of "com.google.protobuf.Foo"). The probe is
        // deterministic within a single resolve() call, so caching is safe.
        //
        // Only allocated for dotted names — simple names (the majority) go through
        // resolveSimpleNameToClassIdImpl directly, avoiding the HashMap allocation entirely.
        val cache = HashMap<ClassId, Boolean>()
        val cachedTryResolve: (ClassId) -> Boolean = { classId ->
            cache.getOrPut(classId) { tryResolve(classId) }
        }
        // Pre-split once so recursive prefix probes don't re-split. This is the entry point for
        // *all* dotted Java type names — fully qualified ones like `java.util.Map` reach
        // `tryResolve` only through the [probeFqnSplits] tail of
        // [resolveQualifiedNameToClassIdFromParts].
        return resolveQualifiedNameToClassIdFromParts(name.split('.'), cachedTryResolve, fullResolution = true)
    }
    return resolveSimpleNameToClassIdImpl(name, { tryResolve(it) }, fullResolution = true)
}

/**
 * Unified internal workhorse for qualified-name resolution. Implements JLS 6.5.2 priority
 * (nested-class interpretation first, when the outer is a class in scope) and falls back to
 * plain `package.Class` splits via [probeFqnSplits] when no JLS 6.5.2 outer is in scope.
 *
 * [fullResolution] controls whether inherited-inner-class lookup is enabled (false → the
 * `WithoutInheritance` flavor used as a reentrance-safe fallback from
 * [resolveInheritedInnerClassToClassId]). Keeping a single implementation prevents the two
 * copies from drifting when one is updated.
 *
 * Operates on a pre-split parts list to avoid O(n²) [String.split] + `joinToString`
 * allocations on recursive calls.
 */
context(c: JavaResolutionContext)
private fun resolveQualifiedNameToClassIdFromParts(
    parts: List<String>,
    tryResolve: (ClassId) -> Boolean,
    fullResolution: Boolean,
): ClassId? {
    // Try resolving increasing prefixes as outer classes using normal resolution rules.
    // This respects JLS 6.5.2: nested class takes priority when the outer class is in scope.
    for (i in 1 until parts.size) {
        val outerParts = parts.subList(0, i)
        val nestedParts = parts.subList(i, parts.size)

        val outerClassId = if (outerParts.size > 1) {
            resolveQualifiedNameToClassIdFromParts(outerParts, tryResolve, fullResolution)
        } else {
            resolveSimpleNameToClassIdImpl(outerParts[0], tryResolve, fullResolution = fullResolution)
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
            if (fullResolution && nestedParts.size == 1) {
                val inherited = findInheritedNestedClass(outerClassId, nestedParts[0])
                if (inherited != null) return inherited
            }
        }
    }

    // Re-entrance-safe finder fallback for the `Outer.Inner` shape: when the upper loop's
    // `findInheritedNestedClass(...)` was short-circuited because `outerClassId` is currently
    // mid-resolution on the supertype-cycle-checker stack, the cycle guard skips its own
    // `finder.collectInheritedInnerClasses(...)` tail. Re-run that probe here without the
    // cycle guard. Limited to `parts.size == 2` because that is the exact shape
    // `collectInheritedInnerClasses` is keyed by (one outer `ClassId`, one inner simple
    // name) and because `parts[0]` is treated as a simple name here — multi-segment
    // package qualifiers like `java.util.Map.Entry` are intentionally handed off to
    // [probeFqnSplits] below.
    val finder = c.fileContext.classFinder
    if (fullResolution && finder != null && parts.size == 2) {
        val outerClassId = resolveSimpleNameToClassIdImpl(parts[0], tryResolve, fullResolution = true)
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
 * Unified workhorse for simple-name resolution.
 *
 * [fullResolution] selects the resolution flavor. `true` is the full primary path. `false` is the
 * reentrance-safe flavor used as a fallback from [resolveInheritedInnerClassToClassId] while an
 * inherited-inner-class walk is already in progress: it skips Step 1 and downgrades the
 * explicit/star steps to their single-split flavors — exactly the steps that would otherwise
 * recurse back into the same walk.
 * Omitting them is both correct and required for termination: the `false` flavor only turns a
 * supertype reference name into a [ClassId], which the remaining steps resolve directly, and the
 * dropped behaviors matter only for doubly-nested corner cases already handled by the primary path.
 */
context(c: JavaResolutionContext)
private fun resolveSimpleNameToClassIdImpl(
    simpleName: String,
    tryResolve: (ClassId) -> Boolean,
    fullResolution: Boolean,
): ClassId? {
    // JLS 6.4.1: member types of the enclosing class shadow single-type imports.
    if (fullResolution) {
        resolveFromLocalScope(simpleName, tryResolve)?.let { return it }
    }
    // JLS 6.4.1: same-file top-level types shadow single-type imports.
    resolveFromSameFile(simpleName, tryResolve)?.let { return it }
    // JLS 7.5.1: single-type imports.
    resolveFromExplicitImport(simpleName, tryResolve, fullResolution)?.let { return it }
    // JLS 7.5.3: single-static imports (rank 4, same as 7.5.1; tried after).
    resolveFromStaticSingleImport(simpleName, tryResolve)?.let { return it }
    // JLS 6.4.1: same-package top-level types from *other* files are
    // shadowed by the import (Step 3), so this step runs after it.
    resolveFromSamePackage(simpleName, tryResolve)?.let { return it }
    // JLS 7.3: java.lang.* is implicitly imported.
    resolveFromJavaLang(simpleName, tryResolve)?.let { return it }
    // JLS 7.5.2: type-import-on-demand.
    resolveFromTypeStarImports(simpleName, tryResolve)?.let { return it }
    // JLS 7.5.4: static-import-on-demand (strictly lower rank than 7.5.2).
    return resolveFromStaticStarImports(simpleName, tryResolve, fullResolution)
}

/**
 * Step 1: Current scope classes and inherited inner classes (JLS 6.4.1 / 6.5.2).
 *
 * Per JLS 6.4.1, member types of the enclosing class (own and inherited) shadow single-type
 * imports of the same simple name within the class body, so this step runs *before*
 * [resolveFromExplicitImport].
 *
 * Walks the containing class chain from innermost to outermost. At each level it probes the
 * member type **declared** by that class, then the member types it **inherits** from its
 * supertypes, before moving outward. Per JLS 6.4.1 a member type declared or inherited at an
 * inner level shadows one declared or inherited at an enclosing level, so the declared and
 * inherited lookups must interleave level by level. The inherited lookup mirrors the AST
 * classifier path in [findClassInCurrentScope] but additionally reaches Kotlin / binary
 * supertypes through the `ClassId` BFS.
 *
 * Same-file top-level classes are NOT resolved here: they share their `ClassId` with same-package
 * cross-file classes, so [resolveFromSamePackage] picks them up at the next step. The AST fast
 * path remains in [findClassInCurrentScope] for the AST classifier path.
 */
context(c: JavaResolutionContext)
private fun resolveFromLocalScope(
    simpleName: String,
    tryResolve: (ClassId) -> Boolean,
): ClassId? {
    val nameId = Name.identifier(simpleName)
    var current: JavaClass? = c.scopeContext.containingClass
    while (current != null) {
        val fqName = current.fqName
        if (fqName != null) {
            val containingId = fqNameToClassId(fqName)
            // Declared member type of this class. The resulting `ClassId(packageFqName, ...)` is
            // identical to what the FIR symbol provider would resolve
            // `containingId.createNestedClassId(name)` to (FIR's `JvmSymbolProvider` ->
            // `JavaClassFinderOverAstImpl` resolves it through the same AST node when the inner is
            // in source).
            val declared = containingId.createNestedClassId(nameId)
            if (tryResolve(declared)) return declared
            // Member types this class inherits from its supertypes (cross-file Java source,
            // Kotlin, and binary), restricted to this single level so the interleaving holds.
            resolveInheritedInnerForLevel(simpleName, current, containingId, tryResolve)?.let { return it }
        }
        current = current.outerClass
    }
    return null
}

/**
 * Resolves a member type [simpleName] inherited by a single class [containingClass] (identified by
 * [containingId]) from its supertypes. Uses the per-class cached map of inherited inner names for
 * Java-source supertypes (fast path + same-level ambiguity detection) and falls back to the
 * supertype BFS for the Kotlin / binary supertypes the map does not cover.
 */
context(c: JavaResolutionContext)
private fun resolveInheritedInnerForLevel(
    simpleName: String,
    containingClass: JavaClass,
    containingId: ClassId,
    tryResolve: (ClassId) -> Boolean,
): ClassId? {
    val inherited = getInheritedInnerClassesForClass(containingId)
    if (inherited != null) {
        val candidates = inherited[simpleName] ?: emptySet()
        when {
            candidates.size > 1 -> return null // Ambiguously inherited at this level – don't resolve.
            candidates.size == 1 -> {
                val candidateClassId = candidates.first()
                if (tryResolve(candidateClassId)) return candidateClassId
            }
            // candidates.isEmpty(): fall back to the BFS for Kotlin / binary supertypes.
        }
    }
    return resolveInheritedInnerClassToClassId(
        simpleName, tryResolve, containingClass, includeOuterClasses = false,
    )
}

/**
 * Step 2: Top-level type declared in the **same file** as the resolving reference.
 * Per JLS 6.4.1, such a type shadows any single-type-import of the same simple name.
 *
 * Driven by [JavaScopeContext.sameFileTopLevelClassProvider], which is the only source of truth
 * for "is `simpleName` declared as a top-level class in *this* file?". The bare
 * `ClassId(packageFqName, simpleName)` probe used by [resolveFromSamePackage] cannot distinguish
 * same-file from cross-file because both share the same `ClassId`.
 */
context(c: JavaResolutionContext)
private fun resolveFromSameFile(
    simpleName: String,
    tryResolve: (ClassId) -> Boolean,
): ClassId? {
    c.scopeContext.sameFileTopLevelClassProvider(Name.identifier(simpleName)) ?: return null
    val classId = ClassId(c.packageFqName, Name.identifier(simpleName))
    return if (tryResolve(classId)) classId else null
}

/**
 * Step 3a: Explicit single-type imports (JLS 7.5.1).
 *
 * Per JLS 6.4.1, single-type imports are *shadowed* by both member types of the enclosing class
 * (Step 1) and same-file top-level types (Step 2), so this step runs only after both
 * of those have missed. Same-package types declared in *other* files, on the other
 * hand, are shadowed *by* this step — they appear at Step 4.
 */
context(c: JavaResolutionContext)
private fun resolveFromExplicitImport(
    simpleName: String,
    tryResolve: (ClassId) -> Boolean,
    fullResolution: Boolean,
): ClassId? {
    val imported = c.fileContext.imports.simpleTypeImports[simpleName] ?: return null
    if (fullResolution) {
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
 * `import static a.b.C.X;` brings `X` (a static member of `a.b.C`) into scope. For classifier
 * resolution, only the case where `X` is a *type* matters; the method and field cases drop out
 * cleanly when [tryResolve] returns `false`. The imported FqName always ends with the type's
 * simple name and the prefix is the outer class, so [resolveAsClassId] does the right thing on
 * its own — longest-package-first split, which for `a.b.C.X` will probe `ClassId(a.b, C.X)`
 * (success) before degenerate splits.
 *
 * Per JLS 6.4.1 this is also rank 4 — same as [resolveFromExplicitImport]. A same-simple
 * collision between a type single-import and a static-single-import of a type is malformed Java
 * in practice (`javac` flags the conflict), so the ordering rank-4-type before rank-4-static is
 * a no-op for well-formed code.
 */
context(c: JavaResolutionContext)
private fun resolveFromStaticSingleImport(
    simpleName: String,
    tryResolve: (ClassId) -> Boolean,
): ClassId? {
    val imported = c.fileContext.imports.staticSingleImports[simpleName] ?: return null
    return resolveAsClassId(imported, tryResolve)
}

/**
 * Step 4: Same-package top-level type from *another* file.
 *
 * Per JLS 6.4.1, single-type imports shadow top-level types declared in other files
 * of the same package, so this step runs *after* [resolveFromExplicitImport]. The probe
 * `ClassId(packageFqName, simpleName)` also matches same-file top-level types, but those are
 * already short-circuited by Step 2 ([resolveFromSameFile]), so reaching this step
 * means "not declared in this file".
 */
context(c: JavaResolutionContext)
private fun resolveFromSamePackage(simpleName: String, tryResolve: (ClassId) -> Boolean): ClassId? {
    val classId = ClassId(c.packageFqName, Name.identifier(simpleName))
    return if (tryResolve(classId)) classId else null
}

/** Step 5: `java.lang.*` — implicitly imported by every Java file. */
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
 * The primary probe is `ClassId(pkg, simpleName)`. Since the on-demand target is a
 * `PackageOrTypeName`, it may also be a *class/interface* (`import a.D.*;`, importing the member
 * types of `a.D`), so on a miss we fall back to resolving the entry as a class via
 * [resolveAsClassId] and probing `outerClassId.createNestedClassId(simpleName)`
 * (`testImportThriceNestedClass`, `testNestedAndTopLevelClassClash`). The static variant
 * (`import static a.b.C.*;`, JLS 7.5.4) is handled by [resolveFromStaticStarImports] at rank 7.
 *
 * Returns `null` on ambiguity (two entries resolving the same name to different ClassIds — a
 * JLS 7.5.2 compile-time error), falling through to Step 7.
 */
context(c: JavaResolutionContext)
private fun resolveFromTypeStarImports(
    simpleName: String,
    tryResolve: (ClassId) -> Boolean,
): ClassId? {
    var foundClassId: ClassId? = null
    for (starImport in c.fileContext.imports.typeStarImports) {
        val candidateClassId = ClassId(starImport, Name.identifier(simpleName))
        if (tryResolve(candidateClassId)) {
            if (foundClassId != null && foundClassId != candidateClassId) return null // Ambiguous
            foundClassId = candidateClassId
        } else {
            // Class-level fallback (`import a.D.*` where `a.D` is a class): resolve the
            // entry as a ClassId and form the nested-class shape.
            val outerClassId = resolveAsClassId(starImport, tryResolve)
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
 * Strictly lower JLS shadowing rank than [resolveFromTypeStarImports] (rank 7 vs 6). Each entry
 * is the *outer class* FqName — not a package. The probe shape is:
 *
 *  1. Resolve the outer-class FqName via [resolveAsClassId] (longest-package-first split).
 *  2. Form `outerClassId.createNestedClassId(simpleName)` and probe via [tryResolve].
 *
 * Without [fullResolution] (reentrance-safe fallback path) only direct nested-class resolution
 * is attempted.
 */
context(c: JavaResolutionContext)
private fun resolveFromStaticStarImports(
    simpleName: String,
    tryResolve: (ClassId) -> Boolean,
    fullResolution: Boolean,
): ClassId? {
    var foundClassId: ClassId? = null
    for (outerFqName in c.fileContext.imports.staticStarImports) {
        val outerClassId = resolveAsClassId(outerFqName, tryResolve) ?: continue
        val nestedClassId = outerClassId.createNestedClassId(Name.identifier(simpleName))
        if (tryResolve(nestedClassId)) {
            if (!fullResolution) return nestedClassId
            if (foundClassId != null && foundClassId != nestedClassId) return null // Ambiguous
            foundClassId = nestedClassId
        }
    }
    return foundClassId
}

/**
 * Try to resolve a simple name as an inner class inherited from the supertypes of [containingClass].
 * The BFS reads supertypes through the per-origin [directSupertypeClassIds] dispatcher.
 *
 * When [includeOuterClasses] is `false` only [containingClass]'s own supertypes are searched, so
 * the caller ([resolveFromLocalScope]) can interleave declared and inherited lookups level by level
 * (JLS 6.4.1).
 */
context(c: JavaResolutionContext)
private fun resolveInheritedInnerClassToClassId(
    simpleName: String,
    tryResolve: (ClassId) -> Boolean,
    containingClass: JavaClass?,
    includeOuterClasses: Boolean,
): ClassId? = c.fileContext.inheritedMemberResolver.resolveInheritedInnerClassToClassId(
    simpleName, tryResolve, { directSupertypeClassIds(it) }, containingClass,
    resolveWithoutInheritance = { name, resolve ->
        if (name.contains('.')) {
            resolveQualifiedNameToClassIdFromParts(name.split('.'), resolve, fullResolution = false)
        } else {
            resolveSimpleNameToClassIdImpl(name, resolve, fullResolution = false)
        }
    },
    includeOuterClasses = includeOuterClasses,
)

/**
 * Searches the supertype hierarchy of [outerClassId] for an inherited nested class with
 * [nestedName]. Dispatches via [directSupertypeClassIds] and probes via [tryResolve]; cycles are
 * bounded by [cycleGuardedSupertypeWalk].
 */
context(c: JavaResolutionContext)
private fun findInheritedNestedClass(
    outerClassId: ClassId,
    nestedName: String,
): ClassId? {
    // Read supertypes BEFORE the cycle guard: [directSupertypeClassIds] shares the same
    // per-session supertype-walk guard keyed by `classId` and would bail out if entered re-entrantly.
    val supers = directSupertypeClassIds(outerClassId)
    return c.fileContext.session.cycleGuardedSupertypeWalk(outerClassId, default = null) {
        for (supertypeId in supers) {
            val candidateId = supertypeId.createNestedClassId(Name.identifier(nestedName))
            if (tryResolve(candidateId)) return@cycleGuardedSupertypeWalk candidateId
            // Recurse into supertype's supertypes
            findInheritedNestedClass(supertypeId, nestedName)?.let { return@cycleGuardedSupertypeWalk it }
        }

        // Also check via the class finder for same-package Java source supertypes
        val finder = c.fileContext.classFinder
        if (finder != null) {
            val inheritedInners = finder.collectInheritedInnerClasses(outerClassId)
            val candidates = inheritedInners[nestedName]
            if (candidates != null && candidates.size == 1) {
                val candidateClassId = candidates.first()
                if (tryResolve(candidateClassId)) return@cycleGuardedSupertypeWalk candidateClassId
            }
        }

        null
    }
}

/**
 * Builtins-filtered class-existence probe: `true` if [classId] is known to the session's symbol
 * provider and is not a Kotlin builtin. Returns `false` for sessions with no symbol provider —
 * AST-only resolution paths (type parameters, current scope classes, multi-part navigation) still
 * work without it.
 *
 * The `origin != BuiltIns` filter keeps this probe in agreement with PSI's file-backed class
 * finder: PSI only resolves a Java type reference when an actual `.class`/`.java` file exists,
 * whereas FIR's symbol provider also fetches the `kotlin.*` builtins from the `.kotlin_builtins`
 * metadata bundled into the compiler.
 *
 * Without the filter `testInheritFromAnnotationClass2` fails: its `J.java` does
 * `extends kotlin.annotation.Target`, and resolving that builtin lets FIR's
 * `FirAnnotationClassInheritanceChecker` walk the whole supertype chain and emit extra
 * `EXTENDING_AN_ANNOTATION_CLASS_ERROR`.
 */
context(c: JavaResolutionContext)
internal fun tryResolve(classId: ClassId): Boolean {
    val symbol = c.fileContext.session.cycleSafeClassLikeSymbol(classId) ?: return false
    return symbol.origin != FirDeclarationOrigin.BuiltIns
}

/**
 * Whether [classId] denotes an annotation class whose declared `@Target` lists `TYPE_USE`
 * (Java) or `TYPE` (Kotlin). Used by [org.jetbrains.kotlin.java.direct.model.JavaTypeOverAst]
 * to pre-filter `memberAnnotations`.
 *
 * Cached per session via [JavaModelTypeUseClassIdCache]; the underlying probe goes through
 * [cycleSafeClassLikeSymbol] so KT-74097-class cycles cannot fire here either.
 */
context(c: JavaResolutionContext)
internal fun isTypeUseAnnotationClass(classId: ClassId): Boolean =
    c.fileContext.session.isTypeUseAnnotationClass(classId)

/**
 * Cross-language constant-field resolution used by
 * [org.jetbrains.kotlin.java.direct.model.JavaFieldOverAst]'s `initializerValue` to evaluate
 * qualified references such as `Foo.BAR` where `Foo` is a Kotlin class / facade. Returns `null`
 * when no `const val` is found.
 *
 * `currentPackage` defaults to this context's file package, matching how the Java
 * field's `containingClass` lives in the same file.
 */
context(c: JavaResolutionContext)
internal fun resolveExternalFieldValue(classQualifier: String?, fieldName: String): Any? =
    c.fileContext.session.resolveExternalFieldValue(classQualifier, fieldName, c.packageFqName)

/**
 * Const-vs-enum-entry disambiguation used by
 * [org.jetbrains.kotlin.java.direct.model.createAnnotationArgumentFromValue] for annotation
 * arguments that look syntactically like enum entries but may denote a Kotlin `const val`.
 * Returns `null` when the reference is a real enum entry or unresolvable.
 */
context(c: JavaResolutionContext)
internal fun resolveConstFieldValue(classId: ClassId, fieldName: Name): Any? =
    c.fileContext.session.resolveConstFieldValue(classId, fieldName)

/**
 * Wraps [classId] in a [FirBackedJavaClassAdapter] backed by this context's session, or `null`
 * when the session has no `FirSymbolProvider` (parsing-level unit fixtures): the adapter could
 * not materialise its fields, and FIR-side `findClassIdByFqNameString` handles such references
 * instead.
 */
context(c: JavaResolutionContext)
internal fun classifierAdapterFor(classId: ClassId): JavaClass? {
    val session = c.fileContext.session
    return if (session.nullableSymbolProvider != null) FirBackedJavaClassAdapter(classId, session) else null
}

/**
 * Recovers the JLS-implicit outer-class type arguments for a **bare inherited inner-class
 * reference** whose outer arguments are neither written in source nor lexically in scope —
 * e.g. `J1.NestedSubClass extends NestedInSuperClass` with `J1 → KFirst → SuperClass<String>`
 * yields `[String]`, so the supertype is `SuperClass<String>.NestedInSuperClass`.
 *
 * The lexical containing class is read from [JavaScopeContext.containingClass]. The
 * walk starts from the containing class's **outer** class, whose supertypes are still
 * being resolved — and descends each outer class's [FirBackedJavaClassAdapter.supertypes]
 * looking for [innerClassId]'s outer class.
 *
 * Returns the recovered arguments as FIR-backed [JavaType]s (so FIR's `JavaTypeConversion`
 * reconstructs the cone type), or `null` when nothing is recovered (top-level inner class,
 * no containing class, a `static` nested class along the chain severs the enclosing-instance
 * chain, or the outer class is not found in the hierarchy).
 */
context(c: JavaResolutionContext)
internal fun recoverInheritedOuterTypeArguments(innerClassId: ClassId): List<JavaType>? {
    val outerClassId = innerClassId.outerClassId ?: return null
    val containingClass = c.scopeContext.containingClass ?: return null
    val session = c.fileContext.session
    // Walk the containing class's outer classes (skipping the containing class itself). Outer
    // classes have their supertypes resolved already (FIR resolves outer before inner).
    //
    // [child] is the class whose outer is [currentOuter]; its static-ness gates access to
    // [currentOuter]'s enclosing instance. Per JLS a `static` nested class has no enclosing
    // instance, so it severs the chain of implicit outer type arguments. This mirrors the static
    // break in PSI's `JavaClassifierTypeImpl.getTypeParameters` and IntelliJ's
    // `PsiUtil.typeParametersIterable`.
    var child: JavaClass = containingClass
    var currentOuter: JavaClass? = child.outerClass
    while (currentOuter != null) {
        if (child.isStatic) break
        val currentOuterId = currentOuter.classId
        if (currentOuterId != null) {
            for (supertype in FirBackedJavaClassAdapter(currentOuterId, session).supertypes) {
                val coneSupertype = (supertype as? FirBackedJavaClassifierType)?.coneType ?: continue
                val recovered = findTypeArgsForClassInHierarchy(coneSupertype, outerClassId, session, mutableSetOf())
                if (recovered != null) return recovered.map { firBackedJavaType(it, session) }
            }
        }
        child = currentOuter
        currentOuter = currentOuter.outerClass
    }
    return null
}

/**
 * Recursively searches [type]'s supertype hierarchy (via [FirBackedJavaClassAdapter.supertypes])
 * for [targetClassId], substituting type arguments down each intermediate class so that, e.g.,
 * `A<X> : Super<X>` instantiated as `A<String>` yields `Super<String>`. Returns the matched
 * class's cone type arguments, or `null` if [targetClassId] is not in the hierarchy.
 */
private fun findTypeArgsForClassInHierarchy(
    type: ConeClassLikeType,
    targetClassId: ClassId,
    session: FirSession,
    visited: MutableSet<ClassId>,
): List<ConeTypeProjection>? {
    val typeClassId = type.lookupTag.classId
    if (typeClassId == targetClassId) return type.typeArguments.toList()
    if (!visited.add(typeClassId)) return null

    for (supertype in FirBackedJavaClassAdapter(typeClassId, session).supertypes) {
        val declaredSupertype = (supertype as? FirBackedJavaClassifierType)?.coneType ?: continue
        val substituted = substituteTypeArgs(declaredSupertype, type, session)
        findTypeArgsForClassInHierarchy(substituted, targetClassId, session, visited)?.let {
            return it
        }
    }
    return null
}

/**
 * Substitutes type-parameter references in [declaredSupertype] with the concrete type arguments
 * of [actualType]. E.g. given `A<X> : SuperClass<X>` and actual `A<String>`, rewrites the declared
 * `SuperClass<X>` to `SuperClass<String>`. The declaring class's type parameters are read through
 * [cycleSafeClassLikeSymbol] to stay on the cycle-safe symbol path.
 */
private fun substituteTypeArgs(
    declaredSupertype: ConeClassLikeType,
    actualType: ConeClassLikeType,
    session: FirSession,
): ConeClassLikeType {
    if (actualType.typeArguments.isEmpty()) return declaredSupertype
    val declaringParams =
        (session.cycleSafeClassLikeSymbol(actualType.lookupTag.classId) as? FirRegularClassSymbol)?.typeParameterSymbols
            ?: return declaredSupertype
    if (declaringParams.isEmpty()) return declaredSupertype

    val substitution = buildMap {
        declaringParams.forEachIndexed { index, typeParam ->
            val arg = actualType.typeArguments.getOrNull(index) ?: return@forEachIndexed
            val type = arg as? ConeKotlinType
                ?: ConeErrorType(ConeSimpleDiagnostic("illegal projection usage", DiagnosticKind.IllegalProjectionUsage))
            put(typeParam, type)
        }
    }
    if (substitution.isEmpty()) return declaredSupertype

    return substitutorByMap(substitution, session).substituteOrSelf(declaredSupertype) as? ConeClassLikeType
        ?: declaredSupertype
}

/**
 * Per-origin direct-supertype-`ClassId` dispatcher, guarded by [cycleGuardedSupertypeWalk] so
 * direct (`A extends A`) and indirect (`A → B → A`) Java-side cycles terminate cleanly.
 *
 *  1. **Source Java arm** — `classFinder.findClass(classId)` hits: walk `JavaClass.supertypes`
 *     directly (no FIR phase involved).
 *  2. **Binary Java arm** — FIR symbol is a [FirJavaClass]: read the pre-resolved
 *     [FirJavaClass.directSupertypeClassIds] cache (no enhancement triggered).
 *  3. **Kotlin / built-in / deserialized arm** — `lazyResolveToPhase(SUPER_TYPES)` is honest in
 *     compiler mode (the eager driver finishes these before Java member conversion runs).
 *     Cycles on this arm are bounded by FIR's `SupertypeComputationStatus.Computing` sentinel,
 *     not by the model-side checker.
 */
@OptIn(SymbolInternals::class)
context(c: JavaResolutionContext)
internal fun directSupertypeClassIds(classId: ClassId): List<ClassId> =
    c.fileContext.session.cycleGuardedSupertypeWalk(classId, default = emptyList()) {
        // 1. Source Java arm — walk our own AST. Supertype names are syntactically
        // knowable; no FIR phase is involved.
        val finder = c.fileContext.classFinder
        if (finder != null && finder.isClassInIndex(classId)) {
            val javaClass = finder.findClass(JavaClassFinder.Request(classId))
            if (javaClass != null) {
                return@cycleGuardedSupertypeWalk resolveSupertypeNames(javaClass)
            }
        }

        // 2. & 3. Look up the FIR symbol — the model's only handle for non-source-Java
        // classes (binary Java, Kotlin, deserialized).
        val symbol = c.fileContext.session.cycleSafeClassLikeSymbol(classId) ?: return@cycleGuardedSupertypeWalk emptyList()
        val firClass = symbol.fir as? FirRegularClass ?: return@cycleGuardedSupertypeWalk emptyList()

        // 2. Binary Java arm — read the pre-resolved cache on FirJavaClass; never
        // touches the lazy `superTypeRefs` enhancement.
        if (firClass is FirJavaClass) {
            return@cycleGuardedSupertypeWalk firClass.directSupertypeClassIds()
        }

        // 3. Kotlin / built-in / deserialized arm — lazyResolveToPhase is honest here.
        symbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
        firClass.superTypeRefs.mapNotNull { ref ->
            ((ref as? FirResolvedTypeRef)?.coneType as? ConeClassLikeType)?.lookupTag?.classId
        }
    }

/**
 * Resolves the supertype names of a Java source [enclosing] class to a list of direct-supertype
 * [ClassId]s. Reads the materialised `classifier` field on each [JavaClassifierType] in
 * [JavaClass.supertypes], which is reliable for every reference (cross-file too).
 */
private fun resolveSupertypeNames(enclosing: JavaClass): List<ClassId> =
    enclosing.supertypes.mapNotNull { supertype ->
        (supertype.classifier as? JavaClass)?.classId
    }

/**
 * Transitively inherited inner class names for a single enclosing class [classId], as reported by
 * the Java-source class finder (maps simpleName -> Set<ClassId>). Cached per class on
 * [JavaScopeContext.inheritedInnerCache] so the level-by-level walk in [resolveFromLocalScope] does
 * not re-collect the same class on every simple-name resolution. Returns `null` when no class
 * finder is available (the caller then falls back to the supertype BFS).
 */
context(c: JavaResolutionContext)
private fun getInheritedInnerClassesForClass(classId: ClassId): Map<String, Set<ClassId>>? {
    val finder = c.fileContext.classFinder ?: return null
    val cache = c.scopeContext.inheritedInnerCache
    return cache.byClass.getOrPut(classId) {
        finder.collectInheritedInnerClasses(classId)
    }
}

/**
 * Unified single-import lookup ([JavaImports.getSingleImport]): tries the single-type-import
 * bucket first, then the single-static-import bucket. Used by model-side consumers that need a
 * yes/no answer to "is there *any* single-import of this simple name?".
 *
 * The dispatcher inside [resolveSimpleNameToClassIdImpl] does not call this — it probes
 * [JavaImports.simpleTypeImports] and [JavaImports.staticSingleImports] separately so it can keep
 * the JLS rank-4 ordering between them explicit.
 */
context(c: JavaResolutionContext)
internal fun getSimpleImport(simpleName: String): FqName? = c.fileContext.imports.getSingleImport(simpleName)

/**
 * Static-only single-import lookup: returns the FqName of an `import static a.b.C.X;` declaration
 * if and only if [simpleName] was imported in that shape. Used by
 * [org.jetbrains.kotlin.java.direct.model.JavaEnumValueAnnotationArgumentOverAst] to recover the
 * implicit `Outer.member` enum-entry binding from a bare identifier.
 */
context(c: JavaResolutionContext)
internal fun getStaticImport(simpleName: String): FqName? = c.fileContext.imports.staticSingleImports[simpleName]

/**
 * Returns the parsed imports (four-bucket [JavaImports]) from this context. Used by
 * [JavaClassFinderOverAstImpl.getDirectSupertypes] on the fast path to avoid re-extracting
 * imports from the AST root.
 */
context(c: JavaResolutionContext)
internal fun getImports(): JavaImports = c.fileContext.imports

/**
 * Returns the first star import package that could contain a class with the given simple name.
 * Used for best-effort classId resolution when we can't call the symbol provider.
 */
@TestOnly
context(c: JavaResolutionContext)
internal fun getFirstStarImportCandidate(simpleName: String): ClassId? {
    // Only type-import-on-demand makes sense for the `ClassId(pkg, simpleName)` shape:
    // static-import-on-demand (`import static a.b.C.*;`) holds an outer-class FqName, not
    // a package, and would need the nested-class shape — which this best-effort accessor
    // is not the place for. Callers that need static-star fallback go through the full
    // [resolve] dispatcher.
    val starPackage = c.fileContext.imports.typeStarImports.firstOrNull() ?: return null
    return ClassId(starPackage, Name.identifier(simpleName))
}

context(c: JavaResolutionContext)
private fun fqNameToClassId(fqName: FqName): ClassId =
    fqNameInPackageToClassId(fqName, c.packageFqName)

/**
 * Resolves a FqName to a ClassId by trying all possible package/class splits, using the
 * tryResolve callback to validate each candidate.
 *
 * Unlike ClassId.topLevel which only tries the trivial split at the last dot, this tries all
 * splits from longest package to shortest, so "a.x.b.b.b" will try ClassId(a.x.b.b, b),
 * ClassId(a.x.b, b.b), ClassId(a.x, b.b.b), ClassId(a, x.b.b.b).
 *
 * Used for explicit imports with nested class FQNs and for class-level star import resolution.
 */
private fun resolveAsClassId(fqName: FqName, tryResolve: (ClassId) -> Boolean): ClassId? {
    if (fqName.isRoot) return null

    // most common case: the longest-package split
    ClassId.topLevel(fqName).takeIf(tryResolve)?.let { return it }

    val parts = fqName.pathSegments()
    val stringParts = parts.map { it.asString() }
    for (classStartIndex in (parts.size - 2) downTo 0) {
        val pkg = when (classStartIndex) {
            0 -> FqName.ROOT
            else -> FqName.fromSegments(stringParts.subList(0, classStartIndex))
        }
        val cls = FqName.fromSegments(stringParts.subList(classStartIndex, stringParts.size))
        val classId = ClassId(pkg, cls, false)

        if (tryResolve(classId)) return classId
    }
    return null
}

/**
 * Probe every package/class split of [parts] from longest package prefix down to the root
 * package, returning the first [ClassId] accepted by [tryResolve].
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
