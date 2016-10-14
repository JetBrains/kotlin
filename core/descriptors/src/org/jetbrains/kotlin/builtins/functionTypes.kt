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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValueFactory
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.util.*

val KotlinType.isFunctionTypeOrSubtype: Boolean
    get() = isFunctionType || DFS.dfsFromNode(
            this,
            DFS.Neighbors { it.constructor.supertypes },
            DFS.VisitedWithSet(),
            object : DFS.AbstractNodeHandler<KotlinType, Boolean>() {
                private var result = false

                override fun beforeChildren(current: KotlinType): Boolean {
                    if (current.isFunctionType) {
                        result = true
                    }
                    return !result
                }

                override fun result() = result
            }
    )

val KotlinType.isFunctionType: Boolean
    get() {
        val descriptor = constructor.declarationDescriptor
        return descriptor != null && isNumberedFunctionClassFqName(descriptor.fqNameUnsafe)
    }

val KotlinType.isNonExtensionFunctionType: Boolean
    get() = isFunctionType && !isTypeAnnotatedWithExtensionFunctionType

val KotlinType.isExtensionFunctionType: Boolean
    get() = isFunctionType && isTypeAnnotatedWithExtensionFunctionType

private val KotlinType.isTypeAnnotatedWithExtensionFunctionType: Boolean
    get() = annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.extensionFunctionType) != null

/**
 * @return true if this is an FQ name of a fictitious class representing the function type,
 * e.g. kotlin.Function1 (but NOT kotlin.reflect.KFunction1)
 */
fun isNumberedFunctionClassFqName(fqName: FqNameUnsafe): Boolean {
    if (!fqName.startsWith(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME)) return false

    val segments = fqName.pathSegments()
    if (segments.size != 2) return false

    val shortName = segments.last().asString()
    return BuiltInFictitiousFunctionClassFactory.isFunctionClassName(shortName, KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME)
}

fun KotlinType.getReceiverTypeFromFunctionType(): KotlinType? {
    assert(isFunctionType) { "Not a function type: ${this}" }
    return if (isTypeAnnotatedWithExtensionFunctionType) arguments.first().type else null
}

fun KotlinType.getReturnTypeFromFunctionType(): KotlinType {
    assert(isFunctionType) { "Not a function type: ${this}" }
    return arguments.last().type
}

fun KotlinType.getValueParameterTypesFromFunctionType(): List<TypeProjection> {
    assert(isFunctionType) { "Not a function type: ${this}" }
    val arguments = arguments
    val first = if (isExtensionFunctionType) 1 else 0
    val last = arguments.size - 1
    assert(first <= last) { "Not an exact function type: ${this}" }
    return arguments.subList(first, last)
}

fun KotlinType.extractParameterNameFromFunctionTypeArgument(): Name? {
    val annotation = annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.parameterName) ?: return null
    val name = (annotation.allValueArguments.values.singleOrNull() as? StringValue)
                       ?.value
                       ?.check { Name.isValidIdentifier(it) }
               ?: return null
    return Name.identifier(name)
}

fun getFunctionTypeArgumentProjections(
        receiverType: KotlinType?,
        parameterTypes: List<KotlinType>,
        parameterNames: List<Name>?,
        returnType: KotlinType,
        builtIns: KotlinBuiltIns
): List<TypeProjection> {
    val arguments = ArrayList<TypeProjection>(parameterTypes.size + (if (receiverType != null) 1 else 0) + 1)

    arguments.addIfNotNull(receiverType?.asTypeProjection())

    parameterTypes.mapIndexedTo(arguments) { index, type ->
        val name = parameterNames?.get(index)?.check { !it.isSpecial }
        val typeToUse = if (name != null) {
            val annotationClass = builtIns.getBuiltInClassByName(KotlinBuiltIns.FQ_NAMES.parameterName.shortName())
            val nameValue = ConstantValueFactory(builtIns).createStringValue(name.asString())
            val parameterNameAnnotation = AnnotationDescriptorImpl(
                    annotationClass.defaultType,
                    mapOf(annotationClass.unsubstitutedPrimaryConstructor!!.valueParameters.single() to nameValue),
                    org.jetbrains.kotlin.descriptors.SourceElement.NO_SOURCE
            )
            type.replaceAnnotations(AnnotationsImpl(type.annotations + parameterNameAnnotation))
        }
        else {
            type
        }
        typeToUse.asTypeProjection()
    }

    arguments.add(returnType.asTypeProjection())

    return arguments
}
