/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.declaration

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.validateVararg
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrVararg

/**
 * Makes sure that: [IrVararg.type] is an array of [IrVararg.varargElementType].
 */
object IrValueParameterVarargTypesChecker : IrElementChecker<IrValueParameter>(IrValueParameter::class) {
    override fun check(
        element: IrValueParameter,
        context: CheckerContext
    ) {
        element.varargElementType?.let {
            validateVararg(element, element.type, it, context)
        }
    }
}
