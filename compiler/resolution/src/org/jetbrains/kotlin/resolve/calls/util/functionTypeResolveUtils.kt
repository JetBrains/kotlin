/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.getFunctionTypeArgumentProjections
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeProjection

fun createValueParametersForInvokeInFunctionType(
        functionDescriptor: FunctionDescriptor, parameterTypes: List<TypeProjection>
): List<ValueParameterDescriptor> {
    return parameterTypes.mapIndexed { i, typeProjection ->
        ValueParameterDescriptorImpl(
                functionDescriptor, null, i, Annotations.EMPTY,
                Name.identifier("p${i + 1}"), typeProjection.type,
                /* declaresDefaultValue = */ false,
                /* isCrossinline = */ false,
                /* isNoinline = */ false,
                /* isCoroutine = */ false,
                null, SourceElement.NO_SOURCE
        )
    }
}

fun createFunctionType(
        builtIns: KotlinBuiltIns,
        annotations: Annotations,
        receiverType: KotlinType?,
        parameterTypes: List<KotlinType>,
        parameterNames: List<Name>?,
        returnType: KotlinType
): SimpleType {
    val arguments = getFunctionTypeArgumentProjections(receiverType, parameterTypes, returnType)
    val size = parameterTypes.size
    val classDescriptor = builtIns.getFunction(if (receiverType == null) size else size + 1)

    val typeAnnotations =
            if (receiverType == null || annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.extensionFunctionType) != null) {
                annotations
            }
            else {
                val extensionFunctionAnnotation = AnnotationDescriptorImpl(
                        builtIns.getBuiltInClassByName(KotlinBuiltIns.FQ_NAMES.extensionFunctionType.shortName()).defaultType,
                        emptyMap(), SourceElement.NO_SOURCE
                )

                // TODO: preserve laziness of given annotations
                AnnotationsImpl(annotations + extensionFunctionAnnotation)
            }

    return KotlinTypeFactory.functionType(typeAnnotations, classDescriptor, arguments, parameterNames)
}

fun getValueParametersCountFromFunctionType(type: KotlinType): Int {
    assert(type.isFunctionType) { "Not a function type: $type" }
    // Function type arguments = receiver? + parameters + return-type
    return type.arguments.size - (if (type.isExtensionFunctionType) 1 else 0) - 1
}
