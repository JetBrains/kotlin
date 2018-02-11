/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.descriptors

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaStaticClassScope
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.firstArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ValueParameterData(val type: KotlinType, val hasDefaultValue: Boolean)

fun copyValueParameters(
        newValueParametersTypes: Collection<ValueParameterData>,
        oldValueParameters: Collection<ValueParameterDescriptor>,
        newOwner: CallableDescriptor
): List<ValueParameterDescriptor> {
    assert(newValueParametersTypes.size == oldValueParameters.size) {
        "Different value parameters sizes: Enhanced = ${newValueParametersTypes.size}, Old = ${oldValueParameters.size}"
    }

    return newValueParametersTypes.zip(oldValueParameters).map { (newParameter, oldParameter) ->
        ValueParameterDescriptorImpl(
                newOwner,
                null,
                oldParameter.index,
                oldParameter.annotations,
                oldParameter.name,
                newParameter.type,
                newParameter.hasDefaultValue,
                oldParameter.isCrossinline,
                oldParameter.isNoinline,
                if (oldParameter.varargElementType != null) newOwner.module.builtIns.getArrayElementType(newParameter.type) else null,
                oldParameter.source
        )
    }
}

fun ClassDescriptor.getParentJavaStaticClassScope(): LazyJavaStaticClassScope? {
    val superClassDescriptor = getSuperClassNotAny() ?: return null

    val staticScope = superClassDescriptor.staticScope

    if (staticScope !is LazyJavaStaticClassScope) return superClassDescriptor.getParentJavaStaticClassScope()

    return staticScope
}

fun DeserializedMemberDescriptor.getImplClassNameForDeserialized(): JvmClassName? =
        (containerSource as? JvmPackagePartSource)?.className

fun DeserializedMemberDescriptor.isFromJvmPackagePart(): Boolean =
        containerSource is JvmPackagePartSource

fun ValueParameterDescriptor.getParameterNameAnnotation(): AnnotationDescriptor? {
    val annotation = annotations.findAnnotation(JvmAnnotationNames.PARAMETER_NAME_FQ_NAME) ?: return null
    if (annotation.firstArgument()?.safeAs<StringValue>()?.value?.isEmpty() != false) {
        return null
    }

    return annotation
}

sealed class AnnotationDefaultValue
class StringDefaultValue(val value: String) : AnnotationDefaultValue()
object NullDefaultValue : AnnotationDefaultValue()

fun ValueParameterDescriptor.getDefaultValueFromAnnotation(): AnnotationDefaultValue? {
    annotations.findAnnotation(JvmAnnotationNames.DEFAULT_VALUE_FQ_NAME)
            ?.firstArgument()
            ?.safeAs<StringValue>()?.value
            ?.let { return StringDefaultValue(it) }

    if (annotations.hasAnnotation(JvmAnnotationNames.DEFAULT_NULL_FQ_NAME)) {
        return NullDefaultValue
    }

    return null
}
