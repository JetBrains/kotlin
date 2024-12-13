/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.ensureTypeIs
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideMaybeAbstract

internal object IrCallTypeChecker : IrCallChecker {
    override fun check(
        expression: IrCall,
        context: CheckerContext,
    ) {
        val callee = expression.symbol.owner
        // TODO: We don't have the proper type substitution yet, so skip generics for now.
        val actualCallee = callee.resolveFakeOverrideMaybeAbstract { it.returnType.classifierOrNull !is IrTypeParameterSymbol } ?: callee
        val returnType = actualCallee.returnType
        if (returnType is IrSimpleType &&
            returnType.classifier is IrClassSymbol &&
            returnType.arguments.isEmpty()
        ) {
            expression.ensureTypeIs(callee.returnType, context)
        }
    }

}