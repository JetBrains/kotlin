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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

fun IrTranslationContext.translateIsType(type: KotlinType): JsExpression {
    val result = doTranslateIsType(type)!!
    return if (type.isMarkedNullable) {
        buildJs { "Kotlin".dotPure("orNull").invoke(result).pure() }
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
                result = if (result != null) buildJs { "Kotlin".dotPure("andPredicate").invoke(next).pure() } else next
            }
        }
        return result
    }

    val referencedClass = DescriptorUtils.getClassDescriptorForType(type)
    val typeName = translateAsTypeReference(referencedClass)
    return buildJs { "Kotlin".dotPure("isInstanceOf").invoke(typeName).pure() }
}

private fun IrTranslationContext.getIsTypeCheckCallableForReifiedType(typeParameter: TypeParameterDescriptor): JsExpression {
    assert(typeParameter.isReified) { "Expected reified type, actual: " + typeParameter }
    val containingDeclaration = typeParameter.containingDeclaration
    assert(containingDeclaration is CallableDescriptor) {
        "Expected type parameter $typeParameter to be contained in CallableDescriptor, actual: ${containingDeclaration.javaClass}"
    }

    return aliases[typeParameter]!!
}