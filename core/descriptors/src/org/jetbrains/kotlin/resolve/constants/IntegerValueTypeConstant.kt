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

package org.jetbrains.kotlin.resolve.constants

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.*

import java.util.Collections

public class IntegerValueTypeConstant(
        value: Number,
        canBeUsedInAnnotations: Boolean,
        usesVariableAsConstant: Boolean
) : IntegerValueConstant<Number>(value, canBeUsedInAnnotations, true, usesVariableAsConstant) {

    private val typeConstructor = IntegerValueTypeConstructor(value.toLong())

    override fun getType(kotlinBuiltIns: KotlinBuiltIns): JetType {
        return JetTypeImpl(
                Annotations.EMPTY, typeConstructor, false, emptyList<TypeProjection>()
                , ErrorUtils.createErrorScope("Scope for number value type (" + typeConstructor.toString() + ")", true)
        )
    }

    deprecated("")
    override val value: Number
        get() = throw UnsupportedOperationException("Use IntegerValueTypeConstant.getValue(expectedType) instead")

    public fun getType(expectedType: JetType): JetType = TypeUtils.getPrimitiveNumberType(typeConstructor, expectedType)

    public fun getValue(expectedType: JetType): Number {
        val numberValue = typeConstructor.getValue()
        val builtIns = KotlinBuiltIns.getInstance()

        val valueType = getType(expectedType)
        if (valueType == builtIns.getIntType()) {
            return numberValue.toInt()
        }
        else if (valueType == builtIns.getByteType()) {
            return numberValue.toByte()
        }
        else if (valueType == builtIns.getShortType()) {
            return numberValue.toShort()
        }
        else {
            return numberValue.toLong()
        }
    }

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitNumberTypeValue(this, data)

    override fun toString() = typeConstructor.toString()
}
