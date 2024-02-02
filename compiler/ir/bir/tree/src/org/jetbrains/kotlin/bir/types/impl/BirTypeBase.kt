/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types.impl

import org.jetbrains.kotlin.bir.declarations.BirFunction
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.types.BirTypeProjection
import org.jetbrains.kotlin.types.KotlinType

abstract class BirTypeBase(val kotlinType: KotlinType?) : BirType(), BirTypeProjection {
    override val type: BirType get() = this
}

class ReturnTypeIsNotInitializedException(function: BirFunction) : IllegalStateException(
    "Return type is not initialized for function '${function.name}'"
)