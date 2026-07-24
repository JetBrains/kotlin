/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.annotations.TestOnly
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
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
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
 * Stateless type-reference resolution for Java source files: the JLS 6.4.1 simple-name and
 * JLS 6.5.5/6.5.4 qualified-name dispatchers, supertype-`ClassId` walking, and the session-backed
 * probes, all operating on the given [JavaResolutionContext].
 */

/**
 * Resolves a type name to a [ClassId], which unambiguously encodes the package/class boundary
 * (`"a.b"` could mean either `ClassId("a", "b")` or `ClassId("", "a.b")` — a string cannot).
 */
context(c: JavaResolutionContext)
internal fun resolve(name: String): ClassId? {
    if (name.contains('.')) {
        return resolveQualifiedNameToClassIdFromParts(name.split('.'), fullResolution = true)
    }
    return resolveSimpleNameToClassIdImpl(name, fullResolution = true)
}

/**
 * Qualified type name resolution (JLS 6.5.5): a single left-to-right pass that classifies a
 * PackageOrTypeName qualifier according to JLS 6.5.4, then descends into member types:
 *
 *  - the first segment is a type iff it resolves as a simple type name in scope (JLS 6.5.4.1) —
 *    e.g. for `Map.Entry`, `Map` resolves via the `java.util.Map` import;
 *  - otherwise the package prefix grows one segment at a time until a segment names a top-level
 *    type in that package (JLS 6.5.4.2) — e.g. for an inline `java.util.List`, `java` and
 *    `java.util` are packages and `List` is the type;
 *  - each segment after the leftmost type must be a member type of the previous one (JLS
 *    6.5.5.2): declared, or — with [fullResolution] — inherited from its supertypes.
 */
context(c: JavaResolutionContext)
internal fun resolveQualifiedNameToClassIdFromParts(
    parts: List<String>,
    fullResolution: Boolean,
): ClassId? {
    require(parts.size > 1)

    // Leftmost type (JLS 6.5.4): the first segment as a simple type name in scope, else the
    // shortest package prefix whose next segment names a top-level type.
    var leftmostClassId: ClassId? = resolveSimpleNameToClassIdImpl(parts[0], fullResolution)
    var next = 1
    while (leftmostClassId == null && next < parts.size) {
        val candidate = ClassId(FqName.fromSegments(parts.subList(0, next)), Name.identifier(parts[next]))
        if (classExists(candidate, fullResolution)) leftmostClassId = candidate
        next++
    }

    var outerClassId = leftmostClassId ?: return null // no segment names a type in scope

    // Member-type descent (JLS 6.5.5.2). The inherited lookup covers names like
    // `SimpleFunctionDescriptor.CopyBuilder`, where `CopyBuilder` comes from the
    // `FunctionDescriptor` superinterface of the resolved prefix.
    while (next < parts.size) {
        val declared = outerClassId.createNestedClassId(Name.identifier(parts[next]))
        outerClassId = when {
            classExists(declared, fullResolution) -> declared
            fullResolution -> findInheritedNestedClass(outerClassId, parts[next]) ?: return null // TODO: (KT-87823) maybe we should return a dangling (unresolvable) classId here
            else -> return null
        }
        next++
    }
    return outerClassId
}

/**
 * Unified workhorse for simple-name resolution.
 *
 * [fullResolution] selects the flavor. `true` is the full primary path. `false` is the
 * reentrance-safe fallback used while an inherited-inner-class walk
 * ([resolveInheritedInnerClassToClassId]) is already in progress: it skips Step 1 (the local-scope
 * member-type lookup that would recurse back into the same walk) and probes class existence via
 * the source-index-aware [tryResolveInherited] rather than [tryResolve]. Every import step still
 * uses the same full package/class split in both flavors.
 */
context(c: JavaResolutionContext)
internal fun resolveSimpleNameToClassIdImpl(
    simpleName: String,
    fullResolution: Boolean,
): ClassId? {
    // JLS 6.4.1: member types of the enclosing class shadow single-type imports.
    if (fullResolution) {
        resolveFromEnclosingClasses(simpleName)?.let { return it }
    }
    // JLS 6.4.1: same-file top-level types shadow single-type imports.
    resolveFromSameFile(simpleName, fullResolution)?.let { return it }
    // JLS 7.5.1: single-type imports.
    resolveFromExplicitImport(simpleName, fullResolution)?.let { return it }
    // JLS 7.5.3: single-static imports (rank 4, same as 7.5.1; tried after).
    resolveFromStaticSingleImport(simpleName, fullResolution)?.let { return it }
    // JLS 6.4.1: same-package top-level types from *other* files are
    // shadowed by the import (Step 3), so this step runs after it.
    resolveFromSamePackage(simpleName, fullResolution)?.let { return it }
    // JLS 7.3: java.lang.* is implicitly imported.
    resolveFromJavaLang(simpleName, fullResolution)?.let { return it }
    // JLS 7.5.2: type-import-on-demand.
    resolveFromTypeStarImports(simpleName, fullResolution)?.let { return it }
    // JLS 7.5.4: static-import-on-demand (strictly lower rank than 7.5.2).
    return resolveFromStaticStarImports(simpleName, fullResolution)
}

/**
 * Step 1: member types of the enclosing classes, declared and inherited (JLS 6.4.1 / 6.5.2);
 * these shadow single-type imports, so this step runs before [resolveFromExplicitImport].
 *
 * Walks the containing-class chain from innermost to outermost, at each level probing the
 * declared member type and then the inherited ones — the lookups must interleave level by level
 * because a member type at an inner level shadows one at an enclosing level (JLS 6.4.1).
 *
 * Same-file top-level classes are NOT resolved here: they share their `ClassId` with
 * same-package cross-file classes, so [resolveFromSamePackage] picks them up.
 */
context(c: JavaResolutionContext)
private fun resolveFromEnclosingClasses(simpleName: String): ClassId? {
    val nameId = Name.identifier(simpleName)
    var current: JavaClass? = c.scopeContext.containingClass
    while (current != null) {
        val fqName = current.fqName
        if (fqName != null) {
            val containingId = fqNameToClassId(fqName)
            val declared = containingId.createNestedClassId(nameId)
            if (tryResolve(declared)) return declared
            // Inherited member types, restricted to this level so the interleaving holds.
            resolveInheritedInnerClassToClassId(simpleName, current)?.let { return it }
        }
        current = current.outerClass
    }
    return null
}

/**
 * Step 2: top-level type declared in the same file (shadows single-type imports, JLS 6.4.1).
 * [JavaScopeContext.sameFileTopLevelClassProvider] is required because the bare
 * `ClassId(packageFqName, simpleName)` probe cannot distinguish same-file from cross-file.
 */
context(c: JavaResolutionContext)
private fun resolveFromSameFile(
    simpleName: String,
    fullResolution: Boolean,
): ClassId? {
    c.scopeContext.sameFileTopLevelClassProvider(Name.identifier(simpleName)) ?: return null
    val classId = ClassId(c.packageFqName, Name.identifier(simpleName))
    return if (classExists(classId, fullResolution)) classId else null
}

/**
 * Step 3a: explicit single-type imports (JLS 7.5.1). Shadowed by Steps 1–2; shadows
 * same-package types from other files (Step 4).
 */
context(c: JavaResolutionContext)
private fun resolveFromExplicitImport(
    simpleName: String,
    fullResolution: Boolean,
): ClassId? {
    val imported = c.fileContext.imports.simpleTypeImports[simpleName] ?: return null
    // resolveAsClassId handles nested-class import FQNs like `a.x.b.b.b`, where ClassId.topLevel
    // would mis-split as package `a.x.b.b`, class `b`. This holds on the reentrance-safe path too:
    // resolveAsClassId only probes class existence, never re-entering inherited-inner-class
    // resolution, so the full split is safe in both flavors (matching the static/star import steps).
    return resolveAsClassId(imported, fullResolution)
}

/**
 * Step 3b: single-static imports (JLS 7.5.3) — the type-only arm; method and field imports drop
 * out when the class-existence probe returns `false`. Same JLS rank (4) as [resolveFromExplicitImport]; a
 * same-name collision between the two is malformed Java (`javac` flags it), so trying the type
 * import first is a no-op for well-formed code.
 */
context(c: JavaResolutionContext)
private fun resolveFromStaticSingleImport(
    simpleName: String,
    fullResolution: Boolean,
): ClassId? {
    val imported = c.fileContext.imports.staticSingleImports[simpleName] ?: return null
    return resolveAsClassId(imported, fullResolution)
}

/**
 * Step 4: same-package top-level type from another file (shadowed by single-type imports,
 * JLS 6.4.1). The probe also matches same-file types, but Step 2 already short-circuited those.
 */
context(c: JavaResolutionContext)
private fun resolveFromSamePackage(simpleName: String, fullResolution: Boolean): ClassId? {
    val classId = ClassId(c.packageFqName, Name.identifier(simpleName))
    return if (classExists(classId, fullResolution)) classId else null
}

/** Step 5: `java.lang.*` — implicitly imported by every Java file. */
context(c: JavaResolutionContext)
private fun resolveFromJavaLang(simpleName: String, fullResolution: Boolean): ClassId? {
    val classId = ClassId(FqName("java.lang"), Name.identifier(simpleName))
    return if (classExists(classId, fullResolution)) classId else null
}

/**
 * Step 6: type-import-on-demand (`import a.b.*;`, JLS 7.5.2). The on-demand target is a
 * `PackageOrTypeName`, so on a package-probe miss the entry is retried as a *class* whose member
 * types are imported (`import a.D.*;`). Returns `null` on ambiguity (a JLS 7.5.2 compile-time
 * error), falling through to Step 7.
 */
context(c: JavaResolutionContext)
private fun resolveFromTypeStarImports(
    simpleName: String,
    fullResolution: Boolean,
): ClassId? {
    var foundClassId: ClassId? = null
    for (starImport in c.fileContext.imports.typeStarImports) {
        val candidateClassId = ClassId(starImport, Name.identifier(simpleName))
        if (classExists(candidateClassId, fullResolution)) {
            if (foundClassId != null && foundClassId != candidateClassId) return null // Ambiguous
            foundClassId = candidateClassId
        } else {
            // Class-level fallback: `import a.D.*` where `a.D` is a class.
            val outerClassId = resolveAsClassId(starImport, fullResolution)
            if (outerClassId != null) {
                val nestedClassId = outerClassId.createNestedClassId(Name.identifier(simpleName))
                if (classExists(nestedClassId, fullResolution)) {
                    if (foundClassId != null && foundClassId != nestedClassId) return null // Ambiguous
                    foundClassId = nestedClassId
                }
            }
        }
    }
    return foundClassId
}

/**
 * Step 7: static-import-on-demand (`import static a.b.C.*;`, JLS 7.5.4) — lower rank than
 * Step 6. Each entry is an outer *class* FqName, not a package: resolve it via [resolveAsClassId]
 * and probe its nested class. Without [fullResolution] (reentrance-safe fallback) the first
 * match is returned without the ambiguity check.
 */
context(c: JavaResolutionContext)
private fun resolveFromStaticStarImports(
    simpleName: String,
    fullResolution: Boolean,
): ClassId? {
    var foundClassId: ClassId? = null
    for (outerFqName in c.fileContext.imports.staticStarImports) {
        val outerClassId = resolveAsClassId(outerFqName, fullResolution) ?: continue
        val nestedClassId = outerClassId.createNestedClassId(Name.identifier(simpleName))
        if (classExists(nestedClassId, fullResolution)) {
            if (!fullResolution) return nestedClassId
            if (foundClassId != null && foundClassId != nestedClassId) return null // Ambiguous
            foundClassId = nestedClassId
        }
    }
    return foundClassId
}

/**
 * Searches the supertype hierarchy of [outerClassId] for an inherited nested class named
 * [nestedName]. Delegates to [resolveInheritedInnerClassToClassId], which stays safe when
 * [outerClassId] is itself mid-resolution (e.g. `Outer.Nested` referenced inside `Outer`'s own
 * extends/implements clause) — see its KDoc.
 */
context(c: JavaResolutionContext)
private fun findInheritedNestedClass(
    outerClassId: ClassId,
    nestedName: String,
): ClassId? = resolveInheritedInnerClassToClassId(nestedName, classifierAdapterFor(outerClassId))

/**
 * Builtins- and typealias-filtered class-existence probe: `true` if [classId] is known to the
 * session's symbol provider and denotes a real class (not a Kotlin builtin, not a `typealias`).
 * Returns `false` for sessions with no symbol provider.
 *
 * Both filters exist to match PSI's file-backed class finder, which only resolves a Java type
 * reference when an actual `.class`/`.java` file exists:
 *  - `origin != BuiltIns` drops the `kotlin.*` builtins that FIR's symbol provider bundles into
 *    the compiler but PSI never sees;
 *  - `!is FirTypeAliasSymbol` drops Kotlin type aliases, which are a Kotlin-only concept with no
 *    corresponding `.class` file, hence invisible to Java. FIR's symbol provider returns them
 *    from [cycleSafeClassLikeSymbol] (a `FirClassLikeSymbol`), so without this filter a bare
 *    Java reference could wrongly resolve to a `typealias`.
 */
context(c: JavaResolutionContext)
private fun tryResolve(classId: ClassId): Boolean {
    val symbol = c.fileContext.session.cycleSafeClassLikeSymbol(classId) ?: return false
    if (symbol is FirTypeAliasSymbol) return false
    return symbol.origin != FirDeclarationOrigin.BuiltIns
}

/**
 * [tryResolve] extended to probe [classId] against the source index first, so a cross-file Java
 * source ancestor is checked for existence from its AST alone and can be compared for ambiguity
 * in the same walk as a binary Java or Kotlin ancestor.
 */
context(c: JavaResolutionContext)
internal fun tryResolveInherited(classId: ClassId): Boolean {
    val finder = c.fileContext.classFinder
    if (finder != null && finder.isClassInIndex(classId)) {
        return finder.findClass(JavaClassFinder.Request(classId)) != null
    }
    return tryResolve(classId)
}

/**
 * Selects the class-existence probe for the current resolution flavor: [tryResolve] for the
 * primary path, [tryResolveInherited] for the reentrance-safe inherited-inner-class walk (the
 * latter also consults the source index). [fullResolution] is `true` exactly on the primary
 * path, so it fully determines which probe applies.
 */
context(c: JavaResolutionContext)
private fun classExists(classId: ClassId, fullResolution: Boolean): Boolean =
    if (fullResolution) tryResolve(classId) else tryResolveInherited(classId)

/**
 * Whether [classId] denotes an annotation class whose declared `@Target` lists `TYPE_USE`
 * (Java) or `TYPE` (Kotlin). Cached per session; the underlying probe goes through
 * [cycleSafeClassLikeSymbol] so KT-74097-class cycles cannot fire here either.
 */
context(c: JavaResolutionContext)
internal fun isTypeUseAnnotationClass(classId: ClassId): Boolean =
    c.fileContext.session.isTypeUseAnnotationClass(classId)

/**
 * Cross-language constant-field resolution for qualified references such as `Foo.BAR` where
 * `Foo` is a Kotlin class / facade. Returns `null` when no `const val` is found.
 */
context(c: JavaResolutionContext)
internal fun resolveExternalFieldValue(classQualifier: String?, fieldName: String): Any? =
    c.fileContext.session.resolveExternalFieldValue(classQualifier, fieldName, c.packageFqName)

/**
 * Const-vs-enum-entry disambiguation for annotation arguments that look syntactically like enum
 * entries but may denote a Kotlin `const val`. Returns `null` when the reference is a real enum
 * entry or unresolvable.
 */
context(c: JavaResolutionContext)
internal fun resolveConstFieldValue(classId: ClassId, fieldName: Name): Any? =
    c.fileContext.session.resolveConstFieldValue(classId, fieldName)

/**
 * Materialises [classId] into a navigable [JavaClass]:
 *  - source Java: the finder's canonical instance — required, not just preferred, because FIR
 *    matches `FirJavaTypeParameter` to `JavaTypeParameter` by reference identity;
 *  - binary Java / Kotlin: a [FirBackedJavaClassAdapter], or `null` when the session has no
 *    `FirSymbolProvider`.
 */
context(c: JavaResolutionContext)
internal fun classifierAdapterFor(classId: ClassId): JavaClass? {
    val finder = c.fileContext.classFinder
    if (finder != null && finder.isClassInIndex(classId)) {
        finder.findClass(JavaClassFinder.Request(classId))?.let { return it }
    }
    val session = c.fileContext.session
    return if (session.nullableSymbolProvider != null) FirBackedJavaClassAdapter(classId, session) else null
}

/**
 * Recovers the JLS-implicit outer-class type arguments for a bare inherited inner-class
 * reference whose outer arguments are neither written in source nor lexically in scope —
 * e.g. `J1.NestedSubClass extends NestedInSuperClass` with `J1 → KFirst → SuperClass<String>`
 * yields `[String]`, so the supertype is `SuperClass<String>.NestedInSuperClass`.
 *
 * Returns the recovered arguments as FIR-backed [JavaType]s, or `null` when nothing is
 * recovered.
 */
context(c: JavaResolutionContext)
internal fun recoverInheritedOuterTypeArguments(innerClassId: ClassId): List<JavaType>? {
    val outerClassId = innerClassId.outerClassId ?: return null
    val containingClass = c.scopeContext.containingClass ?: return null
    val session = c.fileContext.session
    // Walk the containing class's outer classes, whose supertypes are resolved already (FIR
    // resolves outer before inner). Per JLS a `static` nested class has no enclosing instance and
    // severs the chain of implicit outer type arguments — mirrors the static break in PSI's
    // `JavaClassifierTypeImpl.getTypeParameters` and IntelliJ's `PsiUtil.typeParametersIterable`.
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
 * Java-side supertype cycles terminate cleanly, and memoized per session. The memoization is
 * essential, not an optimization: the source arm re-enters resolution via `.classifier`, so
 * transitive walks would otherwise re-resolve ancestors exponentially.
 *
 *  1. **Source Java** — walk `JavaClass.supertypes` from the AST (no FIR phase involved).
 *  2. **Binary Java** — read the pre-resolved [FirJavaClass.directSupertypeClassIds] cache
 *     (never triggers the lazy enhancement).
 *  3. **Kotlin / built-in / deserialized** — `lazyResolveToPhase(SUPER_TYPES)`; cycles here are
 *     bounded by FIR's own `SupertypeComputationStatus.Computing` sentinel.
 */
@OptIn(SymbolInternals::class)
context(c: JavaResolutionContext)
internal fun directSupertypeClassIds(classId: ClassId): List<ClassId> =
    c.fileContext.session.memoizedDirectSupertypeClassIds(classId) {
        c.fileContext.session.cycleGuardedSupertypeWalk(classId, default = emptyList()) {
            // 1. Source Java arm.
            val finder = c.fileContext.classFinder
            if (finder != null && finder.isClassInIndex(classId)) {
                val javaClass = finder.findClass(JavaClassFinder.Request(classId))
                if (javaClass != null) {
                    return@cycleGuardedSupertypeWalk resolveSupertypeNames(javaClass)
                }
            }

            val symbol = c.fileContext.session.cycleSafeClassLikeSymbol(classId) ?: return@cycleGuardedSupertypeWalk emptyList()
            val firClass = symbol.fir as? FirRegularClass ?: return@cycleGuardedSupertypeWalk emptyList()

            // 2. Binary Java arm.
            if (firClass is FirJavaClass) {
                return@cycleGuardedSupertypeWalk firClass.directSupertypeClassIds()
            }

            // 3. Kotlin / built-in / deserialized arm.
            symbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)
            firClass.superTypeRefs.mapNotNull { ref ->
                ((ref as? FirResolvedTypeRef)?.coneType as? ConeClassLikeType)?.lookupTag?.classId
            }
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
 * Unified single-import lookup: the single-type-import bucket first, then the single-static one.
 * The dispatcher in [resolveSimpleNameToClassIdImpl] probes the buckets separately instead, to
 * keep the JLS rank-4 ordering between them explicit.
 */
context(c: JavaResolutionContext)
internal fun getSimpleImport(simpleName: String): FqName? = c.fileContext.imports.getSingleImport(simpleName)

/**
 * Static-only single-import lookup: the FqName of an `import static a.b.C.X;` declaration, used
 * to recover the implicit `Outer.member` enum-entry binding from a bare identifier.
 */
context(c: JavaResolutionContext)
internal fun getStaticImport(simpleName: String): FqName? = c.fileContext.imports.staticSingleImports[simpleName]

/** Parsed imports of this context's file, exposed to avoid re-extracting them from the AST. */
context(c: JavaResolutionContext)
internal fun getImports(): JavaImports = c.fileContext.imports

/**
 * Returns the first star import package that could contain a class with the given simple name.
 * Used for best-effort classId resolution when we can't call the symbol provider.
 */
@TestOnly
context(c: JavaResolutionContext)
internal fun getFirstStarImportCandidate(simpleName: String): ClassId? {
    // Only type-import-on-demand fits the `ClassId(pkg, simpleName)` shape; static-star entries
    // hold an outer-class FqName and would need the nested-class shape.
    val starPackage = c.fileContext.imports.typeStarImports.firstOrNull() ?: return null
    return ClassId(starPackage, Name.identifier(simpleName))
}

context(c: JavaResolutionContext)
private fun fqNameToClassId(fqName: FqName): ClassId {
    val fqnString = fqName.asString()
    val pkgString = c.packageFqName.asString()
    val className = if (pkgString.isEmpty()) {
        fqnString
    } else if (fqnString.startsWith(pkgString) && fqnString.length > pkgString.length && fqnString[pkgString.length] == '.') {
        fqnString.substring(pkgString.length + 1)
    } else {
        fqnString
    }
    return ClassId(c.packageFqName, FqName(className), isLocal = false)
}


/**
 * Resolves a FqName to a ClassId by trying all package/class splits from longest package to
 * shortest — "a.x.b.b.b" tries ClassId(a.x.b.b, b), ClassId(a.x.b, b.b), … — unlike
 * ClassId.topLevel, which only splits at the last dot.
 */
context(c: JavaResolutionContext)
private fun resolveAsClassId(fqName: FqName, fullResolution: Boolean): ClassId? {
    if (fqName.isRoot) return null

    // most common case: the longest-package split
    ClassId.topLevel(fqName).takeIf { classExists(it, fullResolution) }?.let { return it }

    val parts = fqName.pathSegments()
    val stringParts = parts.map { it.asString() }
    for (classStartIndex in (parts.size - 2) downTo 0) {
        val pkg = when (classStartIndex) {
            0 -> FqName.ROOT
            else -> FqName.fromSegments(stringParts.subList(0, classStartIndex))
        }
        val cls = FqName.fromSegments(stringParts.subList(classStartIndex, stringParts.size))
        val classId = ClassId(pkg, cls, false)

        if (classExists(classId, fullResolution)) return classId
    }
    return null
}
