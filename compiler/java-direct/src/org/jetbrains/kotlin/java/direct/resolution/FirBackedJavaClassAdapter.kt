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
 * `(javaType.classifier as? JavaClass)?.classId` path resolves without a side-channel. Also
 * exposes a real [outerClass] chain whose [typeParameters] carry `FirTypeParameterSymbol`s, so
 * outer-class type-parameter counts can be inspected across files.
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
     * Lazily resolved FIR symbol for [resolvedClassId]; `null` when the symbol provider does not
     * know it (the adapter still answers `classId` correctly).
     */
    @OptIn(SymbolInternals::class)
    private val firRegularClass: FirRegularClass? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        (session.cycleSafeClassLikeSymbol(resolvedClassId) as? FirRegularClassSymbol)?.fir
    }

    /**
     * For a [FirJavaClass], decided structurally from [FirOuterClassTypeParameterRef] entries
     * (present iff the class is a non-static inner class) — avoids reading `firClass.status`,
     * which is lazy and runs status-transformer extensions. Non-Java FIR classes fall back to
     * `status.isInner`.
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
     * Own type parameters (outer-class params are filtered out; callers walk [outerClass] for
     * them). Reads `nonEnhancedTypeParameters` rather than `typeParameters` to avoid the
     * `FirSignatureEnhancement` cycle through `JavaTypeConversion.isRaw`. Cached because FIR
     * matches Java type parameters by object identity.
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
     * Resolved supertype chain, mirroring `FirJavaElementFinder.resolveSupertypesOnAir`: prefer
     * already-resolved `superTypeRefs`, otherwise resolve on-air. Each cone type is exposed as a
     * [FirBackedJavaClassifierType] so its arguments can be read back. Guarded by
     * [cycleGuardedSupertypeWalk]; symbol resolution funnels through [cycleSafeClassLikeSymbol].
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
     * Directly declared nested-classifier simple names, from enhancement-free primitives:
     * [FirJavaClass.existingNestedClassifierNames] for Java (populated structurally at
     * conversion time), or [nestedClassifierNames] for other FIR classes.
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
     * Resolves a directly declared nested class by [name], probing the candidate `ClassId` via
     * [cycleSafeClassLikeSymbol] without forcing FIR enhancement or `declarations`. Wrapping the
     * result in another adapter is identity-safe: none of this adapter's nested classes can be
     * same-file source (a Kotlin/binary class cannot declare a class from a source-only unit).
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
 * [JavaTypeParameter] wrapper with `FirTypeParameterSymbol`-backed name and identity, so
 * outer-class type parameters can be counted across files without the per-`FirJavaClass`
 * `MutableJavaTypeParameterStack` (which does not see resolution-time-synthesised adapters).
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
     * Empty — the wrapper only carries the symbol; bound resolution is driven by the enhanced
     * symbol's own `FirTypeParameter`.
     */
    override val upperBounds: Collection<JavaClassifierType> get() = emptyList()

    override fun equals(other: Any?): Boolean =
        other is FirBackedJavaTypeParameter && firTypeParameterSymbol == other.firTypeParameterSymbol

    override fun hashCode(): Int = firTypeParameterSymbol.hashCode()
    override fun toString(): String = "FirBackedJavaTypeParameter(${firTypeParameterSymbol.name})"
}

/**
 * Resolves this class's supertypes on-air with a fresh, throwaway [SupertypeComputationSession]
 * and [ScopeSession] against the shared, long-lived [session]. Mirrors
 * `FirJavaElementFinder.resolveSupertypesOnAir`.
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
