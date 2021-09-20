/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.typeEnhancement

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.load.java.AbstractAnnotationTypeQualifierResolver
import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType
import org.jetbrains.kotlin.load.java.JavaTypeQualifiersByElementType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.types.model.*

abstract class AbstractSignatureParts<TAnnotation : Any> {
    // TODO: some of this might be better off as parameters
    abstract val annotationTypeQualifierResolver: AbstractAnnotationTypeQualifierResolver<TAnnotation>
    abstract val containerAnnotations: Iterable<TAnnotation>
    abstract val containerApplicabilityType: AnnotationQualifierApplicabilityType
    abstract val containerDefaultTypeQualifiers: JavaTypeQualifiersByElementType?
    abstract val containerIsVarargParameter: Boolean
    abstract val isCovariant: Boolean
    abstract val typeSystem: TypeSystemContext
    abstract val typeFactory: TypeSystemTypeFactoryContext

    open val enableImprovementsInStrictMode: Boolean
        get() = true

    open val skipRawTypeArguments: Boolean
        get() = false

    open val forceOnlyHeadTypeConstructor: Boolean
        get() = false

    abstract fun createRawType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): RawTypeMarker

    abstract val TAnnotation.forceWarning: Boolean

    abstract val KotlinTypeMarker.annotations: Iterable<TAnnotation>
    abstract val KotlinTypeMarker.enhancedForWarnings: KotlinTypeMarker?
    abstract val KotlinTypeMarker.fqNameUnsafe: FqNameUnsafe?
    abstract fun KotlinTypeMarker.isEqual(other: KotlinTypeMarker): Boolean
    abstract fun KotlinTypeMarker.withEnhancementForWarnings(enhancement: KotlinTypeMarker): KotlinTypeMarker?

    abstract val TypeParameterMarker.isFromJava: Boolean
    abstract fun TypeParameterMarker.upperBound(substitution: Map<TypeParameterMarker, TypeArgumentMarker>): KotlinTypeMarker

    abstract fun TypeConstructorMarker.replaceClassId(mapper: (ClassId) -> ClassId?): TypeConstructorMarker?

    open val KotlinTypeMarker.isNotNullTypeParameterCompat: Boolean
        get() = false

    open fun SimpleTypeMarker.makeDefinitelyNotNull(): SimpleTypeMarker =
        with(typeSystem) { makeSimpleTypeDefinitelyNotNullOrNotNull() }

    abstract fun SimpleTypeMarker.replace(
        constructor: TypeConstructorMarker?,
        arguments: List<TypeArgumentMarker?>,
        nullability: Boolean?
    ): SimpleTypeMarker

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

    private class TypeAndDefaultQualifiers(
        val type: KotlinTypeMarker?,
        val defaultQualifiers: JavaTypeQualifiersByElementType?,
        val typeParameterForArgument: TypeParameterMarker?
    )

    private fun TypeAndDefaultQualifiers.extractQualifiersFromAnnotations(isHeadTypeConstructor: Boolean): JavaTypeQualifiers {
        if (type == null && with(typeSystem) { typeParameterForArgument?.getVariance() } == TypeVariance.IN) {
            // Star projections can only be enhanced in one way: `?` -> `? extends <something>`. Given a Kotlin type `C<in T>
            // (declaration-site variance), this is not a valid enhancement due to conflicting variances.
            return JavaTypeQualifiers.NONE
        }

        val typeAnnotations = type?.annotations ?: emptyList()
        val typeParameterUse = with(typeSystem) { type?.typeConstructor()?.getTypeParameterClassifier() }
        val typeParameterBounds = containerApplicabilityType == AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS
        val composedAnnotation = when {
            !isHeadTypeConstructor -> typeAnnotations
            !typeParameterBounds && enableImprovementsInStrictMode ->
                // We don't apply container type use annotations to avoid double applying them like with arrays:
                //      @NotNull Integer [] f15();
                // Otherwise, in the example above we would apply `@NotNull` to `Integer` (i.e. array element; as TYPE_USE annotation)
                // and to entire array (as METHOD annotation).
                // In other words, we prefer TYPE_USE target of an annotation, and apply the annotation only according to it, if it's present.
                // See KT-24392 for more details.
                containerAnnotations.filter { !annotationTypeQualifierResolver.isTypeUseAnnotation(it) } + typeAnnotations
            else -> containerAnnotations + typeAnnotations
        }

        val annotationsMutability = annotationTypeQualifierResolver.extractMutability(composedAnnotation)
        val annotationsNullability = annotationTypeQualifierResolver.extractNullability(composedAnnotation) { forceWarning }
        if (annotationsNullability != null) {
            return JavaTypeQualifiers(
                annotationsNullability.qualifier, annotationsMutability,
                annotationsNullability.qualifier == NullabilityQualifier.NOT_NULL && typeParameterUse != null,
                annotationsNullability.isForWarningOnly
            )
        }

        val applicabilityType = when {
            isHeadTypeConstructor || typeParameterBounds -> containerApplicabilityType
            else -> AnnotationQualifierApplicabilityType.TYPE_USE
        }
        val defaultTypeQualifier = defaultQualifiers?.get(applicabilityType)

        val referencedParameterBoundsNullability = typeParameterUse?.boundsNullability
        // For type parameter uses, we have *three* options:
        //   T!! - NOT_NULL, isNotNullTypeParameter = true
        //         happens if T is bounded by @NotNull (technically !! is redundant) or context says unannotated
        //         type parameters are non-null;
        //   T   - NOT_NULL, isNotNullTypeParameter = false
        //         happens if T is bounded by @Nullable (should it?) or context says unannotated types in general are non-null;
        //   T?  - NULLABLE, isNotNullTypeParameter = false
        //         happens if context says unannotated types in general are nullable.
        // For other types, this is more straightforward (just take nullability from the context).
        // TODO: clean up the representation of those cases in JavaTypeQualifiers
        val defaultNullability =
            referencedParameterBoundsNullability?.copy(qualifier = NullabilityQualifier.NOT_NULL)
                ?: defaultTypeQualifier?.nullabilityQualifier
        val definitelyNotNull =
            referencedParameterBoundsNullability?.qualifier == NullabilityQualifier.NOT_NULL ||
                    (typeParameterUse != null && defaultTypeQualifier?.definitelyNotNull == true)

        // We should also enhance this type to satisfy the bound of the type parameter it is instantiating:
        // for C<T extends @NotNull V>, C<X!> becomes C<X!!> regardless of the above.
        val substitutedParameterBoundsNullability = typeParameterForArgument?.boundsNullability
            ?.let { if (it.qualifier == NullabilityQualifier.NULLABLE) it.copy(qualifier = NullabilityQualifier.FORCE_FLEXIBILITY) else it }
        val result = mostSpecific(substitutedParameterBoundsNullability, defaultNullability)
        return JavaTypeQualifiers(result?.qualifier, annotationsMutability, definitelyNotNull, result?.isForWarningOnly == true)
    }

    private fun mostSpecific(
        a: NullabilityQualifierWithMigrationStatus?,
        b: NullabilityQualifierWithMigrationStatus?
    ): NullabilityQualifierWithMigrationStatus? {
        if (a == null) return b // null < not null
        if (b == null) return a
        if (a.isForWarningOnly && !b.isForWarningOnly) return b // warnings < errors
        if (!a.isForWarningOnly && b.isForWarningOnly) return a
        if (a.qualifier < b.qualifier) return b // T! < T? < T
        if (a.qualifier > b.qualifier) return a
        return b // they are equal
    }

    private val TypeParameterMarker.boundsNullability: NullabilityQualifierWithMigrationStatus?
        get() = with(typeSystem) {
            if (!isFromJava) return null
            val bounds = getUpperBounds()
            val enhancedBounds = when {
                bounds.all { it.isError() } -> return null
                // TODO: what if e.g. one bound is nullable and another is not null for warnings?
                bounds.any { it.nullabilityQualifier != null } -> bounds
                bounds.any { it.enhancedForWarnings != null } -> bounds.mapNotNull { it.enhancedForWarnings }
                else -> return null
            }
            val qualifier = if (enhancedBounds.all { it.isNullableType() }) NullabilityQualifier.NULLABLE else NullabilityQualifier.NOT_NULL
            return NullabilityQualifierWithMigrationStatus(qualifier, isForWarningOnly = enhancedBounds !== bounds)
        }

    fun KotlinTypeMarker.enhance(overrides: List<KotlinTypeMarker>, predefined: TypeEnhancementInfo?): KotlinTypeMarker? {
        val indexedThisType = toIndexed()
        val indexedFromSupertypes = overrides.map { it.toIndexed() }

        // The covariant case may be hard, e.g. in the superclass the return may be Super<T>, but in the subclass it may be Derived, which
        // is declared to extend Super<T>, and propagating data here is highly non-trivial, so we only look at the head type constructor
        // (outermost type), unless the type in the subclass is interchangeable with the all the types in superclasses:
        // e.g. we have (Mutable)List<String!>! in the subclass and { List<String!>, (Mutable)List<String>! } from superclasses
        // Note that `this` is flexible here, so it's equal to it's bounds
        val onlyHeadTypeConstructor = forceOnlyHeadTypeConstructor || (isCovariant && overrides.any { !this@enhance.isEqual(it) })

        val computedResult = Array(if (onlyHeadTypeConstructor) 1 else indexedThisType.size) { index ->
            val qualifiers = indexedThisType[index].extractQualifiersFromAnnotations(index == 0)
            val superQualifiers = indexedFromSupertypes.mapNotNull { it.getOrNull(index)?.type?.extractQualifiers() }
            qualifiers.computeQualifiersForOverride(superQualifiers, index == 0 && isCovariant, index == 0 && containerIsVarargParameter)
        }
        val nextSiblingMap = IntArray(indexedThisType.size).apply { this[0] = findNextSibling(this, 0) }
        val qualifiers = IndexedJavaTypeQualifiers(predefined?.map, computedResult, nextSiblingMap)
        return enhance(qualifiers, 0)
    }

    private fun <T> T.flattenTree(result: MutableList<T>, children: (T) -> Iterable<T>?) {
        result.add(this)
        children(this)?.forEach { it.flattenTree(result, children) }
    }

    private fun <T> T.flattenTree(children: (T) -> Iterable<T>?): List<T> =
        ArrayList<T>(1).also { flattenTree(it, children) }

    private fun KotlinTypeMarker.extractAndMergeDefaultQualifiers(oldQualifiers: JavaTypeQualifiersByElementType?) =
        annotationTypeQualifierResolver.extractAndMergeDefaultQualifiers(oldQualifiers, annotations)

    // The index in the lambda is the position of the type component in a depth-first walk of the tree.
    // Example: A<B<C, D>, E<F>> - 0<1<2, 3>, 4<5>>. For flexible types, some arguments in the lower bound
    // may be replaced with star projections in the upper bound, but otherwise corresponding arguments
    // have the same index: (A<B<C>, D>..E<*, F>) -> (0<1<2>, 3>..0<1, 3>).
    private fun KotlinTypeMarker.toIndexed(): List<TypeAndDefaultQualifiers> = with(typeSystem) {
        TypeAndDefaultQualifiers(this@toIndexed, extractAndMergeDefaultQualifiers(containerDefaultTypeQualifiers), null).flattenTree {
            // Enhancement of raw type arguments may enter a loop in FE1.0.
            if (it.type == null || (skipRawTypeArguments && it.type.asFlexibleType()?.asRawType() != null)) return@flattenTree null

            val parameters = it.type.typeConstructor().getParameters()
            it.type.getArguments().mapIndexed { index, arg ->
                val parameter = parameters.getOrNull(index)
                if (arg.isStarProjection()) {
                    TypeAndDefaultQualifiers(null, it.defaultQualifiers, parameter)
                } else {
                    val type = arg.getType()
                    TypeAndDefaultQualifiers(type, type.extractAndMergeDefaultQualifiers(it.defaultQualifiers), parameter)
                }
            }
        }
    }

    // This function permits quickly skipping to the next type argument in the list produced by `toIndexed`. E.g. for the example
    // in that function's comment, the next sibling of 1 (B<C, D>) is 4 (E<F>).
    private fun KotlinTypeMarker.findNextSibling(cache: IntArray, index: Int): Int = with(typeSystem) {
        if (skipRawTypeArguments && asFlexibleType()?.asRawType() != null) return index + 1
        getArguments().fold(index + 1) { argIndex, arg ->
            if (arg.isStarProjection()) {
                argIndex + 1
            } else {
                arg.getType().findNextSibling(cache, argIndex)
            }.also { cache[argIndex] = it }
        }
    }

    private fun KotlinTypeMarker.enhance(qualifiers: IndexedJavaTypeQualifiers, index: Int): KotlinTypeMarker? =
        with(typeSystem) {
            if (isError()) return null
            val enhanced = asFlexibleType()?.enhance(qualifiers, index)
                ?: asSimpleType()?.enhance(qualifiers, index, TypeComponentPosition.INFLEXIBLE, isBoundOfRawType = false)
                ?: return null
            // TODO: if the enhancement is both for nullability warnings and mutability, then this type should be
            //   something like `enhance(mutability only).wrapEnhancement(enhance(both nullability and mutability))`.
            return if (qualifiers[index].isNullabilityQualifierForWarning) withEnhancementForWarnings(enhanced) else enhanced
        }

    private fun FlexibleTypeMarker.enhance(qualifiers: IndexedJavaTypeQualifiers, index: Int): KotlinTypeMarker? =
        with(typeSystem) {
            val isRawType = asRawType() != null
            val lowerBound = lowerBound()
            val upperBound = upperBound()
            val lowerResult = lowerBound.enhance(qualifiers, index, TypeComponentPosition.FLEXIBLE_LOWER, isRawType)
            val upperResult = upperBound.enhance(qualifiers, index, TypeComponentPosition.FLEXIBLE_UPPER, isRawType)
            when {
                lowerResult == null && upperResult == null -> null
                isRawType -> createRawType(lowerResult ?: lowerBound, upperResult ?: upperBound)
                else -> typeFactory.createFlexibleType(lowerResult ?: lowerBound, upperResult ?: upperBound)
            }
        }

    private fun SimpleTypeMarker.enhance(
        qualifiers: IndexedJavaTypeQualifiers,
        index: Int,
        position: TypeComponentPosition,
        isBoundOfRawType: Boolean
    ): SimpleTypeMarker? = with(typeSystem) {
        val shouldEnhance = position.shouldEnhance()
        val shouldEnhanceArguments = !isBoundOfRawType || !skipRawTypeArguments
        if (!shouldEnhance && (!shouldEnhanceArguments || getArguments().isEmpty())) return null

        val originalConstructor = typeConstructor()

        val effectiveQualifiers = qualifiers[index]
        val enhancedConstructor = originalConstructor.takeIf { shouldEnhance }?.let {
            when {
                effectiveQualifiers.mutability == MutabilityQualifier.READ_ONLY && position == TypeComponentPosition.FLEXIBLE_LOWER ->
                    it.replaceClassId(JavaToKotlinClassMap::mutableToReadOnly)
                effectiveQualifiers.mutability == MutabilityQualifier.MUTABLE && position == TypeComponentPosition.FLEXIBLE_UPPER ->
                    it.replaceClassId(JavaToKotlinClassMap::readOnlyToMutable)
                else -> null
            }
        }
        val enhancedNullability = effectiveQualifiers.nullability.takeIf { shouldEnhance }?.let {
            when (it) {
                NullabilityQualifier.FORCE_FLEXIBILITY -> null
                NullabilityQualifier.NULLABLE -> true
                NullabilityQualifier.NOT_NULL -> false
            }
        }

        val typeArguments = getArguments()
        val typeParameters = (enhancedConstructor ?: originalConstructor).getParameters()

        // If we've changed the classifier, the variances need to be updated, as they may be at declaration site.
        val enhancedArguments = typeArguments.mapIndexedTo(ArrayList(typeArguments.size)) { index, arg ->
            val parameter = typeParameters.getOrNull(index)
            if (enhancedConstructor == null || parameter == null) {
                null
            } else if (arg.isStarProjection()) {
                typeFactory.createStarProjection(parameter)
            } else {
                val variance = arg.getVariance().takeIf { it != parameter.getVariance() } ?: TypeVariance.INV
                typeFactory.createTypeArgument(arg.getType(), variance)
            }
        }

        if (shouldEnhanceArguments) {
            qualifiers.forEachIndexed(index, typeArguments) { globalIndex, localIndex, arg ->
                if (arg.isStarProjection()) return@forEachIndexed
                val enhanced = arg.getType().enhance(qualifiers, globalIndex) ?: return@forEachIndexed
                enhancedArguments[localIndex] = typeFactory.createTypeArgument(enhanced, arg.getVariance())
            }

            // Enhancement of unbounded `?` should be done after all explicit arguments. This is because
            // `?` is effectively the same as `? extends V` where `V` is the upper bound of the type parameter,
            // but unlike an explicitly bounded wildcard, it may reference other type parameters. For example,
            // given `class J<T, V extends @Nullable T>`, the type `J<@NotNull String, ?>` in a scope that forces
            // `?` to be flexible should be enhanced into `J<String, out (String..String?)>`.
            var typeParameterToArgument: Map<TypeParameterMarker, TypeArgumentMarker>? = null
            qualifiers.forEachIndexed(index, typeArguments) { globalIndex, localIndex, arg ->
                if (!arg.isStarProjection()) return@forEachIndexed
                val parameter = typeParameters.getOrNull(localIndex) ?: return@forEachIndexed
                val qualifier = qualifiers[globalIndex].nullability
                // KT-48515: given `C<T extends @Nullable V>`, `C<?>` in unannotated scope is `C<out V..V?>`.
                // The other options for the qualifier also have meaning, but we don't use them right now:
                //   * `C<?>` -> `C<out V?>` - does something with jspecify if `C<T extends @NullnessUnspecified V>`;
                //     maybe useful if the `?` is in a `@NullMarked` scope.
                //   * `C<?>` -> `C<out V>` - jspecify explicitly states that this does *not* happen, and we
                //     currently don't do it for JSR-305 type qualifier defaults either, but who knows.
                if (qualifier != NullabilityQualifier.FORCE_FLEXIBILITY) return@forEachIndexed

                val substitution = typeParameterToArgument
                    ?: typeParameters.zip(enhancedArguments.zip(typeArguments) { enhanced, original -> enhanced ?: original }).toMap()
                        .also { typeParameterToArgument = it }
                val upperBound = parameter.upperBound(substitution)
                if (upperBound.nullabilityQualifier == qualifier) return@forEachIndexed
                val enhanced = typeFactory.createFlexibleType(
                    upperBound.lowerBoundIfFlexible().withNullability(false),
                    upperBound.upperBoundIfFlexible().withNullability(true)
                )
                enhancedArguments[localIndex] = typeFactory.createTypeArgument(enhanced, TypeVariance.OUT)
            }
        }

        if (enhancedConstructor == null && enhancedNullability == null && enhancedArguments.all { it == null })
            return null
        val enhancedType = replace(enhancedConstructor, enhancedArguments, enhancedNullability)
        return if (effectiveQualifiers.definitelyNotNull) enhancedType.makeDefinitelyNotNull() else enhancedType
    }
}

class IndexedJavaTypeQualifiers(
    private val predefined: Map<Int, JavaTypeQualifiers>?,
    private val computed: Array<JavaTypeQualifiers>,
    private val nextSiblingMap: IntArray
) {
    operator fun get(index: Int): JavaTypeQualifiers = predefined?.get(index) ?: computed.getOrNull(index) ?: JavaTypeQualifiers.NONE

    fun nextSibling(index: Int): Int = nextSiblingMap[index]

    inline fun forEachIndexed(index: Int, arguments: List<TypeArgumentMarker>, mapper: (Int, Int, TypeArgumentMarker) -> Unit) {
        var nextGlobalIndex = index + 1
        for ((localIndex, argument) in arguments.withIndex()) {
            mapper(nextGlobalIndex.also { nextGlobalIndex = nextSibling(it) }, localIndex, argument)
        }
    }
}
