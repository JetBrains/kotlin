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

//@file:JvmName("ConstUtil")

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

object ConstUtil {
    @JvmStatic fun canBeUsedForConstVal(type: KotlinType) = type.canBeUsedForConstVal()
}

fun KotlinType.canBeUsedForConstVal() =
    (KotlinBuiltIns.isPrimitiveType(this) || UnsignedTypes.isUnsignedType(this)) && !TypeUtils.isNullableType(this) ||
            KotlinBuiltIns.isString(this)
