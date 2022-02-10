/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.IrDefinitelyNotNullType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

class IrDefinitelyNotNullTypeImpl(
    kotlinType: KotlinType?,
    override val original: IrType,
) : IrDefinitelyNotNullType(kotlinType) {

    override val annotations: List<IrConstructorCall>
        get() = original.annotations

    override val variance: Variance
        get() = Variance.INVARIANT

    override fun equals(other: Any?): Boolean =
        other is IrDefinitelyNotNullType &&
                this.original == other.original

    override fun hashCode(): Int =
        original.hashCode()
}