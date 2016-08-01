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

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.upperIfFlexible
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*


class CallableReferenceResolver(
        val reflectionTypes: ReflectionTypes,
        val argumentsToParametersMapper: ArgumentsToParametersMapper
) {

    fun resolvePropertyReference(
            descriptor: PropertyDescriptor,
            propertyReference: ChosenCallableReferenceDescriptor,
            outerCall: KotlinCall,
            scopeOwnerDescriptor: DeclarationDescriptor
    ): ResolvedPropertyReference {
        val mutable = descriptor.isVar && run {
            val setter = descriptor.setter
            setter == null || Visibilities.isVisible(propertyReference.candidate.dispatchReceiver?.receiverValue, setter, scopeOwnerDescriptor)
        }

        val reflectionType = reflectionTypes.getKPropertyType(Annotations.EMPTY, listOfNotNull(propertyReference.dispatchNotBoundReceiver,
                                                              propertyReference.extensionNotBoundReceiver), descriptor.type.unwrap(), mutable)

        return ResolvedPropertyReference(outerCall, propertyReference, reflectionType)
    }

    private fun createFakeArgumentsAndMapArguments(
            functionReference: ChosenCallableReferenceDescriptor,
            argumentCount: Int?
    ): Pair<List<UnwrappedType>, ArgumentsToParametersMapper.ArgumentMapping?> {
        if (argumentCount == null) {
            return functionReference.candidate.descriptor.valueParameters.map { it.varargElementType?.unwrap() ?: it.type.unwrap() } to null
        }

        val fakeArguments = (0..(argumentCount - 1)).map { FakeKotlinCallArgumentForCallableReference(functionReference, it) }

        val argumentsToParametersMapping = argumentsToParametersMapper.mapArguments(fakeArguments, null, functionReference.candidate.descriptor)
        val parameters = Array<UnwrappedType?>(argumentCount) { null }

        for ((parameter, resolvedArgument) in argumentsToParametersMapping.parameterToCallArgumentMap) {
            for (argument in resolvedArgument.arguments) {
                val index = (argument as FakeKotlinCallArgumentForCallableReference).index
                parameters[index] = parameter.type.unwrap()
            }
        }

        return parameters.map { it ?: ErrorUtils.createErrorType("Wrong parameters mapping") } to argumentsToParametersMapping
    }

    fun resolveFunctionReference(
            functionReference: ChosenCallableReferenceDescriptor,
            outerCall: KotlinCall,
            expectedType: UnwrappedType
    ): ResolvedFunctionReference {
        val functionType =
                if (expectedType.isFunctionType) {
                    expectedType
                }
                else if (ReflectionTypes.isNumberedKFunction(expectedType)) {
                    expectedType.immediateSupertypes().first { it.isFunctionType }
                }
                else {
                    null
                }

        val parameterTypes = ArrayList<UnwrappedType>(functionType?.arguments?.size ?: 2)
        parameterTypes.addIfNotNull(functionReference.dispatchNotBoundReceiver)
        parameterTypes.addIfNotNull(functionReference.extensionNotBoundReceiver)

        // (A, B, C) -> Int if A -- receiver, then B & C -- parameters
        // here parameterTypes contains only receivers, all parameters will be added later
        val argumentCount = functionType?.arguments?.let { it.size - parameterTypes.size - 1 }?.takeIf { it >= 0 }
        val (parameters, mapping) = createFakeArgumentsAndMapArguments(functionReference, argumentCount)
        parameterTypes.addAll(parameters)

        val unitExpectedType = functionType?.let(KotlinType::getReturnTypeFromFunctionType)?.takeIf { it.upperIfFlexible().isUnit() }
        // coercion to unit
        val returnType = unitExpectedType ?: functionReference.candidate.descriptor.returnType
                         ?: ErrorUtils.createErrorType("Error return type")

        val kFunctionType = reflectionTypes.getKFunctionType(Annotations.EMPTY, null, parameterTypes, null, returnType, expectedType.builtIns)

        return ResolvedFunctionReference(outerCall, functionReference, kFunctionType, mapping)
    }
}

