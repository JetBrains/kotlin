/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types

import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.types.impl.BirErrorClassSymbolImpl
import org.jetbrains.kotlin.bir.types.impl.BirTypeBase
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

class BirErrorType(
    kotlinType: KotlinType?,
    override val annotations: List<BirConstructorCall>,
    override val variance: Variance,
    val isMarkedNullable: Boolean = false
) : BirTypeBase(kotlinType), SimpleTypeMarker {
    val symbol: BirClassSymbol
        get() = BirErrorClassSymbolImpl

    override fun equals(other: Any?): Boolean = other is BirErrorType
    override fun hashCode(): Int = BirErrorType::class.java.hashCode()
}