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

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.builtins.functions.BuiltInFictitiousFunctionClassFactory
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.*
import java.util.*

val KotlinType.isFunctionOrExtensionFunctionType: Boolean
    get() = isFunctionType || isExtensionFunctionType

val KotlinType.isFunctionType: Boolean
    get() = isExactFunctionType || constructor.supertypes.any(KotlinType::isFunctionType)

val KotlinType.isExtensionFunctionType: Boolean
    get() = isExactExtensionFunctionType || constructor.supertypes.any(KotlinType::isExtensionFunctionType)

val KotlinType.isExactFunctionOrExtensionFunctionType: Boolean
    get() {
        val descriptor = constructor.declarationDescriptor
        return descriptor != null && isNumberedFunctionClassFqName(descriptor.fqNameUnsafe)
    }

val KotlinType.isExactFunctionType: Boolean
    get() = isExactFunctionOrExtensionFunctionType && !isTypeAnnotatedWithExtension

val KotlinType.isExactExtensionFunctionType: Boolean
    get() = isExactFunctionOrExtensionFunctionType && isTypeAnnotatedWithExtension

private val KotlinType.isTypeAnnotatedWithExtension: Boolean
    get() = annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.extensionFunctionType) != null

/**
 * @return true if this is an FQ name of a fictitious class representing the function type,
 * e.g. kotlin.Function1 (but NOT kotlin.reflect.KFunction1)
 */
fun isNumberedFunctionClassFqName(fqName: FqNameUnsafe): Boolean {
    val segments = fqName.pathSegments()
    if (segments.size != 2) return false

    if (KotlinBuiltIns.BUILT_INS_PACKAGE_NAME != segments.first()) return false

    val shortName = segments.last().asString()
    return BuiltInFictitiousFunctionClassFactory.isFunctionClassName(shortName, KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME)
}

fun getReceiverTypeFromFunctionType(type: KotlinType): KotlinType? {
    assert(type.isFunctionOrExtensionFunctionType) { type }
    if (type.isExtensionFunctionType) {
        // TODO: this is incorrect when a class extends from an extension function and swaps type arguments
        return type.arguments[0].type
    }
    return null
}

fun getValueParametersFromFunctionType(functionDescriptor: FunctionDescriptor, type: KotlinType): List<ValueParameterDescriptor> {
    assert(type.isFunctionOrExtensionFunctionType) { type }
    return getParameterTypeProjectionsFromFunctionType(type).mapIndexed { i, typeProjection ->
        ValueParameterDescriptorImpl(
                functionDescriptor, null, i, Annotations.EMPTY,
                Name.identifier("p${i + 1}"), typeProjection.type,
                /* declaresDefaultValue = */ false,
                /* isCrossinline = */ false,
                /* isNoinline = */ false,
                null, SourceElement.NO_SOURCE
        )
    }
}

fun getReturnTypeFromFunctionType(type: KotlinType): KotlinType {
    assert(type.isFunctionOrExtensionFunctionType) { type }
    return type.arguments.last().type
}

fun getParameterTypeProjectionsFromFunctionType(type: KotlinType): List<TypeProjection> {
    assert(type.isFunctionOrExtensionFunctionType) { type }
    val arguments = type.arguments
    val first = if (type.isExtensionFunctionType) 1 else 0
    val last = arguments.size - 2
    // TODO: fix bugs associated with this here and in neighboring methods, see KT-9820
    assert(first <= last + 1) { "Not an exact function type: $type" }
    val parameterTypes = ArrayList<TypeProjection>(last - first + 1)
    for (i in first..last) {
        parameterTypes.add(arguments[i])
    }
    return parameterTypes
}

fun createFunctionType(
        builtIns: KotlinBuiltIns,
        annotations: Annotations,
        receiverType: KotlinType?,
        parameterTypes: List<KotlinType>,
        returnType: KotlinType
): KotlinType {
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

    return KotlinTypeImpl.create(typeAnnotations, classDescriptor, false, arguments)
}

internal fun getFunctionTypeArgumentProjections(
        receiverType: KotlinType?,
        parameterTypes: List<KotlinType>,
        returnType: KotlinType
): List<TypeProjection> {
    fun KotlinType.defaultProjection() = TypeProjectionImpl(Variance.INVARIANT, this)

    val arguments = ArrayList<TypeProjection>(parameterTypes.size + (if (receiverType != null) 1 else 0) + 1)
    receiverType?.let { arguments.add(it.defaultProjection()) }
    parameterTypes.mapTo(arguments, KotlinType::defaultProjection)
    arguments.add(returnType.defaultProjection())
    return arguments
}
