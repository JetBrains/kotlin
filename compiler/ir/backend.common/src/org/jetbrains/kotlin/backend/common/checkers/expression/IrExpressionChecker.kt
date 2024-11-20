/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

internal interface IrExpressionChecker<in E : IrExpression> {
    fun check(expression: E, context: CheckerContext)
}

internal fun <E : IrExpression> List<IrExpressionChecker<E>>.check(expression: E, context: CheckerContext) {
    for (checker in this) {
        checker.check(expression, context)
    }
}

internal typealias IrVarargChecker = IrExpressionChecker<IrVararg>
internal typealias IrFieldAccessChecker = IrExpressionChecker<IrFieldAccessExpression>
internal typealias IrDeclarationReferenceChecker = IrExpressionChecker<IrDeclarationReference>
internal typealias IrValueAccessChecker = IrExpressionChecker<IrValueAccessExpression>
internal typealias IrFunctionAccessChecker = IrExpressionChecker<IrFunctionAccessExpression>
internal typealias IrFunctionReferenceChecker = IrExpressionChecker<IrFunctionReference>
internal typealias IrMemberAccessChecker = IrExpressionChecker<IrMemberAccessExpression<IrFunctionSymbol>>