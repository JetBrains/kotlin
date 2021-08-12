/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.typeEnhancement

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.load.java.AbstractAnnotationTypeQualifierResolver
import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType
import org.jetbrains.kotlin.load.java.JavaTypeQualifiersByElementType
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext

abstract class AbstractSignatureParts<Annotation : Any> {
    abstract val annotationTypeQualifierResolver: AbstractAnnotationTypeQualifierResolver<Annotation>
    abstract val enableImprovementsInStrictMode: Boolean
    abstract val containerAnnotations: Iterable<Annotation>
    abstract val containerApplicabilityType: AnnotationQualifierApplicabilityType
    abstract val containerDefaultTypeQualifiers: JavaTypeQualifiersByElementType?
    abstract val containerIsVarargParameter: Boolean
    abstract val isCovariant: Boolean
    abstract val typeSystem: TypeSystemContext

    open val forceOnlyHeadTypeConstructor: Boolean
        get() = false

    abstract val Annotation.forceWarning: Boolean

    abstract val KotlinTypeMarker.annotations: Iterable<Annotation>
    abstract val KotlinTypeMarker.enhancedForWarnings: KotlinTypeMarker?
    abstract val KotlinTypeMarker.fqNameUnsafe: FqNameUnsafe?
    abstract fun KotlinTypeMarker.isEqual(other: KotlinTypeMarker): Boolean

    abstract val TypeParameterMarker.starProjectedType: KotlinTypeMarker?
    abstract val TypeParameterMarker.isFromJava: Boolean

    open val KotlinTypeMarker.isNotNullTypeParameterCompat: Boolean
        get() = false

    private val KotlinTypeMarker.nullabilityQualifier: NullabilityQualifier?
        get() = with(typeSystem) {
            when {
                lowerBoundIfFlexible().isMarkedNullable() -> NullabilityQualifier.NULLABLE
                !upperBoundIfFlexible().isMarkedNullable() -> NullabilityQualifier.NOT_NULL
                else -> null
            }
        }

    private fun KotlinTypeMarker.extractQualifiers(): JavaTypeQualifiers {
        val forErrors = nullabilityQualifier
        val forErrorsOrWarnings = forErrors ?: enhancedForWarnings?.nullabilityQualifier
        val mutability = with(typeSystem) {
            when {
                JavaToKotlinClassMap.isReadOnly(lowerBoundIfFlexible().fqNameUnsafe) -> MutabilityQualifier.READ_ONLY
                JavaToKotlinClassMap.isMutable(upperBoundIfFlexible().fqNameUnsafe) -> MutabilityQualifier.MUTABLE
                else -> null
            }
        }
        val isNotNullTypeParameter = with(typeSystem) { isDefinitelyNotNullType() } || isNotNullTypeParameterCompat
        return JavaTypeQualifiers(forErrorsOrWarnings, mutability, isNotNullTypeParameter, forErrorsOrWarnings != forErrors)
    }

    private fun TypeAndDefaultQualifiers.extractQualifiersFromAnnotations(): JavaTypeQualifiers {
        val isHeadTypeConstructor = typeParameterForArgument == null
        val typeOrBound = type ?: typeParameterForArgument?.starProjectedType ?: return JavaTypeQualifiers.NONE
        val typeParameterUse = with(typeSystem) { typeOrBound.typeConstructor().getTypeParameterClassifier() }
        val typeParameterBounds = containerApplicabilityType == AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS
        val composedAnnotation = when {
            !isHeadTypeConstructor -> typeOrBound.annotations
            !typeParameterBounds && enableImprovementsInStrictMode ->
                // We don't apply container type use annotations to avoid double applying them like with arrays:
                //      @NotNull Integer [] f15();
                // Otherwise, in the example above we would apply `@NotNull` to `Integer` (i.e. array element; as TYPE_USE annotation)
                // and to entire array (as METHOD annotation).
                // In other words, we prefer TYPE_USE target of an annotation, and apply the annotation only according to it, if it's present.
                // See KT-24392 for more details.
                containerAnnotations.filter { !annotationTypeQualifierResolver.isTypeUseAnnotation(it) } + typeOrBound.annotations
            else -> containerAnnotations + typeOrBound.annotations
        }

        val annotationsMutability = annotationTypeQualifierResolver.extractMutability(composedAnnotation)
        val annotationsNullability = annotationTypeQualifierResolver.extractNullability(composedAnnotation) { forceWarning }

        if (type != null && annotationsNullability != null) {
            return JavaTypeQualifiers(
                annotationsNullability.qualifier, annotationsMutability,
                annotationsNullability.qualifier == NullabilityQualifier.NOT_NULL && typeParameterUse != null,
                annotationsNullability.isForWarningOnly
            )
        }

        // TODO: check whether the code below works properly for star projections (when typeOrBound != type)
        val applicabilityType = when {
            isHeadTypeConstructor || typeParameterBounds -> containerApplicabilityType
            else -> AnnotationQualifierApplicabilityType.TYPE_USE
        }
        val defaultTypeQualifier = defaultQualifiers?.get(applicabilityType)
            ?.takeIf { it.affectsTypeParameterBasedTypes || typeParameterUse == null }

        val referencedParameterBoundsNullability = typeParameterUse?.boundsNullability()
        // For type parameter uses, we have *three* options:
        //   T!! - NOT_NULL, isNotNullTypeParameter = true
        //         happens if T is bounded by @NotNull (technically !! is redundant) or context says unannotated
        //         type parameters are non-null;
        //   T   - NOT_NULL, isNotNullTypeParameter = false
        //         happens if T is bounded by @Nullable or context says unannotated types in general are non-null;
        //   T?  - NULLABLE, isNotNullTypeParameter = false
        //         happens if context says unannotated types in general are nullable.
        // For other types, this is more straightforward (just take nullability from the context).
        // TODO: clean up the representation of those cases in JavaTypeQualifiers
        val defaultNullability =
            referencedParameterBoundsNullability?.copy(qualifier = NullabilityQualifier.NOT_NULL)
                ?: defaultTypeQualifier?.nullabilityQualifier
        val isNotNullTypeParameter =
            referencedParameterBoundsNullability?.qualifier == NullabilityQualifier.NOT_NULL ||
                    (typeParameterUse != null && defaultTypeQualifier?.nullabilityQualifier?.qualifier == NullabilityQualifier.NOT_NULL)

        // We should also enhance this type to satisfy the bound of the type parameter it is instantiating:
        // for C<T extends @NotNull V>, C<X!> becomes C<X!!> regardless of the above.
        val substitutedParameterBoundsNullability = typeParameterForArgument?.boundsNullability()
        val result = when {
            substitutedParameterBoundsNullability == null -> defaultNullability
            defaultNullability == null ->
                if (substitutedParameterBoundsNullability.qualifier == NullabilityQualifier.NULLABLE)
                    substitutedParameterBoundsNullability.copy(qualifier = NullabilityQualifier.FORCE_FLEXIBILITY)
                else
                    substitutedParameterBoundsNullability
            type == null -> substitutedParameterBoundsNullability
            else -> mostSpecific(substitutedParameterBoundsNullability, defaultNullability)
        }
        return JavaTypeQualifiers(result?.qualifier, annotationsMutability, isNotNullTypeParameter, result?.isForWarningOnly == true)
    }

    private fun mostSpecific(
        a: NullabilityQualifierWithMigrationStatus,
        b: NullabilityQualifierWithMigrationStatus
    ): NullabilityQualifierWithMigrationStatus {
        // TODO: this probably behaves really weirdly when some of those are warnings.
        if (a.qualifier == NullabilityQualifier.FORCE_FLEXIBILITY) return b
        if (b.qualifier == NullabilityQualifier.FORCE_FLEXIBILITY) return a
        if (a.qualifier == NullabilityQualifier.NULLABLE) return b
        if (b.qualifier == NullabilityQualifier.NULLABLE) return a
        assert(a.qualifier == b.qualifier && a.qualifier == NullabilityQualifier.NOT_NULL) {
            "Expected everything is NOT_NULL, but $a and $b are found"
        }

        return NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL)
    }

    private val List<KotlinTypeMarker>.boundsNullabilityQualifier: NullabilityQualifier
        get() = with(typeSystem) { if (all { it.isNullableType() }) NullabilityQualifier.NULLABLE else NullabilityQualifier.NOT_NULL }

    private fun TypeParameterMarker.boundsNullability(): NullabilityQualifierWithMigrationStatus? {
        if (!isFromJava) return null

        val upperBounds = with(typeSystem) { getUpperBounds() }
        if (upperBounds.all { with(typeSystem) { it.isError() } }) return null
        if (upperBounds.all { it.nullabilityQualifier == null }) {
            val forWarnings = upperBounds.mapNotNull { it.enhancedForWarnings }
            if (forWarnings.isEmpty()) return null
            return NullabilityQualifierWithMigrationStatus(forWarnings.boundsNullabilityQualifier, isForWarningOnly = true)
        }
        return NullabilityQualifierWithMigrationStatus(upperBounds.boundsNullabilityQualifier, isForWarningOnly = false)
    }

    fun KotlinTypeMarker.computeIndexedQualifiers(
        overrides: Iterable<KotlinTypeMarker>, predefined: TypeEnhancementInfo?
    ): IndexedJavaTypeQualifiers {
        val indexedThisType = toIndexed()
        val indexedFromSupertypes = overrides.map { it.toIndexed() }

        // The covariant case may be hard, e.g. in the superclass the return may be Super<T>, but in the subclass it may be Derived, which
        // is declared to extend Super<T>, and propagating data here is highly non-trivial, so we only look at the head type constructor
        // (outermost type), unless the type in the subclass is interchangeable with the all the types in superclasses:
        // e.g. we have (Mutable)List<String!>! in the subclass and { List<String!>, (Mutable)List<String>! } from superclasses
        // Note that `this` is flexible here, so it's equal to it's bounds
        val onlyHeadTypeConstructor = forceOnlyHeadTypeConstructor ||
                (isCovariant && overrides.any { !this@computeIndexedQualifiers.isEqual(it) })

        val treeSize = if (onlyHeadTypeConstructor) 1 else indexedThisType.size
        val computedResult = Array(treeSize) { index ->
            val qualifiers = indexedThisType[index].extractQualifiersFromAnnotations()
            val superQualifiers = indexedFromSupertypes.mapNotNull { it.getOrNull(index)?.type?.extractQualifiers() }
            qualifiers.computeQualifiersForOverride(superQualifiers, index == 0 && isCovariant, index == 0 && containerIsVarargParameter)
        }
        return { index -> predefined?.map?.get(index) ?: computedResult.getOrElse(index) { JavaTypeQualifiers.NONE } }
    }

    private fun <T> T.flattenTree(result: MutableList<T>, children: (T) -> Iterable<T>?) {
        result.add(this)
        children(this)?.forEach { it.flattenTree(result, children) }
    }

    private fun <T> T.flattenTree(children: (T) -> Iterable<T>?): List<T> =
        ArrayList<T>(1).also { flattenTree(it, children) }

    private fun KotlinTypeMarker.extractAndMergeDefaultQualifiers(oldQualifiers: JavaTypeQualifiersByElementType?) =
        annotationTypeQualifierResolver.extractAndMergeDefaultQualifiers(oldQualifiers, annotations)

    private fun KotlinTypeMarker.toIndexed(): List<TypeAndDefaultQualifiers> = with(typeSystem) {
        TypeAndDefaultQualifiers(this@toIndexed, extractAndMergeDefaultQualifiers(containerDefaultTypeQualifiers), null).flattenTree {
            it.type?.typeConstructor()?.getParameters()?.zip(it.type.getArguments()) { parameter, arg ->
                if (arg.isStarProjection()) {
                    TypeAndDefaultQualifiers(null, it.defaultQualifiers, parameter)
                } else {
                    val type = arg.getType()
                    TypeAndDefaultQualifiers(type, type.extractAndMergeDefaultQualifiers(it.defaultQualifiers), parameter)
                }
            }
        }
    }

    private class TypeAndDefaultQualifiers(
        val type: KotlinTypeMarker?,
        val defaultQualifiers: JavaTypeQualifiersByElementType?,
        val typeParameterForArgument: TypeParameterMarker?
    )
}

typealias IndexedJavaTypeQualifiers = (Int) -> JavaTypeQualifiers
