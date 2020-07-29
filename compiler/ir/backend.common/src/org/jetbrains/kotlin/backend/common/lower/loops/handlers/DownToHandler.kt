/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.backend.common.lower.matchers.SimpleCalleeMatcher
import org.jetbrains.kotlin.backend.common.lower.matchers.singleArgumentExtension
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.FqName

/** Builds a [HeaderInfo] for progressions built using the `downTo` extension function. */
internal class DownToHandler(private val context: CommonBackendContext, private val progressionElementTypes: Collection<IrType>) :
    ProgressionHandler {

    override val matcher = SimpleCalleeMatcher {
        singleArgumentExtension(FqName("kotlin.ranges.downTo"), progressionElementTypes)
        parameterCount { it == 1 }
        parameter(0) { it.type in progressionElementTypes }
    }

    override fun build(expression: IrCall, data: ProgressionType, scopeOwner: IrSymbol): HeaderInfo? =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            ProgressionHeaderInfo(
                data,
                first = expression.extensionReceiver!!,
                last = expression.getValueArgument(0)!!,
                step = irInt(-1),
                direction = ProgressionDirection.DECREASING
            )
        }
}