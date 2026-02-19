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

import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypePreparator
import org.jetbrains.kotlin.types.AbstractTypeRefiner
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.types.AbstractKType
import kotlin.reflect.jvm.internal.types.DescriptorKType
import kotlin.reflect.jvm.internal.types.ReflectTypeSystemContext
import kotlin.reflect.jvm.internal.useK1Implementation

/**
 * Returns a new type with the same classifier, arguments and annotations as the given type, and with the given nullability.
 */
@SinceKotlin("1.1")
fun KType.withNullability(nullable: Boolean): KType {
    return (this as AbstractKType).makeNullableAsSpecified(nullable)
}


/**
 * Returns `true` if `this` type is the same or is a subtype of [other], `false` otherwise.
 */
@SinceKotlin("1.1")
fun KType.isSubtypeOf(other: KType): Boolean {
    if (useK1Implementation) {
        return (this as DescriptorKType).type.isSubtypeOf((other as DescriptorKType).type)
    }
    val state = TypeCheckerState(
        isErrorTypeEqualsToAnything = false,
        isStubTypeEqualsToAnything = false,
        isDnnTypesEqualToFlexible = false,
        allowedTypeVariable = false,
        typeSystemContext = ReflectTypeSystemContext,
        kotlinTypePreparator = AbstractTypePreparator.Default,
        kotlinTypeRefiner = AbstractTypeRefiner.Default,
    )
    return AbstractTypeChecker.isSubtypeOf(state, this as AbstractKType, other as AbstractKType)
}

/**
 * Returns `true` if `this` type is the same or is a supertype of [other], `false` otherwise.
 */
@SinceKotlin("1.1")
fun KType.isSupertypeOf(other: KType): Boolean {
    return other.isSubtypeOf(this)
}
