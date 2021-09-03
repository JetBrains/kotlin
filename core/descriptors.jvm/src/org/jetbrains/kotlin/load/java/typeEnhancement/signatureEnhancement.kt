/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.java.typeEnhancement

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.composeAnnotations
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.descriptors.*
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.copyWithNewDefaultTypeQualifiers
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaAnnotationDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaTypeParameterDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.isJavaField
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.load.kotlin.signature
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.deprecation.DEPRECATED_FUNCTION_KEY
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class SignatureEnhancement(private val typeEnhancement: JavaTypeEnhancement) {
    fun <D : CallableMemberDescriptor> enhanceSignatures(c: LazyJavaResolverContext, platformSignatures: Collection<D>): Collection<D> {
        return platformSignatures.map {
            it.enhanceSignature(c)
        }
    }

    private fun <D : CallableMemberDescriptor> D.getDefaultAnnotations(c: LazyJavaResolverContext): Annotations {
        val topLevelClassifier = getTopLevelContainingClassifier() ?: return annotations
        val moduleAnnotations = (topLevelClassifier as? LazyJavaClassDescriptor)?.moduleAnnotations

        if (moduleAnnotations.isNullOrEmpty()) return annotations

        val moduleAnnotationDescriptors = moduleAnnotations.map { LazyJavaAnnotationDescriptor(c, it, isFreshlySupportedAnnotation = true) }

        return Annotations.create(annotations + moduleAnnotationDescriptors)
    }

    private fun <D : CallableMemberDescriptor> D.enhanceSignature(c: LazyJavaResolverContext): D {
        // TODO type parameters
        // TODO use new type parameters while enhancing other types
        // TODO Propagation into generic type arguments

        if (this !is JavaCallableMemberDescriptor) return this

        // Fake overrides with one overridden has been enhanced before
        if (kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE && original.overriddenDescriptors.size == 1) return this

        val memberContext = c.copyWithNewDefaultTypeQualifiers(getDefaultAnnotations(c))

        // When loading method as an override for a property, all annotations are stick to its getter
        val annotationOwnerForMember =
            if (this is JavaPropertyDescriptor && getter?.isDefault == false)
                getter!!
            else
                this

        val receiverTypeEnhancement =
            if (extensionReceiverParameter != null)
                partsForValueParameter(
                    parameterDescriptor =
                    annotationOwnerForMember.safeAs<FunctionDescriptor>()
                        ?.getUserData(JavaMethodDescriptor.ORIGINAL_VALUE_PARAMETER_FOR_EXTENSION_RECEIVER),
                    methodContext = memberContext
                ) { it.extensionReceiverParameter!!.type }.enhance()
            else null

        val predefinedEnhancementInfo =
            (this as? JavaMethodDescriptor)
                ?.run { SignatureBuildingComponents.signature(this.containingDeclaration as ClassDescriptor, this.computeJvmDescriptor()) }
                ?.let { signature -> PREDEFINED_FUNCTION_ENHANCEMENT_INFO_BY_SIGNATURE[signature] }


        predefinedEnhancementInfo?.let {
            assert(it.parametersInfo.size == valueParameters.size) {
                "Predefined enhancement info for $this has ${it.parametersInfo.size}, but ${valueParameters.size} expected"
            }
        }

        val valueParameterEnhancements = annotationOwnerForMember.valueParameters.map { p ->
            partsForValueParameter(p, memberContext) { it.valueParameters[p.index].type }
                .enhance(predefinedEnhancementInfo?.parametersInfo?.getOrNull(p.index))
        }

        val returnTypeEnhancement =
            parts(
                typeContainer = annotationOwnerForMember, isCovariant = true,
                containerContext = memberContext,
                containerApplicabilityType =
                if (this.safeAs<PropertyDescriptor>()?.isJavaField == true)
                    AnnotationQualifierApplicabilityType.FIELD
                else
                    AnnotationQualifierApplicabilityType.METHOD_RETURN_TYPE
            ) { it.returnType!! }.enhance(predefinedEnhancementInfo?.returnTypeInfo)

        val containsFunctionN = returnType!!.containsFunctionN() ||
                extensionReceiverParameter?.type?.containsFunctionN() ?: false ||
                valueParameters.any { it.type.containsFunctionN() }
        val additionalUserData = if (containsFunctionN)
            DEPRECATED_FUNCTION_KEY to DeprecationCausedByFunctionNInfo(this)
        else
            null

        if (receiverTypeEnhancement != null || returnTypeEnhancement != null || valueParameterEnhancements.any { it != null } ||
            additionalUserData != null
        ) {
            @Suppress("UNCHECKED_CAST")
            return this.enhance(
                receiverTypeEnhancement ?: extensionReceiverParameter?.type,
                valueParameterEnhancements.mapIndexed { index, enhanced ->
                    ValueParameterData(enhanced ?: valueParameters[index].type, false)
                },
                returnTypeEnhancement ?: returnType!!,
                additionalUserData
            ) as D
        }

        return this
    }

    fun enhanceTypeParameterBounds(
        typeParameter: TypeParameterDescriptor,
        bounds: List<KotlinType>,
        context: LazyJavaResolverContext
    ): List<KotlinType> {
        return bounds.map { bound ->
            // TODO: would not enhancing raw type arguments be sufficient?
            if (bound.contains { it is RawType }) return@map bound

            SignatureParts(
                typeEnhancement, typeParameter, bound, emptyList(), false, context,
                AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS,
                typeParameterBounds = true
            ).enhance() ?: bound
        }
    }

    /*
     * This method should be only used for type enhancement of base classes' type arguments:
     *      class A extends B<@NotNull Integer> {}
     */
    fun enhanceSuperType(type: KotlinType, context: LazyJavaResolverContext) =
        SignatureParts(
            typeEnhancement, typeContainer = null, type, emptyList(), isCovariant = false,
            context, AnnotationQualifierApplicabilityType.TYPE_USE, isSuperTypesEnhancement = true
        ).enhance() ?: type

    private fun KotlinType.containsFunctionN(): Boolean =
        TypeUtils.contains(this) {
            val classifier = it.constructor.declarationDescriptor ?: return@contains false
            classifier.name == JavaToKotlinClassMap.FUNCTION_N_FQ_NAME.shortName() &&
                    classifier.fqNameOrNull() == JavaToKotlinClassMap.FUNCTION_N_FQ_NAME
        }


    private fun CallableMemberDescriptor.parts(
        typeContainer: Annotated?,
        isCovariant: Boolean,
        containerContext: LazyJavaResolverContext,
        containerApplicabilityType: AnnotationQualifierApplicabilityType,
        collector: (CallableMemberDescriptor) -> KotlinType
    ): SignatureParts {
        return SignatureParts(
            typeEnhancement,
            typeContainer,
            collector(this),
            this.overriddenDescriptors.map {
                collector(it)
            },
            isCovariant,
            // recompute default type qualifiers using type annotations
            containerContext.copyWithNewDefaultTypeQualifiers(collector(this).annotations),
            containerApplicabilityType
        )
    }
}

private class SignatureParts(
    private val typeEnhancement: JavaTypeEnhancement,
    private val typeContainer: Annotated?,
    private val fromOverride: KotlinType,
    private val fromOverridden: Collection<KotlinType>,
    private val isCovariant: Boolean,
    private val containerContext: LazyJavaResolverContext,
    private val containerApplicabilityType: AnnotationQualifierApplicabilityType,
    private val typeParameterBounds: Boolean = false,
    private val isSuperTypesEnhancement: Boolean = false
) {
    private val isForVarargParameter get() = typeContainer.safeAs<ValueParameterDescriptor>()?.varargElementType != null

    fun enhance(predefined: TypeEnhancementInfo? = null): KotlinType? =
        with(typeEnhancement) {
            fromOverride.enhance(computeIndexedQualifiersForOverride(predefined), isSuperTypesEnhancement)
        }

    private val KotlinType.isNotNullTypeParameter: Boolean
        get() = unwrap().let { it is NotNullTypeParameter || it is DefinitelyNotNullType }

    private val KotlinType.fqNameUnsafe: FqNameUnsafe?
        get() = TypeUtils.getClassDescriptor(this)?.let { DescriptorUtils.getFqName(it) }

    private val KotlinType.nullabilityQualifier: NullabilityQualifier?
        get() = when {
            lowerIfFlexible().isMarkedNullable -> NullabilityQualifier.NULLABLE
            !upperIfFlexible().isMarkedNullable -> NullabilityQualifier.NOT_NULL
            else -> null
        }

    private val KotlinType.mutabilityQualifier: MutabilityQualifier?
        get() = when {
            JavaToKotlinClassMap.isReadOnly(lowerIfFlexible().fqNameUnsafe) -> MutabilityQualifier.READ_ONLY
            JavaToKotlinClassMap.isMutable(upperIfFlexible().fqNameUnsafe) -> MutabilityQualifier.MUTABLE
            else -> null
        }

    private fun KotlinType.extractQualifiers(): JavaTypeQualifiers {
        val forErrors = nullabilityQualifier
        val forErrorsOrWarnings = forErrors ?: unwrapEnhancement().nullabilityQualifier
        return JavaTypeQualifiers(forErrorsOrWarnings, mutabilityQualifier, isNotNullTypeParameter, forErrorsOrWarnings != forErrors)
    }

    private fun TypeAndDefaultQualifiers.extractQualifiersFromAnnotations(): JavaTypeQualifiers {
        if (type == null && typeParameterForArgument?.variance == Variance.IN_VARIANCE) {
            // Star projections can only be enhanced in one way: `?` -> `? extends <something>`. Given a Kotlin type `C<in T>
            // (declaration-site variance), this is not a valid enhancement due to conflicting variances.
            return JavaTypeQualifiers.NONE
        }

        val annotationTypeQualifierResolver = containerContext.components.annotationTypeQualifierResolver
        val areImprovementsInStrictMode = containerContext.components.settings.typeEnhancementImprovementsInStrictMode

        val isHeadTypeConstructor = typeParameterForArgument == null
        val typeOrBound = type ?: typeParameterForArgument!!.starProjectionType()
        val composedAnnotation =
            if (isHeadTypeConstructor && typeContainer != null && !typeParameterBounds && areImprovementsInStrictMode) {
                val filteredContainerAnnotations = typeContainer.annotations.filter {
                    /*
                     * We don't apply container type use annotations to avoid double applying them like with arrays:
                     *      @NotNull Integer [] f15();
                     * Otherwise, in the example above we would apply `@NotNull` to `Integer` (i.e. array element; as TYPE_USE annotation)
                     * and to entire array (as METHOD annotation).
                     * In other words, we prefer TYPE_USE target of an annotation, and apply the annotation only according to it, if it's present.
                     * See KT-24392 for more details.
                     */
                    !annotationTypeQualifierResolver.isTypeUseAnnotation(it)
                }
                composeAnnotations(Annotations.create(filteredContainerAnnotations), typeOrBound.annotations)
            } else if (isHeadTypeConstructor && typeContainer != null) {
                composeAnnotations(typeContainer.annotations, typeOrBound.annotations)
            } else typeOrBound.annotations

        val annotationsMutability = annotationTypeQualifierResolver.extractMutability(composedAnnotation)
        val annotationsNullability = annotationTypeQualifierResolver.extractNullability(composedAnnotation) {
            (this is LazyJavaAnnotationDescriptor && (isFreshlySupportedTypeUseAnnotation || typeParameterBounds) && !areImprovementsInStrictMode) ||
                    (this is PossiblyExternalAnnotationDescriptor && isIdeExternalAnnotation)
        }.takeUnless { type == null }

        if (annotationsNullability != null) {
            return JavaTypeQualifiers(
                annotationsNullability.qualifier, annotationsMutability,
                annotationsNullability.qualifier == NullabilityQualifier.NOT_NULL && typeOrBound.isTypeParameter(),
                annotationsNullability.isForWarningOnly
            )
        }

        val defaultTypeQualifier = (
                if (isHeadTypeConstructor)
                    containerContext.defaultTypeQualifiers?.get(containerApplicabilityType)
                else
                    defaultQualifiers?.get(
                        if (typeParameterBounds)
                            AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS
                        else
                            AnnotationQualifierApplicabilityType.TYPE_USE
                    )
                )?.takeIf { (it.affectsTypeParameterBasedTypes || !typeOrBound.isTypeParameter()) && (it.affectsStarProjection || type != null) }

        val (nullabilityFromBoundsForTypeBasedOnTypeParameter, isTypeParameterWithNotNullableBounds) =
            typeOrBound.nullabilityInfoBoundsForTypeParameterUsage()
        val nullabilityInfo = computeNullabilityInfoInTheAbsenceOfExplicitAnnotation(
            nullabilityFromBoundsForTypeBasedOnTypeParameter,
            defaultTypeQualifier,
            typeParameterForArgument
        )

        val isNotNullTypeParameter =
            isTypeParameterWithNotNullableBounds || defaultTypeQualifier?.makesTypeParameterNotNull == true

        return JavaTypeQualifiers(
            nullabilityInfo?.qualifier, annotationsMutability,
            isNotNullTypeParameter = isNotNullTypeParameter && typeOrBound.isTypeParameter(),
            isNullabilityQualifierForWarning = nullabilityInfo?.isForWarningOnly == true
        )
    }

    private fun computeNullabilityInfoInTheAbsenceOfExplicitAnnotation(
        nullabilityFromBoundsForTypeBasedOnTypeParameter: NullabilityQualifierWithMigrationStatus?,
        defaultTypeQualifier: JavaDefaultQualifiers?,
        typeParameterForArgument: TypeParameterDescriptor?
    ): NullabilityQualifierWithMigrationStatus? {

        val result =
            nullabilityFromBoundsForTypeBasedOnTypeParameter
                ?: defaultTypeQualifier?.nullabilityQualifier?.let { nullabilityQualifierWithMigrationStatus ->
                    NullabilityQualifierWithMigrationStatus(
                        nullabilityQualifierWithMigrationStatus.qualifier,
                        nullabilityQualifierWithMigrationStatus.isForWarningOnly
                    )
                }

        val boundsFromTypeParameterForArgument = typeParameterForArgument?.boundsNullability() ?: return result

        if (defaultTypeQualifier == null && result == null && boundsFromTypeParameterForArgument.qualifier == NullabilityQualifier.NULLABLE) {
            return NullabilityQualifierWithMigrationStatus(
                NullabilityQualifier.FORCE_FLEXIBILITY,
                boundsFromTypeParameterForArgument.isForWarningOnly
            )
        }

        if (result == null) return boundsFromTypeParameterForArgument

        return mostSpecific(boundsFromTypeParameterForArgument, result)
    }

    private fun mostSpecific(
        a: NullabilityQualifierWithMigrationStatus,
        b: NullabilityQualifierWithMigrationStatus
    ): NullabilityQualifierWithMigrationStatus {
        if (a.qualifier == NullabilityQualifier.FORCE_FLEXIBILITY) return b
        if (b.qualifier == NullabilityQualifier.FORCE_FLEXIBILITY) return a
        if (a.qualifier == NullabilityQualifier.NULLABLE) return b
        if (b.qualifier == NullabilityQualifier.NULLABLE) return a
        assert(a.qualifier == b.qualifier && a.qualifier == NullabilityQualifier.NOT_NULL) {
            "Expected everything is NOT_NULL, but $a and $b are found"
        }

        return NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL)
    }

    private fun KotlinType.nullabilityInfoBoundsForTypeParameterUsage(): Pair<NullabilityQualifierWithMigrationStatus?, Boolean> {
        val typeParameterBoundsNullability =
            (constructor.declarationDescriptor as? TypeParameterDescriptor)?.boundsNullability() ?: return Pair(null, false)

        // If type parameter has a nullable (non-flexible) upper bound
        // We shouldn't mark its type usages as nullable:
        // interface A<T extends @Nullable Object> {
        //      void foo(T t); // should be loaded as "fun foo(t: T)" but not as "fun foo(t: T?)"
        // }
        return Pair(
            NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL, typeParameterBoundsNullability.isForWarningOnly),
            typeParameterBoundsNullability.qualifier == NullabilityQualifier.NOT_NULL
        )
    }

    private fun TypeParameterDescriptor.boundsNullability(): NullabilityQualifierWithMigrationStatus? {
        // Do not use bounds from Kotlin-defined type parameters
        if (this !is LazyJavaTypeParameterDescriptor || upperBounds.all(KotlinType::isError)) return null

        if (upperBounds.all(KotlinType::isNullabilityFlexible)) {
            if (upperBounds.any { it is FlexibleTypeWithEnhancement && !it.enhancement.isNullable() }) {
                return NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL, isForWarningOnly = true)
            }

            if (upperBounds.any { it is FlexibleTypeWithEnhancement && it.enhancement.isNullable() }) {
                return NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE, isForWarningOnly = true)
            }

            return null
        }

        val resultingQualifier =
            if (upperBounds.any { !it.isNullable() }) NullabilityQualifier.NOT_NULL else NullabilityQualifier.NULLABLE

        return NullabilityQualifierWithMigrationStatus(resultingQualifier)
    }

    private fun computeIndexedQualifiersForOverride(predefined: TypeEnhancementInfo?): (Int) -> JavaTypeQualifiers {
        val indexedFromSupertypes = fromOverridden.map { it.toIndexed() }
        val indexedThisType = fromOverride.toIndexed()

        // The covariant case may be hard, e.g. in the superclass the return may be Super<T>, but in the subclass it may be Derived, which
        // is declared to extend Super<T>, and propagating data here is highly non-trivial, so we only look at the head type constructor
        // (outermost type), unless the type in the subclass is interchangeable with the all the types in superclasses:
        // e.g. we have (Mutable)List<String!>! in the subclass and { List<String!>, (Mutable)List<String>! } from superclasses
        // Note that `this` is flexible here, so it's equal to it's bounds
        val onlyHeadTypeConstructor = isCovariant && fromOverridden.any { !KotlinTypeChecker.DEFAULT.equalTypes(it, fromOverride) }

        val treeSize = if (onlyHeadTypeConstructor) 1 else indexedThisType.size
        val computedResult = Array(treeSize) { index ->
            val qualifiers = indexedThisType[index].extractQualifiersFromAnnotations()
            val superQualifiers = indexedFromSupertypes.mapNotNull { it.getOrNull(index)?.type?.extractQualifiers() }
            qualifiers.computeQualifiersForOverride(superQualifiers, index == 0 && isCovariant, index == 0 && isForVarargParameter)
        }
        return { index -> predefined?.map?.get(index) ?: computedResult.getOrElse(index) { JavaTypeQualifiers.NONE } }
    }

    private fun <T> T.flattenTree(result: MutableList<T>, children: (T) -> Iterable<T>?) {
        result.add(this)
        children(this)?.forEach { it.flattenTree(result, children) }
    }

    private fun <T> T.flattenTree(children: (T) -> Iterable<T>?): List<T> =
        ArrayList<T>(1).also { flattenTree(it, children) }

    private fun KotlinType.extractAndMergeDefaultQualifiers(oldQualifiers: JavaTypeQualifiersByElementType?) =
        containerContext.components.annotationTypeQualifierResolver.extractAndMergeDefaultQualifiers(oldQualifiers, annotations)

    private fun KotlinType.toIndexed(): List<TypeAndDefaultQualifiers> =
        TypeAndDefaultQualifiers(this, extractAndMergeDefaultQualifiers(containerContext.defaultTypeQualifiers), null).flattenTree {
            // Enhancement of raw type arguments may enter a loop.
            if (isSuperTypesEnhancement && it.type is RawType) return@flattenTree null

            it.type?.arguments?.zip(it.type.constructor.parameters) { arg, parameter ->
                if (arg.isStarProjection)
                    TypeAndDefaultQualifiers(null, it.defaultQualifiers, parameter)
                else
                    TypeAndDefaultQualifiers(arg.type, arg.type.extractAndMergeDefaultQualifiers(it.defaultQualifiers), parameter)
            }
        }
}

private data class TypeAndDefaultQualifiers(
    val type: KotlinType?,
    val defaultQualifiers: JavaTypeQualifiersByElementType?,
    val typeParameterForArgument: TypeParameterDescriptor?
)

private fun KotlinType.isNullabilityFlexible(): Boolean {
    val flexibility = unwrap() as? FlexibleType ?: return false
    return flexibility.lowerBound.isMarkedNullable != flexibility.upperBound.isMarkedNullable
}
