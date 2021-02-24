/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.types.typeUtil.contains

fun substituteAlternativesInPublicType(type: KotlinType): UnwrappedType {
    val substitutor = object : NewTypeSubstitutor {
        override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? {
            if (constructor is IntersectionTypeConstructor) {
                constructor.getAlternativeType()?.let { alternative ->
                    return safeSubstitute(alternative.unwrap())
                }
            }

            return null
        }

        override val isEmpty: Boolean by lazy {
            !type.contains { it.constructor is IntersectionTypeConstructor }
        }
    }

    return substitutor.safeSubstitute(type.unwrap())
}
