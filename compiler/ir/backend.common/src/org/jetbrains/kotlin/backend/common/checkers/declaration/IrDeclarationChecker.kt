/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.declaration

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression

internal fun <E : IrDeclaration> List<IrElementChecker<E>>.check(expression: E, context: CheckerContext) {
    for (checker in this) {
        checker.check(expression, context)
    }
}

internal typealias IrValueParameterChecker = IrElementChecker<IrValueParameter>
internal typealias IrFieldChecker = IrElementChecker<IrField>
internal typealias IrFunctionChecker = IrElementChecker<IrFunction>
internal typealias IrPropertyChecker = IrElementChecker<IrProperty>