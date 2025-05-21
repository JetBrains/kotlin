/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.CallInlinerStrategy
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.getCompilerMessageLocation
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.file

class NativeCallInlinerStrategy(val context: LoweringContext, val symbols: KonanSymbols) : CallInlinerStrategy {
    private lateinit var builder: NativeRuntimeReflectionIrBuilder
    override fun at(container: IrDeclaration, expression: IrExpression) {
        builder = symbols.irBuiltIns.createIrBuilder(container.symbol, expression.startOffset, expression.endOffset)
            .toNativeRuntimeReflectionBuilder(symbols) { message ->
                this@NativeCallInlinerStrategy.context.reportCompilationError(message, expression.getCompilerMessageLocation(container.file))
            }
    }

    override fun postProcessTypeOf(expression: IrCall, nonSubstitutedTypeArgument: IrType): IrExpression {
        return builder.irKType(nonSubstitutedTypeArgument)
    }
}

