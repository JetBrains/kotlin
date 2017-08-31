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

package org.jetbrains.kotlin.backend.js.expression

import org.jetbrains.kotlin.backend.js.context.IrTranslationContext
import org.jetbrains.kotlin.backend.js.util.buildJs
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isArray
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsInvocation
import org.jetbrains.kotlin.js.backend.ast.metadata.TypeCheck
import org.jetbrains.kotlin.js.backend.ast.metadata.typeCheck
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.js.patterns.NamePredicate
import org.jetbrains.kotlin.js.patterns.typePredicates.CHAR_SEQUENCE
import org.jetbrains.kotlin.js.patterns.typePredicates.COMPARABLE
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

fun IrTranslationContext.translateIsType(type: KotlinType): JsExpression {
    val result = doTranslateIsType(type)!!
    return if (type.isMarkedNullable) {
        buildJs { "Kotlin".dotPure("orNull").invoke(result).pure().withMetadata(TypeCheck.OR_NULL) }
    }
    else {
        result
    }
}

fun IrTranslationContext.doTranslateIsType(type: KotlinType): JsExpression? {
    val typeParameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(type)
    if (typeParameterDescriptor != null) {
        if (typeParameterDescriptor.isReified) {
            return getIsTypeCheckCallableForReifiedType(typeParameterDescriptor)
        }

        var result: JsExpression? = null
        for (upperBound in typeParameterDescriptor.upperBounds) {
            val next = doTranslateIsType(upperBound)
            if (next != null) {
                result = if (result != null) {
                    buildJs { "Kotlin".dotPure("andPredicate").invoke(next).pure().withMetadata(TypeCheck.AND_PREDICATE) }
                }
                else {
                    next
                }
            }
        }
        return result
    }

    if (type.isFunctionTypeOrSubtype && !ReflectionTypes.isNumberedKPropertyOrKMutablePropertyType(type)) {
        return isTypeOf("function")
    }

    if (isArray(type)) {
        return if (config.isTypedArraysEnabled) {
            buildJs { "Kotlin".dotPure("isArray").invoke() }
        }
        else {
            buildJs { "Array".dotPure("isArray").invoke() }
        }
    }

    if (CHAR_SEQUENCE.test(type)) return buildJs { "Kotlin".dotPure("isCharSequence") }
    if (COMPARABLE.test(type)) return buildJs { "Kotlin".dotPure("isComparable") }

    val typeName = type.nameIfStandardType

    if (NamePredicate.STRING.test(typeName)) {
        return isTypeOf("string")
    }

    if (NamePredicate.BOOLEAN.test(typeName)) {
        return isTypeOf("boolean")
    }

    if (NamePredicate.LONG.test(typeName)) {
        return isInstanceOf(buildJs { "Kotlin".dotPure("Long") })
    }

    if (NamePredicate.NUMBER.test(typeName)) {
        return buildJs { "Kotlin".dotPure("isNumber") }
    }

    if (NamePredicate.CHAR.test(typeName)) {
        return buildJs { "Kotlin".dotPure("isChar") }
    }

    if (NamePredicate.PRIMITIVE_NUMBERS_MAPPED_TO_PRIMITIVE_JS.test(typeName)) {
        return isTypeOf("number")
    }

    if (config.isTypedArraysEnabled && KotlinBuiltIns.isPrimitiveArray(type)) {
        val arrayType = KotlinBuiltIns.getPrimitiveArrayElementType(type)!!
        return buildJs { "Kotlin".dotPure("is" + arrayType.arrayTypeName) }
    }

    val referencedClass = DescriptorUtils.getClassDescriptorForType(type)
    return isInstanceOf(translateAsTypeReference(referencedClass))
}

private fun IrTranslationContext.getIsTypeCheckCallableForReifiedType(typeParameter: TypeParameterDescriptor): JsExpression {
    assert(typeParameter.isReified) { "Expected reified type, actual: " + typeParameter }
    val containingDeclaration = typeParameter.containingDeclaration
    assert(containingDeclaration is CallableDescriptor) {
        "Expected type parameter $typeParameter to be contained in CallableDescriptor, actual: ${containingDeclaration.javaClass}"
    }

    return aliases[typeParameter]!!
}

private fun isTypeOf(name: String): JsExpression =
        buildJs { "Kotlin".dotPure("isTypeOf").invoke(name.str()).withMetadata(TypeCheck.TYPEOF) }

private fun isInstanceOf(typeRef: JsExpression): JsExpression =
        buildJs { "Kotlin".dotPure("isInstanceOf").invoke(typeRef).withMetadata(TypeCheck.INSTANCEOF) }

private fun JsInvocation.withMetadata(metadata: TypeCheck): JsExpression = apply { typeCheck = metadata }