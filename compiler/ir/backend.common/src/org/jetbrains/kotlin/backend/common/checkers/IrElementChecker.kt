/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrExpression
import kotlin.reflect.KClass

abstract class IrElementChecker<in E : IrElement>(
    elementClass: KClass<in E>,
) {
    abstract fun check(element: E, context: CheckerContext)
}

internal fun <E : IrElement> List<IrElementChecker<E>>.check(element: E, context: CheckerContext) {
    for (checker in this) {
        checker.check(element, context)
    }
}
