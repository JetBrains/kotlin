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

@file:JvmName("KTypes")
package kotlin.reflect.full

import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.KTypeImpl

/**
 * Returns a new type with the same classifier, arguments and annotations as the given type, and with the given nullability.
 */
@SinceKotlin("1.1")
fun KType.withNullability(nullable: Boolean): KType {
    if (isMarkedNullable) {
        return if (nullable) this else KTypeImpl(TypeUtils.makeNotNullable((this as KTypeImpl).type)) { javaType }
    }

    // If the type is not marked nullable, it's either a non-null type or a platform type.
    val kotlinType = (this as KTypeImpl).type
    if (kotlinType.isFlexible()) return KTypeImpl(TypeUtils.makeNullableAsSpecified(kotlinType, nullable)) { javaType }

    return if (!nullable) this else KTypeImpl(TypeUtils.makeNullable(kotlinType)) { javaType }
}


/**
 * Returns `true` if `this` type is the same or is a subtype of [other], `false` otherwise.
 */
@SinceKotlin("1.1")
fun KType.isSubtypeOf(other: KType): Boolean {
    return (this as KTypeImpl).type.isSubtypeOf((other as KTypeImpl).type)
}

/**
 * Returns `true` if `this` type is the same or is a supertype of [other], `false` otherwise.
 */
@SinceKotlin("1.1")
fun KType.isSupertypeOf(other: KType): Boolean {
    return other.isSubtypeOf(this)
}
