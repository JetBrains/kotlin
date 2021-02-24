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
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.composeAnnotations
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.descriptors.*
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.copyWithNewDefaultTypeQualifiers
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaAnnotationDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaTypeParameterDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.isJavaField
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.load.kotlin.signature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.deprecation.DEPRECATED_FUNCTION_KEY
import org.jetbrains.kotlin.resolve.descriptorUtil.firstArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.utils.JavaTypeEnhancementState
import org.jetbrains.kotlin.utils.ReportLevel
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class SignatureEnhancement(
    private val annotationTypeQualifierResolver: AnnotationTypeQualifierResolver,
    private val javaTypeEnhancementState: JavaTypeEnhancementState,
    private val typeEnhancement: JavaTypeEnhancement
) {

    private fun AnnotationDescriptor.extractNullabilityTypeFromArgument(isForWarningOnly: Boolean): NullabilityQualifierWithMigrationStatus? {
        val enumValue = firstArgument() as? EnumValue
        // if no argument is specified, use default value: NOT_NULL
            ?: return NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL, isForWarningOnly)

        return when (enumValue.enumEntryName.asString()) {
            "ALWAYS" -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL, isForWarningOnly)
            "MAYBE", "NEVER" -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE, isForWarningOnly)
            "UNKNOWN" -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.FORCE_FLEXIBILITY, isForWarningOnly)
            else -> null
        }
    }

    fun extractNullability(
        annotationDescriptor: AnnotationDescriptor,
        areImprovementsEnabled: Boolean,
        typeParameterBounds: Boolean
    ): NullabilityQualifierWithMigrationStatus? {
        extractNullabilityFromKnownAnnotations(annotationDescriptor, areImprovementsEnabled, typeParameterBounds)?.let { return it }

        val typeQualifierAnnotation =
            annotationTypeQualifierResolver.resolveTypeQualifierAnnotation(annotationDescriptor)
                ?: return null

        val jsr305State = annotationTypeQualifierResolver.resolveJsr305AnnotationState(annotationDescriptor)
        if (jsr305State.isIgnore) return null

        return extractNullabilityFromKnownAnnotations(typeQualifierAnnotation, areImprovementsEnabled, typeParameterBounds)
            ?.copy(isForWarningOnly = jsr305State.isWarning)
    }

    private fun extractNullabilityFromKnownAnnotations(
        annotationDescriptor: AnnotationDescriptor,
        areImprovementsEnabled: Boolean,
        typeParameterBounds: Boolean
    ): NullabilityQualifierWithMigrationStatus? {
        val annotationFqName = annotationDescriptor.fqName ?: return null
        val isForWarningOnly = annotationDescriptor is LazyJavaAnnotationDescriptor
                && (annotationDescriptor.isFreshlySupportedTypeUseAnnotation || typeParameterBounds)
                && !areImprovementsEnabled

        val migrationStatus = jspecifyMigrationStatus(annotationFqName)
            ?: commonMigrationStatus(annotationFqName, annotationDescriptor, isForWarningOnly)
            ?: return null

        return if (!migrationStatus.isForWarningOnly
            && annotationDescriptor is PossiblyExternalAnnotationDescriptor
            && annotationDescriptor.isIdeExternalAnnotation
        ) {
            migrationStatus.copy(isForWarningOnly = true)
        } else migrationStatus
    }

    private fun jspecifyMigrationStatus(
        annotationFqName: FqName
    ): NullabilityQualifierWithMigrationStatus? {
        if (javaTypeEnhancementState.jspecifyReportLevel == ReportLevel.IGNORE) return null
        val isForWarningOnly = javaTypeEnhancementState.jspecifyReportLevel == ReportLevel.WARN
        return when (annotationFqName) {
            JSPECIFY_NULLABLE -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE, isForWarningOnly)
            JSPECIFY_NULLNESS_UNKNOWN ->
                NullabilityQualifierWithMigrationStatus(NullabilityQualifier.FORCE_FLEXIBILITY, isForWarningOnly)
            else -> null
        }
    }

    private fun commonMigrationStatus(
        annotationFqName: FqName,
        annotationDescriptor: AnnotationDescriptor,
        isForWarningOnly: Boolean = false
    ): NullabilityQualifierWithMigrationStatus? = when {
        annotationFqName in NULLABLE_ANNOTATIONS -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE, isForWarningOnly)
        annotationFqName in NOT_NULL_ANNOTATIONS -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL, isForWarningOnly)
        annotationFqName == JAVAX_NONNULL_ANNOTATION -> annotationDescriptor.extractNullabilityTypeFromArgument(isForWarningOnly)

        annotationFqName == COMPATQUAL_NULLABLE_ANNOTATION && javaTypeEnhancementState.enableCompatqualCheckerFrameworkAnnotations ->
            NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE, isForWarningOnly)

        annotationFqName == COMPATQUAL_NONNULL_ANNOTATION && javaTypeEnhancementState.enableCompatqualCheckerFrameworkAnnotations ->
            NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL, isForWarningOnly)

        annotationFqName == ANDROIDX_RECENTLY_NON_NULL_ANNOTATION -> NullabilityQualifierWithMigrationStatus(
            NullabilityQualifier.NOT_NULL,
            isForWarningOnly = true
        )

        annotationFqName == ANDROIDX_RECENTLY_NULLABLE_ANNOTATION -> NullabilityQualifierWithMigrationStatus(
            NullabilityQualifier.NULLABLE,
            isForWarningOnly = true
        )
        else -> null
    }

    fun <D : CallableMemberDescriptor> enhanceSignatures(c: LazyJavaResolverContext, platformSignatures: Collection<D>): Collection<D> {
        return platformSignatures.map {
            it.enhanceSignature(c)
        }
    }

    private fun <D : CallableMemberDescriptor> D.enhanceSignature(c: LazyJavaResolverContext): D {
        // TODO type parameters
        // TODO use new type parameters while enhancing other types
        // TODO Propagation into generic type arguments

        if (this !is JavaCallableMemberDescriptor) return this

        // Fake overrides with one overridden has been enhanced before
        if (kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE && original.overriddenDescriptors.size == 1) return this

        val memberContext = c.copyWithNewDefaultTypeQualifiers(annotations)

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

        val containsFunctionN = receiverTypeEnhancement?.containsFunctionN == true || returnTypeEnhancement.containsFunctionN ||
                valueParameterEnhancements.any { it.containsFunctionN }

        if ((receiverTypeEnhancement?.wereChanges == true)
            || returnTypeEnhancement.wereChanges || valueParameterEnhancements.any { it.wereChanges } || containsFunctionN
        ) {
            val additionalUserData =
                if (containsFunctionN)
                    DEPRECATED_FUNCTION_KEY to DeprecationCausedByFunctionN(this)
                else
                    null

            @Suppress("UNCHECKED_CAST")
            return this.enhance(
                receiverTypeEnhancement?.type,
                valueParameterEnhancements.map { ValueParameterData(it.type, false) },
                returnTypeEnhancement.type,
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
            if (bound.contains { it is RawType }) return@map bound

            SignatureParts(
                typeParameter, bound, emptyList(), false, context,
                AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS,
                typeParameterBounds = true
            ).enhance().type
        }
    }

    /*
     * This method should be only used for type enhancement of base classes' type arguments:
     *      class A extends B<@NotNull Integer> {}
     */
    fun enhanceSuperType(type: KotlinType, context: LazyJavaResolverContext) =
        SignatureParts(null, type, emptyList(), false, context, AnnotationQualifierApplicabilityType.TYPE_USE).enhance().type

    private inner class SignatureParts(
        private val typeContainer: Annotated?,
        private val fromOverride: KotlinType,
        private val fromOverridden: Collection<KotlinType>,
        private val isCovariant: Boolean,
        private val containerContext: LazyJavaResolverContext,
        private val containerApplicabilityType: AnnotationQualifierApplicabilityType,
        private val typeParameterBounds: Boolean = false
    ) {

        private val isForVarargParameter get() = typeContainer.safeAs<ValueParameterDescriptor>()?.varargElementType != null

        fun enhance(predefined: TypeEnhancementInfo? = null): PartEnhancementResult {
            val qualifiers = computeIndexedQualifiersForOverride()

            val qualifiersWithPredefined: ((Int) -> JavaTypeQualifiers)? = predefined?.let {
                { index ->
                    predefined.map[index] ?: qualifiers(index)
                }
            }

            val containsFunctionN = TypeUtils.contains(fromOverride) {
                val classifier = it.constructor.declarationDescriptor ?: return@contains false

                classifier.name == JavaToKotlinClassMap.FUNCTION_N_FQ_NAME.shortName() &&
                        classifier.fqNameOrNull() == JavaToKotlinClassMap.FUNCTION_N_FQ_NAME
            }

            return with(typeEnhancement) {
                fromOverride.enhance(qualifiersWithPredefined ?: qualifiers)?.let { enhanced ->
                    PartEnhancementResult(enhanced, wereChanges = true, containsFunctionN = containsFunctionN)
                } ?: PartEnhancementResult(fromOverride, wereChanges = false, containsFunctionN = containsFunctionN)
            }
        }

        private fun KotlinType.extractQualifiers(): JavaTypeQualifiers {
            val (lower, upper) =
                if (this.isFlexible())
                    asFlexibleType().let { Pair(it.lowerBound, it.upperBound) }
                else Pair(this, this)

            val mapper = JavaToKotlinClassMapper
            return JavaTypeQualifiers(
                when {
                    lower.isMarkedNullable -> NullabilityQualifier.NULLABLE
                    !upper.isMarkedNullable -> NullabilityQualifier.NOT_NULL
                    else -> null
                },
                when {
                    mapper.isReadOnly(lower) -> MutabilityQualifier.READ_ONLY
                    mapper.isMutable(upper) -> MutabilityQualifier.MUTABLE
                    else -> null
                },
                isNotNullTypeParameter = unwrap() is NotNullTypeParameter
            )
        }

        private fun KotlinType.extractQualifiersFromAnnotations(
            isHeadTypeConstructor: Boolean,
            defaultQualifiersForType: JavaDefaultQualifiers?,
            typeParameterForArgument: TypeParameterDescriptor?,
            isFromStarProjection: Boolean
        ): JavaTypeQualifiers {
            val areImprovementsEnabled = containerContext.components.settings.typeEnhancementImprovements

            val composedAnnotation =
                if (isHeadTypeConstructor && typeContainer != null && typeContainer !is TypeParameterDescriptor && areImprovementsEnabled) {
                    val filteredContainerAnnotations = typeContainer.annotations.filter {
                        val (_, targets) = annotationTypeQualifierResolver.resolveAnnotation(it) ?: return@filter false
                        /*
                         * We don't apply container type use annotations to avoid double applying them like with arrays:
                         *      @NotNull Integer [] f15();
                         * Otherwise, in the example above we would apply `@NotNull` to `Integer` (i.e. array element; as TYPE_USE annotation)
                         * and to entire array (as METHOD annotation).
                         * In other words, we prefer TYPE_USE target of an annotation, and apply the annotation only according to it, if it's present.
                         * See KT-24392 for more details.
                         */
                        AnnotationQualifierApplicabilityType.TYPE_USE !in targets
                    }
                    composeAnnotations(Annotations.create(filteredContainerAnnotations), annotations)
                } else if (isHeadTypeConstructor && typeContainer != null) {
                    composeAnnotations(typeContainer.annotations, annotations)
                } else annotations

            fun <T : Any> List<FqName>.ifPresent(qualifier: T) =
                if (any { composedAnnotation.findAnnotation(it) != null }) qualifier else null

            fun <T : Any> uniqueNotNull(x: T?, y: T?) = if (x == null || y == null || x == y) x ?: y else null

            val defaultTypeQualifier =
                (if (isHeadTypeConstructor)
                    containerContext.defaultTypeQualifiers?.get(containerApplicabilityType)
                else
                    defaultQualifiersForType)?.takeIf {
                    it.affectsTypeParameterBasedTypes || !isTypeParameter()
                }

            val (nullabilityFromBoundsForTypeBasedOnTypeParameter, isTypeParameterWithNotNullableBounds) =
                nullabilityInfoBoundsForTypeParameterUsage()

            val annotationsNullability = composedAnnotation.extractNullability(areImprovementsEnabled, typeParameterBounds)
                ?.takeUnless { isFromStarProjection }
            val nullabilityInfo =
                annotationsNullability
                    ?: computeNullabilityInfoInTheAbsenceOfExplicitAnnotation(
                        nullabilityFromBoundsForTypeBasedOnTypeParameter,
                        defaultTypeQualifier,
                        typeParameterForArgument
                    )

            val isNotNullTypeParameter =
                if (annotationsNullability != null)
                    annotationsNullability.qualifier == NullabilityQualifier.NOT_NULL
                else
                    isTypeParameterWithNotNullableBounds || defaultTypeQualifier?.makesTypeParameterNotNull == true

            return JavaTypeQualifiers(
                nullabilityInfo?.qualifier,
                uniqueNotNull(
                    READ_ONLY_ANNOTATIONS.ifPresent(
                        MutabilityQualifier.READ_ONLY
                    ),
                    MUTABLE_ANNOTATIONS.ifPresent(
                        MutabilityQualifier.MUTABLE
                    )
                ),
                isNotNullTypeParameter = isNotNullTypeParameter && isTypeParameter(),
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
            if (result == null) return NullabilityQualifierWithMigrationStatus(boundsFromTypeParameterForArgument)

            return NullabilityQualifierWithMigrationStatus(
                mostSpecific(boundsFromTypeParameterForArgument, result.qualifier)
            )
        }

        private fun mostSpecific(a: NullabilityQualifier, b: NullabilityQualifier): NullabilityQualifier {
            if (a == NullabilityQualifier.FORCE_FLEXIBILITY) return b
            if (b == NullabilityQualifier.FORCE_FLEXIBILITY) return a
            if (a == NullabilityQualifier.NULLABLE) return b
            if (b == NullabilityQualifier.NULLABLE) return a
            assert(a == b && a == NullabilityQualifier.NOT_NULL) {
                "Expected everything is NOT_NULL, but $a and $b are found"
            }

            return NullabilityQualifier.NOT_NULL
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
                NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL),
                typeParameterBoundsNullability == NullabilityQualifier.NOT_NULL
            )
        }

        private fun TypeParameterDescriptor.boundsNullability(): NullabilityQualifier? {
            // Do not use bounds from Kotlin-defined type parameters
            if (this !is LazyJavaTypeParameterDescriptor) return null
            return when {
                upperBounds.all(KotlinType::isError) -> null
                upperBounds.all(KotlinType::isNullabilityFlexible) -> null
                upperBounds.any { !it.isNullable() } -> NullabilityQualifier.NOT_NULL
                else -> NullabilityQualifier.NULLABLE
            }
        }

        private fun Annotations.extractNullability(
            areImprovementsEnabled: Boolean,
            typeParameterBounds: Boolean
        ): NullabilityQualifierWithMigrationStatus? =
            this.firstNotNullResult { extractNullability(it, areImprovementsEnabled, typeParameterBounds) }

        private fun computeIndexedQualifiersForOverride(): (Int) -> JavaTypeQualifiers {

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
                val isHeadTypeConstructor = index == 0
                assert(isHeadTypeConstructor || !onlyHeadTypeConstructor) { "Only head type constructors should be computed" }

                val (qualifiers, defaultQualifiers, typeParameterForArgument, isFromStarProjection) = indexedThisType[index]
                val verticalSlice = indexedFromSupertypes.mapNotNull { it.getOrNull(index)?.type }

                // Only the head type constructor is safely co-variant
                qualifiers.computeQualifiersForOverride(
                    verticalSlice, defaultQualifiers, isHeadTypeConstructor, typeParameterForArgument, isFromStarProjection
                )
            }

            return { index -> computedResult.getOrElse(index) { JavaTypeQualifiers.NONE } }
        }


        private fun KotlinType.toIndexed(): List<TypeAndDefaultQualifiers> {
            val list = ArrayList<TypeAndDefaultQualifiers>(1)

            fun add(type: KotlinType, ownerContext: LazyJavaResolverContext, typeParameterForArgument: TypeParameterDescriptor?) {
                val c = ownerContext.copyWithNewDefaultTypeQualifiers(type.annotations)

                val defaultQualifiers = c.defaultTypeQualifiers
                    ?.get(
                        if (typeParameterBounds)
                            AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS
                        else
                            AnnotationQualifierApplicabilityType.TYPE_USE
                    )
                list.add(
                    TypeAndDefaultQualifiers(
                        type,
                        defaultQualifiers,
                        typeParameterForArgument,
                        isFromStarProjection = false
                    )
                )

                for ((arg, parameter) in type.arguments.zip(type.constructor.parameters)) {
                    if (arg.isStarProjection) {
                        // TODO: sort out how to handle wildcards
                        list.add(TypeAndDefaultQualifiers(arg.type, defaultQualifiers, parameter, isFromStarProjection = true))
                    } else {
                        add(arg.type, c, parameter)
                    }
                }
            }

            add(this, containerContext, typeParameterForArgument = null)
            return list
        }

        private fun KotlinType.computeQualifiersForOverride(
            fromSupertypes: Collection<KotlinType>,
            defaultQualifiersForType: JavaDefaultQualifiers?,
            isHeadTypeConstructor: Boolean,
            typeParameterForArgument: TypeParameterDescriptor?,
            isFromStarProjection: Boolean
        ): JavaTypeQualifiers {
            val superQualifiers = fromSupertypes.map { it.extractQualifiers() }
            val mutabilityFromSupertypes = superQualifiers.mapNotNull { it.mutability }.toSet()
            val nullabilityFromSupertypes = superQualifiers.mapNotNull { it.nullability }.toSet()
            val nullabilityFromSupertypesWithWarning = fromSupertypes
                .mapNotNull { it.unwrapEnhancement().extractQualifiers().nullability }
                .toSet()

            val own =
                extractQualifiersFromAnnotations(
                    isHeadTypeConstructor, defaultQualifiersForType, typeParameterForArgument, isFromStarProjection
                )
            val ownNullability = own.takeIf { !it.isNullabilityQualifierForWarning }?.nullability
            val ownNullabilityForWarning = own.nullability

            val isCovariantPosition = isCovariant && isHeadTypeConstructor
            val nullability =
                nullabilityFromSupertypes.select(ownNullability, isCovariantPosition)
                    // Vararg value parameters effectively have non-nullable type in Kotlin
                    // and having nullable types in Java may lead to impossibility of overriding them in Kotlin
                    ?.takeUnless { isForVarargParameter && isHeadTypeConstructor && it == NullabilityQualifier.NULLABLE }

            val mutability =
                mutabilityFromSupertypes
                    .select(MutabilityQualifier.MUTABLE, MutabilityQualifier.READ_ONLY, own.mutability, isCovariantPosition)

            val canChange = ownNullabilityForWarning != ownNullability || nullabilityFromSupertypesWithWarning != nullabilityFromSupertypes
            val isAnyNonNullTypeParameter = own.isNotNullTypeParameter || superQualifiers.any { it.isNotNullTypeParameter }
            if (nullability == null && canChange) {
                val nullabilityWithWarning =
                    nullabilityFromSupertypesWithWarning.select(ownNullabilityForWarning, isCovariantPosition)

                return createJavaTypeQualifiers(
                    nullabilityWithWarning, mutability,
                    forWarning = true, isAnyNonNullTypeParameter = isAnyNonNullTypeParameter
                )
            }

            return createJavaTypeQualifiers(
                nullability, mutability,
                forWarning = nullability == null,
                isAnyNonNullTypeParameter = isAnyNonNullTypeParameter
            )
        }
    }

    private open class PartEnhancementResult(
        val type: KotlinType,
        val wereChanges: Boolean,
        val containsFunctionN: Boolean
    )

    private fun CallableMemberDescriptor.partsForValueParameter(
        // TODO: investigate if it's really can be a null (check properties' with extension overrides in Java)
        parameterDescriptor: ValueParameterDescriptor?,
        methodContext: LazyJavaResolverContext,
        collector: (CallableMemberDescriptor) -> KotlinType
    ) = parts(
        parameterDescriptor, false,
        parameterDescriptor?.let { methodContext.copyWithNewDefaultTypeQualifiers(it.annotations) } ?: methodContext,
        AnnotationQualifierApplicabilityType.VALUE_PARAMETER,
        collector
    )

    private fun CallableMemberDescriptor.parts(
        typeContainer: Annotated?,
        isCovariant: Boolean,
        containerContext: LazyJavaResolverContext,
        containerApplicabilityType: AnnotationQualifierApplicabilityType,
        collector: (CallableMemberDescriptor) -> KotlinType
    ): SignatureParts {
        return SignatureParts(
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

private data class TypeAndDefaultQualifiers(
    val type: KotlinType,
    val defaultQualifiers: JavaDefaultQualifiers?,
    val typeParameterForArgument: TypeParameterDescriptor?,
    val isFromStarProjection: Boolean
)

private fun KotlinType.isNullabilityFlexible(): Boolean {
    val flexibility = unwrap() as? FlexibleType ?: return false
    return flexibility.lowerBound.isMarkedNullable != flexibility.upperBound.isMarkedNullable
}
