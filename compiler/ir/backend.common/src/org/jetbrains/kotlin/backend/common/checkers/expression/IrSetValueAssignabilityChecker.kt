/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrSetValue

object IrSetValueAssignabilityChecker : IrElementChecker<IrSetValue>(IrSetValue::class) {
    override fun check(element: IrSetValue, context: CheckerContext) {
        val declaration = element.symbol.owner
        if (declaration is IrValueParameter && !declaration.isAssignable) {
            context.error(element, "Assignment to value parameters not marked assignable")
        }
    }
}