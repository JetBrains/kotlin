/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaConstructor
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.load.java.structure.JavaRecordComponent
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Minimal [JavaClass] adapter that exposes a [ClassId] resolved by the model's own resolver,
 * used to populate `JavaClassifierType.classifier` for cross-file references under post-Step-4.5a
 * `FirSession` injection (per
 * `compiler/java-direct/implDocs/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` Step 4.5b).
 *
 * The adapter's reason to exist is so that `JavaTypeConversion.resolveTypeName` can return to its
 * pre-`java-direct` body — `(javaType.classifier as? JavaClass)?.classId ?: ...` — without the
 * `resolvedClassId` side-channel that Step 4.5a introduced.
 *
 * Members the FIR side reads off this adapter:
 *  - `JavaClass.classId` (i.e. [name] + [fqName] + [outerClass.classId])
 *  - [fqName] for `JavaClassifier.isTriviallyFlexible()`
 *
 * Members the model-internal `JavaClassifierTypeOverAst` consumers read:
 *  - `computeIsRaw`: [typeParameters].size — count materialised lazily from the FIR symbol so
 *    raw-type detection works for cross-file references (with `classifier != null`, FIR's own
 *    raw-type detection at `JavaTypeConversion.kt` line ~161 short-circuits and defers to the
 *    model's `isRaw`).
 *  - `computeTypeArguments`: [outerClass] (recursive), [typeParameters] count, [isStatic]
 *  - `computeClassifierQualifiedName`: [fqName]
 *
 * Everything else returns a safe default. The adapter is **not** intended as a general-purpose
 * `JavaClass` view; widening its surface (full type-parameter wrappers with bounds, supertypes,
 * declared members) is deferred to the structural adapter that Step 4.5c / 4.5d may need.
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

    /**
     * The adapter has no AST source attached. Returning `false` matches the pre-`java-direct`
     * invariant for cross-file classifiers reached through the FIR symbol provider (binary or
     * Kotlin), which were never source-backed in the model's view.
     */
    override val isFromSource: Boolean
        get() = false

    /**
     * Lazily resolved FIR symbol for [resolvedClassId]. Used to materialise [typeParameters]'s
     * count without forcing the model to wrap each `FirTypeParameterRef` in a full
     * `JavaTypeParameter` adapter (the count is the only thing `JavaClassifierTypeOverAst.computeIsRaw`
     * and the implicit-outer walk in `computeTypeArguments` need).
     *
     * Returns `null` when no symbol is registered for [resolvedClassId] (cross-file reference to
     * a class the symbol provider does not know — the adapter still answers `classId` correctly).
     */
    @OptIn(SymbolInternals::class)
    private val firRegularClass: FirRegularClass? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        (sessionAccess.classLikeSymbol(resolvedClassId) as? FirRegularClassSymbol)?.fir
    }

    /**
     * Returning `true` short-circuits `JavaClassifierTypeOverAst.computeTypeArguments`'s
     * implicit-outer-type-parameter walk for cross-file references — the same shape as
     * pre-`java-direct`, where `classifier == null` made the walk skip via the early
     * `javaClass == null` return. Cross-file inner-class type-arg substitution is handled
     * separately by `JavaTypeConversion.findOuterTypeArgsFromHierarchy` (consumed via
     * `containingClassIds`); letting the model-side walk proceed would feed FIR placeholder
     * `JavaTypeParameter` instances (the adapter has no real type-param symbols to wrap) and
     * break downstream substitution.
     */
    override val isStatic: Boolean
        get() = true

    /**
     * Type parameter list with the correct **count** for cross-file references. For
     * [FirJavaClass] symbols we read [FirJavaClass.nonEnhancedTypeParameters] — reading
     * `FirRegularClass.typeParameters` directly would trigger `FirSignatureEnhancement`
     * which itself calls `JavaTypeConversion.isRaw` (→ adapter.typeParameters) and would
     * recurse infinitely (the bound-enhancement-driven cycle observed during Step 4.5b
     * implementation, stack: enhanceTypeParameterBounds → resolveIfJavaType → isRaw →
     * adapter.typeParameters → typeParameters_delegate → enhanceTypeParameterBounds).
     *
     * For non-Java FIR classes (Kotlin / built-in / deserialized) the same cycle does
     * not arise — those classes' type parameters are populated eagerly by the resolver,
     * not lazily by enhancement — so [FirRegularClass.typeParameters] is safe.
     *
     * Each entry is a [PlaceholderJavaTypeParameter] — name and bounds are not exposed
     * because no consumer reads them off the adapter (`computeIsRaw` only needs `.size`).
     *
     * Returns `emptyList()` when the FIR symbol is unavailable.
     */
    override val typeParameters: List<JavaTypeParameter>
        get() {
            val fir = firRegularClass ?: return emptyList()
            val refs: List<FirTypeParameterRef> = if (fir is FirJavaClass) fir.nonEnhancedTypeParameters else fir.typeParameters
            return refs.map { ref -> PlaceholderJavaTypeParameter(ref.symbol.name) }
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
 * Placeholder [JavaTypeParameter] used by [FirBackedJavaClassAdapter.typeParameters] to expose
 * the right count without materialising upper bounds. See the KDoc on
 * [FirBackedJavaClassAdapter.typeParameters] for why this is sufficient.
 */
private class PlaceholderJavaTypeParameter(override val name: Name) : JavaTypeParameter {
    override val isFromSource: Boolean get() = false
    override val annotations: Collection<JavaAnnotation> get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean get() = false
    override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
    override val upperBounds: Collection<JavaClassifierType> get() = emptyList()
}
