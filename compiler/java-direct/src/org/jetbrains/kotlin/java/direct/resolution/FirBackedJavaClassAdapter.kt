/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.java.JavaTypeParameterWithFirSymbol
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Minimal [JavaClass] adapter that exposes a [ClassId] resolved by the model's own resolver,
 * used to populate `JavaClassifierType.classifier` for cross-file references.
 */
internal class FirBackedJavaClassAdapter(
    private val resolvedClassId: ClassId,
    private val sessionAccess: LazySessionAccess,
) : JavaClass {

    override val name: Name = resolvedClassId.shortClassName

    override val fqName: FqName = resolvedClassId.asSingleFqName()

    override val outerClass: JavaClass? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        resolvedClassId.outerClassId?.let { FirBackedJavaClassAdapter(it, sessionAccess) }
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
        (sessionAccess.classLikeSymbol(resolvedClassId) as? FirRegularClassSymbol)?.fir
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
     * `FirTypeParameterSymbol`s. Outer-class type params (encoded as
     * [FirOuterClassTypeParameterRef] entries on `FirJavaClass.nonEnhancedTypeParameters`) are
     * filtered out — the model's `JavaClassifierTypeOverAst.computeTypeArguments` walks
     * [outerClass] recursively and adds them via the outer chain.
     *
     * Reading [FirJavaClass.nonEnhancedTypeParameters] (rather than `typeParameters`) avoids the
     * `FirSignatureEnhancement` cycle through `JavaTypeConversion.isRaw`.
     *
     * The wrappers implement [JavaTypeParameterWithFirSymbol]; FIR's
     * `JavaTypeConversion.kt` `is JavaTypeParameter ->` branch reads
     * `firTypeParameterSymbol` directly without consulting any
     * `MutableJavaTypeParameterStack`.
     */
    override val typeParameters: List<JavaTypeParameter>
        get() {
            val fir = firRegularClass ?: return emptyList()
            val refs: List<FirTypeParameterRef> =
                if (fir is FirJavaClass) fir.nonEnhancedTypeParameters else fir.typeParameters
            return refs
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

    override val supertypes: Collection<JavaClassifierType>
        get() = emptyList()
    override val innerClassNames: Collection<Name>
        get() = emptyList()
    override fun findInnerClass(name: Name): JavaClass? = null

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
 * [JavaTypeParameter] wrapper that carries its [FirTypeParameterSymbol] directly via
 * [JavaTypeParameterWithFirSymbol] for `JavaTypeConversion`'s `is JavaTypeParameter ->` branch.
 *
 * Used by [FirBackedJavaClassAdapter] to expose cross-file outer-class type parameters in a way
 * FIR can resolve without consulting any `MutableJavaTypeParameterStack` (which is populated
 * per-`FirJavaClass` at construction time and does not see resolution-time-synthesised
 * adapters).
 */
internal class FirBackedJavaTypeParameter(
    override val firTypeParameterSymbol: FirTypeParameterSymbol,
) : JavaTypeParameterWithFirSymbol {
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
