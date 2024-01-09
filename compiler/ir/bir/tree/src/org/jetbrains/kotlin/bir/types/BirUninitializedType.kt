/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types

import org.jetbrains.kotlin.bir.expressions.BirConstructorCall

/**
 * An instance which should be used when creating an IR element whose type cannot be determined at the moment of creation.
 *
 * Example: when translating generic functions in psi2ir, we're creating an BirFunction first, then adding BirTypeParameter instances to it,
 * and only then translating the function's return type with respect to those created type parameters.
 *
 * Instead of using this special instance, we could just make BirFunction/BirConstructor constructors allow to accept no return type,
 * however this could lead to a situation where we forget to set return type sometimes. This would result in crashes at unexpected moments,
 * especially in Kotlin/JS where function return types are not present in the resulting binary files.
 */
data object BirUninitializedType : BirType() {
    override val annotations: List<BirConstructorCall> = emptyList()
}