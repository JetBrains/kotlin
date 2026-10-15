/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.irCastIfNeeded
import org.jetbrains.kotlin.ir.util.shallowCopy

/** Builds a [HeaderInfo] for progressions not handled by more specialized handlers. */
internal class DefaultProgressionHandler(
    private val context: CommonBackendContext, private val allowUnsignedBounds: Boolean = false
) : HeaderInfoHandler<IrExpression, Nothing?> {
    private val irBuiltIns = context.irBuiltIns
    private val symbols = context.symbols
    private val rangeClassesTypes = symbols.rangeClasses.map { it.defaultType }.toSet()

    // Function inliner often erases type of range in for loop expression, like
    //   val temp: Iterable<Any> = IntRange(...)
    //   for (i in temp) { ... }
    // Type `Iterable<Any>` would prevent the optimization, so initial type should be taken (IntRange in the example above),
    // which would give `Int` as loop variable type, since IntRange implements Iterable<Int>
    override fun matchIterable(expression: IrExpression): Boolean = TODO()

    override fun build(expression: IrExpression, data: Nothing?, scopeOwner: IrSymbol): HeaderInfo = TODO()
}
