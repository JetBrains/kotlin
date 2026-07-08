/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirSupertypeResolverVisitor
import org.jetbrains.kotlin.fir.resolve.transformers.SupertypeComputationSession
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.java.direct.model.FirBackedJavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaConstructor
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.load.java.structure.JavaRecordComponent
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Minimal [JavaClass] adapter exposing a [ClassId] resolved by the model's own resolver. Used to
 * populate `JavaClassifierType.classifier` for cross-file references so `JavaTypeConversion`'s
 * `(javaType.classifier as? JavaClass)?.classId` path resolves without a side-channel.
 *
 * The adapter also exposes a real [outerClass] chain whose [typeParameters] are
 * [FirBackedJavaTypeParameter] wrappers carrying the corresponding `FirTypeParameterSymbol`.
 * They are used by `JavaClassifierTypeOverAst`'s qualified-form raw-detection walk
 * (`computeIsRaw`) to inspect outer-class type-parameter counts across files; FIR's own
 * `is JavaTypeParameter ->` branch in `JavaTypeConversion` resolves them through the
 * regular per-`FirJavaClass` `MutableJavaTypeParameterStack` lookup.
 */
internal class FirBackedJavaClassAdapter(
    private val resolvedClassId: ClassId,
    private val session: FirSession,
) : JavaClass {

    override val name: Name = resolvedClassId.shortClassName

    override val fqName: FqName = resolvedClassId.asSingleFqName()

    override val outerClass: JavaClass? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        resolvedClassId.outerClassId?.let { FirBackedJavaClassAdapter(it, session) }
    }

    override val isFromSource: Boolean
        get() = false

    /**
     * Lazily resolved FIR symbol for [resolvedClassId]. Reads
     * [FirJavaClass.nonEnhancedTypeParameters] for Java sources to dodge the
     * `FirSignatureEnhancement` cycle through `JavaTypeConversion.isRaw` that the enhanced
     * `typeParameters` path triggers.
     *
     * Returns `null` when no symbol is registered for [resolvedClassId] (cross-file reference to
     * a class the symbol provider does not know — the adapter still answers `classId` correctly).
     */
    @OptIn(SymbolInternals::class)
    private val firRegularClass: FirRegularClass? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        (session.cycleSafeClassLikeSymbol(resolvedClassId) as? FirRegularClassSymbol)?.fir
    }

    /**
     * Inner-class detection.
     *
     * `FirJavaClass.nonEnhancedTypeParameters` includes [FirOuterClassTypeParameterRef] entries
     * for each outer type parameter when the class is non-static inner (added in
     * [org.jetbrains.kotlin.fir.java.FirJavaFacade.convertJavaClassToFir] when
     * `!javaClass.isStatic && parentClassSymbol != null`). Their presence is the structural
     * indicator we use to decide [isStatic] without reading `firClass.status` (which is lazy and
     * runs status-transformer extensions).
     *
     * For non-Java FIR classes (Kotlin / built-in / deserialized) the same encoding does not
     * apply; fall back to `status.isInner` (Kotlin's `inner` keyword). Top-level classes return
     * `true` (no outer type-param capture).
     */
    override val isStatic: Boolean
        get() {
            val fir = firRegularClass ?: return true
            if (fir is FirJavaClass) {
                return fir.nonEnhancedTypeParameters.none { it is FirOuterClassTypeParameterRef }
            }
            return !fir.status.isInner
        }

    /**
     * Own type parameters as [FirBackedJavaTypeParameter] wrappers carrying their
     * `FirTypeParameterSymbol`s. Outer-class type params ([FirOuterClassTypeParameterRef] entries
     * on `FirJavaClass.nonEnhancedTypeParameters`) are filtered out — [outerClass] is walked
     * recursively by `JavaClassifierTypeOverAst.computeTypeArguments` to add them instead.
     *
     * Reading `nonEnhancedTypeParameters` (rather than `typeParameters`) avoids the
     * `FirSignatureEnhancement` cycle through `JavaTypeConversion.isRaw`. Cached (`lazy`) because
     * FIR matches Java type parameters by object identity, and the qualified-form-raw walk in
     * `JavaTypeOverAst.computeIsRaw` reads outer type parameters across files.
     */
    override val typeParameters: List<JavaTypeParameter> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val fir = firRegularClass ?: return@lazy emptyList()
        val refs: List<FirTypeParameterRef> =
            if (fir is FirJavaClass) fir.nonEnhancedTypeParameters else fir.typeParameters
        refs
            .filter { it !is FirOuterClassTypeParameterRef }
            .map { ref -> FirBackedJavaTypeParameter(ref.symbol) }
    }

    // ---- Safe defaults below this line ---------------------------------------------------------

    override val annotations: Collection<JavaAnnotation>
        get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean
        get() = false

    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null

    override val isAbstract: Boolean
        get() = false
    override val isFinal: Boolean
        get() = false
    override val visibility: Visibility
        get() = Visibilities.Public

    /**
     * Real, fully-shaped resolved supertype chain — the `java-direct` analog of the PSI light
     * class's `EXTENDS_LIST` (see [org.jetbrains.kotlin.fir.java.FirJavaElementFinder]). Mirrors
     * its `resolveSupertypesOnAir` strategy: prefer already-resolved `superTypeRefs`; otherwise
     * resolve on-air using a fresh, throwaway `SupertypeComputationSession` / `ScopeSession` and
     * `FirSupertypeResolverVisitor`, against the adapter's existing shared [session].
     *
     * Each resolved [ConeClassLikeType] is exposed as a [FirBackedJavaClassifierType] so the
     * model-side inherited-outer-argument recovery in `JavaClassifierTypeOverAst.computeTypeArguments`
     * (and FIR's own re-conversion) can read the cone arguments back.
     *
     * Cycle safety: the walk is wrapped in [cycleGuardedSupertypeWalk] keyed by [resolvedClassId]
     * and symbol resolution funnels through [cycleSafeClassLikeSymbol] (via [firRegularClass]);
     * sessions without a symbol provider (parsing-level fixtures) yield `emptyList()`.
     */
    override val supertypes: Collection<JavaClassifierType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val fir = firRegularClass ?: return@lazy emptyList()
        session.cycleGuardedSupertypeWalk(resolvedClassId, default = emptyList()) {
            val refs = if (fir.superTypeRefs.all { it is FirResolvedTypeRef }) {
                fir.superTypeRefs
            } else {
                fir.resolveSupertypesOnAir(session)
            }
            refs.mapNotNull { (it as? FirResolvedTypeRef)?.coneType as? ConeClassLikeType }
                .map { FirBackedJavaClassifierType(it, session) }
        }
    }

    /**
     * Enumeration of directly declared nested-classifier simple names, sourced from the same
     * enhancement-free primitives as [findInnerClass]:
     *  - For a [FirJavaClass] target, [FirJavaClass.existingNestedClassifierNames] — populated
     *    structurally at [org.jetbrains.kotlin.fir.java.FirJavaFacade] conversion time from
     *    `JavaClass.innerClassNames`, never touching `declarations`/enhancement.
     *  - For any other [FirRegularClass] (Kotlin source / deserialized), [nestedClassifierNames]
     *    reads `declarations` directly — see its KDoc for why that's safe here.
     *
     * Terminal-lookup only: results returned by [findInnerClass] are themselves fresh
     * [FirBackedJavaClassAdapter] instances (see its KDoc for the identity caveat).
     */
    override val innerClassNames: Collection<Name>
        get() {
            val fir = firRegularClass ?: return emptyList()
            return if (fir is FirJavaClass) {
                fir.existingNestedClassifierNames
            } else {
                nestedClassifierNames(fir)
            }
        }

    /**
     * Resolves a directly declared nested class of this class by [name], without forcing FIR
     * enhancement or [FirRegularClass.declarations] on a [FirJavaClass] target.
     *
     * The candidate `ClassId` is probed for existence via [cycleSafeClassLikeSymbol] — the same
     * KT-74097-safe existence probe [tryResolve] wraps elsewhere in this module, minus the
     * builtins filter (irrelevant here: a nested class of a real class is never a Kotlin builtin).
     *
     * A found result is wrapped in another [FirBackedJavaClassAdapter] — chaining further through
     * it loses same-file object identity if navigation ever loops back into source, but none of
     * this adapter's own nested classes can be same-file source (a Kotlin/binary class's body
     * cannot declare a class from a different, source-only compilation unit), so this is safe.
     */
    override fun findInnerClass(name: Name): JavaClass? {
        firRegularClass ?: return null
        val candidateId = resolvedClassId.createNestedClassId(name)
        return if (session.cycleSafeClassLikeSymbol(candidateId) != null) {
            FirBackedJavaClassAdapter(candidateId, session)
        } else {
            null
        }
    }

    override val isInterface: Boolean
        get() = false
    override val isAnnotationType: Boolean
        get() = false
    override val isEnum: Boolean
        get() = false
    override val isRecord: Boolean
        get() = false
    override val isSealed: Boolean
        get() = false
    override val permittedTypes: Sequence<JavaClassifierType>
        get() = emptySequence()
    override val lightClassOriginKind: LightClassOriginKind?
        get() = null

    override val methods: Collection<JavaMethod>
        get() = emptyList()
    override val fields: Collection<JavaField>
        get() = emptyList()
    override val constructors: Collection<JavaConstructor>
        get() = emptyList()
    override val recordComponents: Collection<JavaRecordComponent>
        get() = emptyList()

    override fun hasDefaultConstructor(): Boolean = false

    override fun equals(other: Any?): Boolean = other is FirBackedJavaClassAdapter && resolvedClassId == other.resolvedClassId
    override fun hashCode(): Int = resolvedClassId.hashCode()
    override fun toString(): String = "FirBackedJavaClassAdapter(${resolvedClassId.asString()})"
}

/**
 * [JavaTypeParameter] wrapper that exposes a `FirTypeParameterSymbol`-backed name and identity
 * so that `JavaClassifierTypeOverAst.computeIsRaw`'s qualified-form raw-detection walk can
 * count outer-class type parameters across files without depending on the per-`FirJavaClass`
 * `MutableJavaTypeParameterStack` (which is populated at `FirJavaFacade.convertJavaClassToFir`
 * time and does not see resolution-time-synthesised adapters).
 *
 * Used by [FirBackedJavaClassAdapter] to surface cross-file outer-class type parameters.
 */
internal class FirBackedJavaTypeParameter(
    val firTypeParameterSymbol: FirTypeParameterSymbol,
) : JavaTypeParameter {
    override val name: Name get() = firTypeParameterSymbol.name
    override val isFromSource: Boolean get() = false
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null

    /**
     * Empty bounds — the adapter's role is to carry the symbol so FIR can substitute through it,
     * not to expose the parameter's own type-bound structure. Bound resolution for the parameter
     * is driven by the enhanced symbol's own `FirTypeParameter`, not by the wrapper.
     */
    override val upperBounds: Collection<JavaClassifierType> get() = emptyList()

    override fun equals(other: Any?): Boolean =
        other is FirBackedJavaTypeParameter && firTypeParameterSymbol == other.firTypeParameterSymbol

    override fun hashCode(): Int = firTypeParameterSymbol.hashCode()
    override fun toString(): String = "FirBackedJavaTypeParameter(${firTypeParameterSymbol.name})"
}

/**
 * Resolves this class's supertypes on-air using a fresh, throwaway [SupertypeComputationSession]
 * and [ScopeSession] (and the [FirSupertypeResolverVisitor] itself) against the given shared
 * [session] — the [FirSession] is the real, long-lived compiler session and is **not** disposable;
 * only the per-call computation/scope caches are. Mirrors `FirJavaElementFinder.resolveSupertypesOnAir`.
 * Used by [FirBackedJavaClassAdapter.supertypes] only when `superTypeRefs` are not yet all
 * [FirResolvedTypeRef] (the common case for already resolved outer/super classes reuses
 * `superTypeRefs` directly and never reaches this path).
 */
private fun FirRegularClass.resolveSupertypesOnAir(session: FirSession): List<FirTypeRef> {
    val visitor = FirSupertypeResolverVisitor(session, SupertypeComputationSession(), ScopeSession())
    return visitor.withFile(session.firProvider.getFirClassifierContainerFile(this.symbol)) {
        visitor.resolveSpecificClassLikeSupertypes(this, superTypeRefs, resolveRecursively = true)
    }
}

/**
 * Directly declared nested-classifier simple names of a non-[FirJavaClass] [FirRegularClass]
 * (Kotlin source or deserialized). Reading [FirRegularClass.declarations] is deliberately opted
 * into here rather than routed through a scope: the KT-74097 publication-lazy hazard this module
 * otherwise avoids is specific to [FirJavaClass]; for any other [FirRegularClass] the list is
 * populated eagerly at parse/deserialization time and reading it triggers no further resolution.
 */
@OptIn(DirectDeclarationsAccess::class)
private fun nestedClassifierNames(fir: FirRegularClass): List<Name> =
    fir.declarations.filterIsInstance<FirClassLikeDeclaration>().map { it.symbol.classId.shortClassName }
