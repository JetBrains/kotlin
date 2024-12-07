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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.load.java.descriptors.PossiblyExternalAnnotationDescriptor
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
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.RawType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.getEnhancement
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.types.typeUtil.contains

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
                enhanceValueParameter(
                    parameterDescriptor = (annotationOwnerForMember as? FunctionDescriptor)
                        ?.getUserData(JavaMethodDescriptor.ORIGINAL_VALUE_PARAMETER_FOR_EXTENSION_RECEIVER),
                    methodContext = memberContext,
                    predefined = null,
                    ignoreDeclarationNullabilityAnnotations = false
                ) { it.extensionReceiverParameter!!.type }
            else null

        val predefinedEnhancementInfo =
            (this as? JavaMethodDescriptor)
                ?.run { SignatureBuildingComponents.signature(this.containingDeclaration as ClassDescriptor, this.computeJvmDescriptor()) }
                ?.let { signature ->
                    PREDEFINED_FUNCTION_ENHANCEMENT_INFO_BY_SIGNATURE[signature]?.let {
                        check(it.errorsSinceLanguageVersion == null || it.errorsSinceLanguageVersion?.startsWith("2.") == true)
                        // We only change behavior for predefined nullability with warnings for versions >= 2.0
                        if (it.errorsSinceLanguageVersion == null) it else it.warningModeClone
                    }
                }


        predefinedEnhancementInfo?.let {
            assert(it.parametersInfo.size == valueParameters.size) {
                "Predefined enhancement info for $this has ${it.parametersInfo.size}, but ${valueParameters.size} expected"
            }
        }

        val ignoreDeclarationNullabilityAnnotations =
            (isJspecifyEnabledInStrictMode(c.components.javaTypeEnhancementState)
                    || memberContext.components.settings.ignoreNullabilityForErasedValueParameters)
                    && hasErasedValueParameters(this)
        val valueParameterEnhancements = annotationOwnerForMember.valueParameters.map { p ->
            val predefined = predefinedEnhancementInfo?.parametersInfo?.getOrNull(p.index)
            enhanceValueParameter(p, memberContext, predefined, ignoreDeclarationNullabilityAnnotations) {
                it.valueParameters[p.index].type
            }
        }

        val returnTypeEnhancement =
            enhance(
                typeContainer = annotationOwnerForMember, isCovariant = true,
                containerContext = memberContext,
                containerApplicabilityType =
                if ((this as? PropertyDescriptor)?.isJavaField == true)
                    AnnotationQualifierApplicabilityType.FIELD
                else
                    AnnotationQualifierApplicabilityType.METHOD_RETURN_TYPE,
                predefinedEnhancementInfo?.returnTypeInfo
            ) { it.returnType!! }

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
                valueParameterEnhancements.mapIndexed { index, enhanced -> enhanced ?: valueParameters[index].type },
                returnTypeEnhancement ?: returnType!!,
                additionalUserData
            ) as D
        }

        return this
    }

    fun enhanceTypeParameterBounds(
        typeParameter: TypeParameterDescriptor,
        bounds: List<KotlinType>,
        context: LazyJavaResolverContext,
    ): List<KotlinType> {
        return bounds.map { bound ->
            // TODO: would not enhancing raw type arguments be sufficient?
            if (bound.contains { it is RawType }) return@map bound

            SignatureParts(typeParameter, false, context, AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS)
                .enhance(bound, emptyList()) ?: bound
        }
    }

    /*
     * This method should be only used for type enhancement of base classes' type arguments:
     *      class A extends B<@NotNull Integer> {}
     */
    fun enhanceSuperType(type: KotlinType, context: LazyJavaResolverContext) =
        SignatureParts(
            typeContainer = null, isCovariant = false,
            context, AnnotationQualifierApplicabilityType.TYPE_USE, skipRawTypeArguments = true
        ).enhance(type, emptyList()) ?: type

    private fun KotlinType.containsFunctionN(): Boolean =
        TypeUtils.contains(this) {
            val classifier = it.constructor.declarationDescriptor ?: return@contains false
            classifier.name == JavaToKotlinClassMap.FUNCTION_N_FQ_NAME.shortName() &&
                    classifier.fqNameOrNull() == JavaToKotlinClassMap.FUNCTION_N_FQ_NAME
        }

    private fun CallableMemberDescriptor.enhanceValueParameter(
        // TODO: investigate if it's really can be a null (check properties' with extension overrides in Java)
        parameterDescriptor: ValueParameterDescriptor?,
        methodContext: LazyJavaResolverContext,
        predefined: TypeEnhancementInfo?,
        ignoreDeclarationNullabilityAnnotations: Boolean,
        collector: (CallableMemberDescriptor) -> KotlinType,
    ) = enhance(
        parameterDescriptor, false,
        parameterDescriptor?.let { methodContext.copyWithNewDefaultTypeQualifiers(it.annotations) } ?: methodContext,
        AnnotationQualifierApplicabilityType.VALUE_PARAMETER,
        predefined, ignoreDeclarationNullabilityAnnotations, collector
    )

    private fun CallableMemberDescriptor.enhance(
        typeContainer: Annotated?,
        isCovariant: Boolean,
        containerContext: LazyJavaResolverContext,
        containerApplicabilityType: AnnotationQualifierApplicabilityType,
        predefined: TypeEnhancementInfo?,
        ignoreDeclarationNullabilityAnnotations: Boolean = false,
        collector: (CallableMemberDescriptor) -> KotlinType,
    ): KotlinType? {
        return SignatureParts(typeContainer, isCovariant, containerContext, containerApplicabilityType)
            .enhance(collector(this), overriddenDescriptors.map { collector(it) }, predefined, ignoreDeclarationNullabilityAnnotations)
    }

    private fun SignatureParts.enhance(
        type: KotlinType,
        overrides: List<KotlinType>,
        predefined: TypeEnhancementInfo? = null,
        ignoreDeclarationNullabilityAnnotations: Boolean = false,
    ) = with(typeEnhancement) {
        type.enhance(type.computeIndexedQualifiers(overrides, predefined, ignoreDeclarationNullabilityAnnotations), skipRawTypeArguments)
    }
}

private class SignatureParts(
    private val typeContainer: Annotated?,
    override val isCovariant: Boolean,
    private val containerContext: LazyJavaResolverContext,
    override val containerApplicabilityType: AnnotationQualifierApplicabilityType,
    override val skipRawTypeArguments: Boolean = false,
) : AbstractSignatureParts<AnnotationDescriptor>() {
    override val annotationTypeQualifierResolver: AnnotationTypeQualifierResolver
        get() = containerContext.components.annotationTypeQualifierResolver

    override val enableImprovementsInStrictMode: Boolean
        get() = containerContext.components.settings.typeEnhancementImprovementsInStrictMode

    override val containerAnnotations: Iterable<AnnotationDescriptor>
        get() = typeContainer?.annotations ?: emptyList()

    override val containerDefaultTypeQualifiers: JavaTypeQualifiersByElementType?
        get() = containerContext.defaultTypeQualifiers

    override val containerIsVarargParameter: Boolean
        get() = typeContainer is ValueParameterDescriptor && typeContainer.varargElementType != null

    override val typeSystem: TypeSystemInferenceExtensionContext
        get() = SimpleClassicTypeSystemContext

    override fun AnnotationDescriptor.forceWarning(unenhancedType: KotlinTypeMarker?): Boolean =
        (this is PossiblyExternalAnnotationDescriptor && isIdeExternalAnnotation) ||
                (this is LazyJavaAnnotationDescriptor && !enableImprovementsInStrictMode &&
                        (isFreshlySupportedTypeUseAnnotation ||
                                containerApplicabilityType == AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS)) ||
                // Previously, type use annotations on primitive arrays were lost, so temporarily treat them as warnings.
                (unenhancedType != null && KotlinBuiltIns.isPrimitiveArray(unenhancedType as KotlinType) &&
                        annotationTypeQualifierResolver.isTypeUseAnnotation(this) &&
                        !containerContext.components.settings.enhancePrimitiveArrays)

    override val KotlinTypeMarker.annotations: Iterable<AnnotationDescriptor>
        get() = (this as KotlinType).annotations

    override val KotlinTypeMarker.enhancedForWarnings: KotlinType?
        get() = (this as KotlinType).getEnhancement()

    override val KotlinTypeMarker.fqNameUnsafe: FqNameUnsafe?
        get() = TypeUtils.getClassDescriptor(this as KotlinType)?.let { DescriptorUtils.getFqName(it) }

    override val KotlinTypeMarker.isNotNullTypeParameterCompat: Boolean
        get() = (this as KotlinType).unwrap() is NotNullTypeParameterImpl

    override fun KotlinTypeMarker.isEqual(other: KotlinTypeMarker): Boolean =
        containerContext.components.kotlinTypeChecker.equalTypes(this as KotlinType, other as KotlinType)

    override fun KotlinTypeMarker.isArrayOrPrimitiveArray(): Boolean = KotlinBuiltIns.isArrayOrPrimitiveArray(this as KotlinType)

    override val TypeParameterMarker.isFromJava: Boolean
        get() = this is LazyJavaTypeParameterDescriptor

    override fun getDefaultNullability(
        referencedParameterBoundsNullability: NullabilityQualifierWithMigrationStatus?,
        defaultTypeQualifiers: JavaDefaultQualifiers?,
    ): NullabilityQualifierWithMigrationStatus? {
        return referencedParameterBoundsNullability?.copy(qualifier = NullabilityQualifier.NOT_NULL)
            ?: defaultTypeQualifiers?.nullabilityQualifier
    }
}
