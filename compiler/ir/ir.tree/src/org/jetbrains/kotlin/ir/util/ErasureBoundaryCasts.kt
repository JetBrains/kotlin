/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.irFlag

/**
 * Set on an `IMPLICIT_CAST` whose argument is a value produced at an *erased* generic type (a type parameter,
 * or its erased upper bound) and which narrows it back to the substituted type the frontend saw, e.g.
 *
 * - the result of a call to a generic function or property getter (`box.value` where `value: T`),
 * - an argument bound to a lambda parameter when the lambda is called through an erased `FunctionN.invoke`,
 * - an argument or return value of an inline function whose non-reified type parameters were erased by the inliner.
 *
 * Such casts are exactly the places where heap pollution caused by an unchecked cast (`x as List<Foo>`) becomes
 * observable, so they are the casts which a backend has to check at runtime to get JVM-like semantics
 * (the JVM gets this from `checkcast` on the erased representation type).
 *
 * `IMPLICIT_CAST`s without this flag are either proven by the frontend (smart casts) or introduced by lowerings
 * just to keep the IR well-typed, and need no runtime check.
 *
 * Only a backend which checks implicit casts at runtime needs to look at this flag. Currently, that's Wasm.
 *
 * Note: like any IR attribute, the flag is not serialized into klibs. So a lowering which runs before serialization and
 * produces such casts has to express them differently (see `LoweringContext.checkErasureBoundaryCastsInInliner`); the
 * flag can still be set on the resulting checked `CAST` to recognize it within the same compilation stage.
 */
var IrTypeOperatorCall.crossesErasureBoundary: Boolean by irFlag(copyByDefault = true)

/**
 * Marks [this] as crossing an erasure boundary (see [crossesErasureBoundary]) if it is an `IMPLICIT_CAST` or a `CAST`,
 * leaves any other expression intact.
 */
fun <E : IrExpression> E.markAsErasureBoundaryCast(): E = apply {
    if (this is IrTypeOperatorCall && (operator == IrTypeOperator.IMPLICIT_CAST || operator == IrTypeOperator.CAST)) {
        crossesErasureBoundary = true
    }
}
