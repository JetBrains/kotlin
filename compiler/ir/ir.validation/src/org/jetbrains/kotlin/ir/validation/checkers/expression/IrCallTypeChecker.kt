/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.expression

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideMaybeAbstract
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.validation.checkers.ensureTypeIs

object IrCallTypeChecker : IrElementChecker<IrCall>(IrCall::class) {
    override fun check(element: IrCall, context: CheckerContext) {
        val callee = element.symbol.owner
        // TODO: We don't have the proper type substitution yet, so skip generics for now.
        val actualCallee = callee.resolveFakeOverrideMaybeAbstract {
            it.isFakeOverride || it.returnType.classifierOrNull !is IrTypeParameterSymbol
        } ?: callee
        val returnType = actualCallee.returnType
        if (returnType is IrSimpleType &&
            returnType.classifier is IrClassSymbol &&
            returnType.arguments.isEmpty()
        ) {
            element.ensureTypeIs(callee.returnType, context)
        }
    }
}
