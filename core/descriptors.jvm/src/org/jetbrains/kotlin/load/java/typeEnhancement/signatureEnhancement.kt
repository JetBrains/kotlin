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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.composeAnnotations
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.descriptors.*
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.copyWithNewDefaultTypeQualifiers
import org.jetbrains.kotlin.load.java.lazy.descriptors.isJavaField
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DEPRECATED_FUNCTION_KEY
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.firstArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.utils.Jsr305State
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class NullabilityQualifierWithMigrationStatus(
    val qualifier: NullabilityQualifier,
    val isForWarningOnly: Boolean = false
)

class SignatureEnhancement(
    private val annotationTypeQualifierResolver: AnnotationTypeQualifierResolver,
    private val jsr305State: Jsr305State
) {

    private fun AnnotationDescriptor.extractNullabilityTypeFromArgument(): NullabilityQualifierWithMigrationStatus? {
        val enumValue = firstArgument() as? EnumValue
        // if no argument is specified, use default value: NOT_NULL
            ?: return NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL)

        return when (enumValue.enumEntryName.asString()) {
            "ALWAYS" -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL)
            "MAYBE", "NEVER" -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE)
            "UNKNOWN" -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.FORCE_FLEXIBILITY)
            else -> null
        }
    }

    fun extractNullability(annotationDescriptor: AnnotationDescriptor): NullabilityQualifierWithMigrationStatus? {
        extractNullabilityFromKnownAnnotations(annotationDescriptor)?.let { return it }

        val typeQualifierAnnotation =
            annotationTypeQualifierResolver.resolveTypeQualifierAnnotation(annotationDescriptor)
                ?: return null

        val jsr305State = annotationTypeQualifierResolver.resolveJsr305AnnotationState(annotationDescriptor)
        if (jsr305State.isIgnore) return null

        return extractNullabilityFromKnownAnnotations(typeQualifierAnnotation)?.copy(isForWarningOnly = jsr305State.isWarning)
    }

    private fun extractNullabilityFromKnownAnnotations(
        annotationDescriptor: AnnotationDescriptor
    ): NullabilityQualifierWithMigrationStatus? {
        val annotationFqName = annotationDescriptor.fqName ?: return null

        return when {
            annotationFqName in NULLABLE_ANNOTATIONS -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE)
            annotationFqName in NOT_NULL_ANNOTATIONS -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL)
            annotationFqName == JAVAX_NONNULL_ANNOTATION -> annotationDescriptor.extractNullabilityTypeFromArgument()

            annotationFqName == COMPATQUAL_NULLABLE_ANNOTATION && jsr305State.enableCompatqualCheckerFrameworkAnnotations ->
                NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE)

            annotationFqName == COMPATQUAL_NONNULL_ANNOTATION && jsr305State.enableCompatqualCheckerFrameworkAnnotations ->
                NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL)

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
            val enhancementResult = partsForValueParameter(p, memberContext) { it.valueParameters[p.index].type }
                .enhance(predefinedEnhancementInfo?.parametersInfo?.getOrNull(p.index))

            val actualType = if (enhancementResult.wereChanges) enhancementResult.type else p.type
            val hasDefaultValue = p.hasDefaultValueInAnnotation(actualType)
            val wereChanges = enhancementResult.wereChanges || (hasDefaultValue != p.declaresDefaultValue())

            ValueParameterEnhancementResult(enhancementResult.type, hasDefaultValue, wereChanges, enhancementResult.containsFunctionN)
        }

        val returnTypeEnhancement =
            parts(
                typeContainer = annotationOwnerForMember, isCovariant = true,
                containerContext = memberContext,
                containerApplicabilityType =
                if (this.safeAs<PropertyDescriptor>()?.isJavaField == true)
                    AnnotationTypeQualifierResolver.QualifierApplicabilityType.FIELD
                else
                    AnnotationTypeQualifierResolver.QualifierApplicabilityType.METHOD_RETURN_TYPE
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
                valueParameterEnhancements.map { ValueParameterData(it.type, it.hasDefaultValue) },
                returnTypeEnhancement.type,
                additionalUserData
            ) as D
        }

        return this
    }

    private fun ValueParameterDescriptor.hasDefaultValueInAnnotation(type: KotlinType): Boolean {
        val defaultValue = getDefaultValueFromAnnotation()

        return when (defaultValue) {
            is StringDefaultValue -> type.lexicalCastFrom(defaultValue.value) != null
            NullDefaultValue -> TypeUtils.acceptsNullable(type)
            null -> declaresDefaultValue()
        } && overriddenDescriptors.isEmpty()
    }

    private inner class SignatureParts(
        private val typeContainer: Annotated?,
        private val fromOverride: KotlinType,
        private val fromOverridden: Collection<KotlinType>,
        private val isCovariant: Boolean,
        private val containerContext: LazyJavaResolverContext,
        private val containerApplicabilityType: AnnotationTypeQualifierResolver.QualifierApplicabilityType
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

            return fromOverride.enhance(qualifiersWithPredefined ?: qualifiers)?.let { enhanced ->
                PartEnhancementResult(enhanced, wereChanges = true, containsFunctionN = containsFunctionN)
            } ?: PartEnhancementResult(fromOverride, wereChanges = false, containsFunctionN = containsFunctionN)
        }

        private fun KotlinType.extractQualifiers(): JavaTypeQualifiers {
            val (lower, upper) =
                    if (this.isFlexible())
                        asFlexibleType().let { Pair(it.lowerBound, it.upperBound) }
                    else Pair(this, this)

            val mapping = JavaToKotlinClassMap
            return JavaTypeQualifiers(
                when {
                    lower.isMarkedNullable -> NullabilityQualifier.NULLABLE
                    !upper.isMarkedNullable -> NullabilityQualifier.NOT_NULL
                    else -> null
                },
                when {
                    mapping.isReadOnly(lower) -> MutabilityQualifier.READ_ONLY
                    mapping.isMutable(upper) -> MutabilityQualifier.MUTABLE
                    else -> null
                },
                isNotNullTypeParameter = unwrap() is NotNullTypeParameter
            )
        }

        private fun KotlinType.extractQualifiersFromAnnotations(
            isHeadTypeConstructor: Boolean,
            defaultQualifiersForType: JavaTypeQualifiers?
        ): JavaTypeQualifiers {
            val composedAnnotation =
                if (isHeadTypeConstructor && typeContainer != null)
                    composeAnnotations(typeContainer.annotations, annotations)
                else
                    annotations

            fun <T : Any> List<FqName>.ifPresent(qualifier: T) =
                if (any { composedAnnotation.findAnnotation(it) != null }) qualifier else null

            fun <T : Any> uniqueNotNull(x: T?, y: T?) = if (x == null || y == null || x == y) x ?: y else null

            val defaultTypeQualifier =
                if (isHeadTypeConstructor)
                    containerContext.defaultTypeQualifiers?.get(containerApplicabilityType)
                else
                    defaultQualifiersForType

            val nullabilityInfo =
                composedAnnotation.extractNullability()
                    ?: defaultTypeQualifier?.nullability?.let {
                        NullabilityQualifierWithMigrationStatus(
                            defaultTypeQualifier.nullability,
                            defaultTypeQualifier.isNullabilityQualifierForWarning
                        )
                    }

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
                isNotNullTypeParameter = nullabilityInfo?.qualifier == NullabilityQualifier.NOT_NULL && isTypeParameter(),
                isNullabilityQualifierForWarning = nullabilityInfo?.isForWarningOnly == true
            )
        }

        private fun Annotations.extractNullability(): NullabilityQualifierWithMigrationStatus? =
            this.firstNotNullResult(this@SignatureEnhancement::extractNullability)

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

                val (qualifiers, defaultQualifiers) = indexedThisType[index]
                val verticalSlice = indexedFromSupertypes.mapNotNull { it.getOrNull(index)?.type }

                // Only the head type constructor is safely co-variant
                qualifiers.computeQualifiersForOverride(verticalSlice, defaultQualifiers, isHeadTypeConstructor)
            }

            return { index -> computedResult.getOrElse(index) { JavaTypeQualifiers.NONE } }
        }


        private fun KotlinType.toIndexed(): List<TypeAndDefaultQualifiers> {
            val list = ArrayList<TypeAndDefaultQualifiers>(1)

            fun add(type: KotlinType, ownerContext: LazyJavaResolverContext) {
                val c = ownerContext.copyWithNewDefaultTypeQualifiers(type.annotations)

                list.add(
                    TypeAndDefaultQualifiers(
                        type,
                        c.defaultTypeQualifiers
                            ?.get(AnnotationTypeQualifierResolver.QualifierApplicabilityType.TYPE_USE)
                    )
                )

                for (arg in type.arguments) {
                    if (arg.isStarProjection) {
                        // TODO: sort out how to handle wildcards
                        list.add(TypeAndDefaultQualifiers(arg.type, null))
                    } else {
                        add(arg.type, c)
                    }
                }
            }

            add(this, containerContext)
            return list
        }

        private fun KotlinType.computeQualifiersForOverride(
            fromSupertypes: Collection<KotlinType>,
            defaultQualifiersForType: JavaTypeQualifiers?,
            isHeadTypeConstructor: Boolean
        ): JavaTypeQualifiers {
            val superQualifiers = fromSupertypes.map { it.extractQualifiers() }
            val mutabilityFromSupertypes = superQualifiers.mapNotNull { it.mutability }.toSet()
            val nullabilityFromSupertypes = superQualifiers.mapNotNull { it.nullability }.toSet()
            val nullabilityFromSupertypesWithWarning = fromSupertypes
                .mapNotNull { it.unwrapEnhancement().extractQualifiers().nullability }
                .toSet()

            val own = extractQualifiersFromAnnotations(isHeadTypeConstructor, defaultQualifiersForType)
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

    private class ValueParameterEnhancementResult(
        type: KotlinType,
        val hasDefaultValue: Boolean,
        wereChanges: Boolean,
        containsFunctionN: Boolean
    ) : PartEnhancementResult(type, wereChanges, containsFunctionN)

    private fun CallableMemberDescriptor.partsForValueParameter(
        // TODO: investigate if it's really can be a null (check properties' with extension overrides in Java)
        parameterDescriptor: ValueParameterDescriptor?,
        methodContext: LazyJavaResolverContext,
        collector: (CallableMemberDescriptor) -> KotlinType
    ) = parts(
        parameterDescriptor, false,
        parameterDescriptor?.let { methodContext.copyWithNewDefaultTypeQualifiers(it.annotations) } ?: methodContext,
        AnnotationTypeQualifierResolver.QualifierApplicabilityType.VALUE_PARAMETER,
        collector
    )

    private fun CallableMemberDescriptor.parts(
        typeContainer: Annotated?,
        isCovariant: Boolean,
        containerContext: LazyJavaResolverContext,
        containerApplicabilityType: AnnotationTypeQualifierResolver.QualifierApplicabilityType,
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

private fun createJavaTypeQualifiers(
    nullability: NullabilityQualifier?,
    mutability: MutabilityQualifier?,
    forWarning: Boolean,
    isAnyNonNullTypeParameter: Boolean
): JavaTypeQualifiers {
    if (!isAnyNonNullTypeParameter || nullability != NullabilityQualifier.NOT_NULL) {
        return JavaTypeQualifiers(nullability, mutability, false, forWarning)
    }
    return JavaTypeQualifiers(nullability, mutability, true, forWarning)
}

private fun <T : Any> Set<T>.select(low: T, high: T, own: T?, isCovariant: Boolean): T? {
    if (isCovariant) {
        val supertypeQualifier = if (low in this) low else if (high in this) high else null
        return if (supertypeQualifier == low && own == high) null else own ?: supertypeQualifier
    }

    // isInvariant
    val effectiveSet = own?.let { (this + own).toSet() } ?: this
    // if this set contains exactly one element, it is the qualifier everybody agrees upon,
    // otherwise (no qualifiers, or multiple qualifiers), there's no single such qualifier
    // and all qualifiers are discarded
    return effectiveSet.singleOrNull()
}

private fun Set<NullabilityQualifier>.select(own: NullabilityQualifier?, isCovariant: Boolean) =
    if (own == NullabilityQualifier.FORCE_FLEXIBILITY)
        NullabilityQualifier.FORCE_FLEXIBILITY
    else
        select(NullabilityQualifier.NOT_NULL, NullabilityQualifier.NULLABLE, own, isCovariant)

private data class TypeAndDefaultQualifiers(
    val type: KotlinType,
    val defaultQualifiers: JavaTypeQualifiers?
)
