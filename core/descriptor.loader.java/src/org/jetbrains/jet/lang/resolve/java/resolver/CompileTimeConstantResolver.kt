/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.resolver

import org.jetbrains.jet.lang.resolve.constants.*
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

fun resolveCompileTimeConstantValue(value: Any?, canBeUsedInAnnotations: Boolean, expectedType: JetType?): CompileTimeConstant<*>? {
    return when (value) {
        is String -> StringValue(value, canBeUsedInAnnotations)
        is Byte -> ByteValue(value, canBeUsedInAnnotations, false)
        is Short -> ShortValue(value, canBeUsedInAnnotations, false)
        is Char -> CharValue(value, canBeUsedInAnnotations, false)
        is Int -> {
            val builtIns = KotlinBuiltIns.getInstance()
            when (expectedType) {
                builtIns.getShortType() -> ShortValue(value.toShort(), canBeUsedInAnnotations, false)
                builtIns.getByteType() -> ByteValue(value.toByte(), canBeUsedInAnnotations, false)
                builtIns.getCharType() ->  CharValue(value.toChar(), canBeUsedInAnnotations, false)
                else -> IntValue(value, canBeUsedInAnnotations, false)
            }
        }
        is Long -> LongValue(value, canBeUsedInAnnotations, false)
        is Float -> FloatValue(value, canBeUsedInAnnotations)
        is Double -> DoubleValue(value, canBeUsedInAnnotations)
        is Boolean -> BooleanValue(value, canBeUsedInAnnotations)
        null -> NullValue.NULL
        else -> null
    }
}