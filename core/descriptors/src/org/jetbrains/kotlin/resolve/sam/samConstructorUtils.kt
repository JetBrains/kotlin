/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.sam

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations.Companion.EMPTY
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import java.util.*

fun createSamConstructorFunction(
    owner: DeclarationDescriptor,
    samInterface: ClassDescriptor,
    samResolver: SamConversionResolver,
    samConversionOracle: SamConversionOracle
): SamConstructorDescriptor {
    assert(getSingleAbstractMethodOrNull(samInterface) != null) { samInterface }

    val result = SamConstructorDescriptorImpl(owner, samInterface)
    val samTypeParameters = samInterface.typeConstructor.parameters
    val unsubstitutedSamType = samInterface.defaultType

    initializeSamConstructorDescriptor(
        samInterface,
        result,
        samTypeParameters,
        unsubstitutedSamType,
        samResolver,
        samConversionOracle
    )

    return result
}

fun createTypeAliasSamConstructorFunction(
    typeAliasDescriptor: TypeAliasDescriptor,
    underlyingSamConstructor: SamConstructorDescriptor,
    samResolver: SamConversionResolver,
    samConversionOracle: SamConversionOracle
): SamConstructorDescriptor? {
    val result = SamTypeAliasConstructorDescriptorImpl(typeAliasDescriptor, underlyingSamConstructor)
    val samInterface = underlyingSamConstructor.baseDescriptorForSynthetic
    val samTypeParameters = typeAliasDescriptor.typeConstructor.parameters
    val unsubstitutedSamType = typeAliasDescriptor.expandedType

    initializeSamConstructorDescriptor(
        samInterface,
        result,
        samTypeParameters,
        unsubstitutedSamType,
        samResolver,
        samConversionOracle
    )

    return result
}

private fun initializeSamConstructorDescriptor(
    samInterface: ClassDescriptor,
    samConstructor: SimpleFunctionDescriptorImpl,
    samTypeParameters: List<TypeParameterDescriptor>,
    unsubstitutedSamType: KotlinType,
    samResolver: SamConversionResolver,
    samConversionOracle: SamConversionOracle
) {
    val typeParameters = recreateAndInitializeTypeParameters(samTypeParameters, samConstructor)
    val parameterTypeUnsubstituted = getFunctionTypeForSamType(unsubstitutedSamType, samResolver, samConversionOracle)
        ?: error("couldn't get function type for SAM type $unsubstitutedSamType")

    val parameterType =
        typeParameters.substitutor.substitute(parameterTypeUnsubstituted, Variance.IN_VARIANCE) ?: error(
            "couldn't substitute type: " + parameterTypeUnsubstituted +
                    ", substitutor = " + typeParameters.substitutor
        )

    val parameter = ValueParameterDescriptorImpl(
        samConstructor, null, 0, EMPTY, Name.identifier("function"),
        parameterType,
        declaresDefaultValue = false,
        isCrossinline = false,
        isNoinline = false,
        varargElementType = null,
        source = SourceElement.NO_SOURCE
    )

    val returnType =
        typeParameters.substitutor.substitute(unsubstitutedSamType, Variance.OUT_VARIANCE) ?: error(
            "couldn't substitute type: " + unsubstitutedSamType +
                    ", substitutor = " + typeParameters.substitutor
        )

    samConstructor.initialize(
        null,
        null,
        emptyList(),
        typeParameters.descriptors, listOf(parameter),
        returnType,
        Modality.FINAL,
        samInterface.visibility
    )
}

fun recreateAndInitializeTypeParameters(
    originalParameters: List<TypeParameterDescriptor>,
    newOwner: DeclarationDescriptor?
): SamConstructorTypeParameters {
    val interfaceToFunTypeParameters = recreateTypeParametersAndReturnMapping(originalParameters, newOwner)

    val typeParametersSubstitutor = createSubstitutorForTypeParameters(interfaceToFunTypeParameters)

    for ((interfaceTypeParameter, funTypeParameter) in interfaceToFunTypeParameters) {
        for (upperBound in interfaceTypeParameter.upperBounds) {
            val upperBoundSubstituted =
                typeParametersSubstitutor.substitute(upperBound, Variance.INVARIANT)
                    ?: error("couldn't substitute type: $upperBound, substitutor = $typeParametersSubstitutor")
            funTypeParameter.addUpperBound(upperBoundSubstituted)
        }
        funTypeParameter.setInitialized()
    }
    return SamConstructorTypeParameters(interfaceToFunTypeParameters.values.toList(), typeParametersSubstitutor)
}

fun recreateTypeParametersAndReturnMapping(
    originalParameters: List<TypeParameterDescriptor>,
    newOwner: DeclarationDescriptor?
): Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> =
    originalParameters.associateWith { typeParameter ->
        TypeParameterDescriptorImpl.createForFurtherModification(
            newOwner ?: typeParameter.containingDeclaration,
            typeParameter.annotations,
            typeParameter.isReified,
            typeParameter.variance,
            typeParameter.name,
            typeParameter.index,
            SourceElement.NO_SOURCE,
            typeParameter.storageManager
        )
    }

fun createSubstitutorForTypeParameters(
    originalToAltTypeParameters: Map<TypeParameterDescriptor, TypeParameterDescriptorImpl>
): TypeSubstitutor {
    val typeSubstitutionContext =
        originalToAltTypeParameters
            .map { (key, value) -> key.typeConstructor to value.defaultType.asTypeProjection() }
            .toMap()

    // TODO: Use IndexedParametersSubstitution here instead of map creation
    return TypeSubstitutor.create(typeSubstitutionContext)
}

class SamConstructorTypeParameters(val descriptors: List<TypeParameterDescriptor>, val substitutor: TypeSubstitutor)