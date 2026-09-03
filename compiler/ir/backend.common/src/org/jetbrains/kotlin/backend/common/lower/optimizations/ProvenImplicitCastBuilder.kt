/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.optimizations

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType

/**
 * The optimizations in this package both remove type checks and introduce `IMPLICIT_CAST`s of their own
 * (to keep the IR well-typed while narrowing the types they computed).
 *
 * A backend which checks the `IMPLICIT_CAST`s at runtime (Wasm does, see KT-87090) must be told that those casts
 * are correct by construction, or the optimizations would trade the checks they remove for new ones. Such a backend
 * overrides [irProvenImplicitCast] with whatever marking its type operator lowering understands.
 */
interface ProvenImplicitCastBuilder {
    /**
     * Builds an `IMPLICIT_CAST` of [argument] to [type] which is known to always succeed.
     *
     * The default wraps it into an [org.jetbrains.kotlin.ir.expressions.IrBlock] marked with
     * [STATEMENT_ORIGIN_NO_CAST_NEEDED], which is what the Native backend understands.
     */
    fun IrBuilderWithScope.irProvenImplicitCast(argument: IrExpression, type: IrType): IrExpression =
            irBlock(origin = STATEMENT_ORIGIN_NO_CAST_NEEDED) {
                +irImplicitCast(argument, type)
            }
}
