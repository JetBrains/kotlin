/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.declaration

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind

object IrFunctionParametersChecker : IrElementChecker<IrFunction>(IrFunction::class) {
    override fun check(element: IrFunction, context: CheckerContext) {
        @OptIn(DeprecatedForRemovalCompilerApi::class)
        for ((i, param) in element.valueParameters.withIndex()) {
            if (param.indexInOldValueParameters != i) {
                context.error(element, "Inconsistent index (old API) of value parameter ${param.indexInOldValueParameters} != $i")
            }
        }

        var lastKind: IrParameterKind? = null
        for ((i, param) in element.parameters.withIndex()) {
            if (param.indexInParameters != i) {
                context.error(element, "Inconsistent index (new API) of value parameter ${param.indexInParameters} != $i")
            }

            val kind = param.kind
            if (lastKind != null) {
                if (kind < lastKind) {
                    context.error(
                        element,
                        "Invalid order of function parameters: $kind is placed after $lastKind.\n" +
                                "Parameters must follow a strict order: " +
                                "[dispatch receiver, context parameters, extension receiver, regular parameters]."
                    )
                }

                if (kind == IrParameterKind.DispatchReceiver || kind == IrParameterKind.ExtensionReceiver) {
                    if (kind == lastKind) {
                        context.error(element, "Function may have only one $kind parameter")
                    }
                }
            }

            lastKind = kind
        }

        for ((i, param) in element.typeParameters.withIndex()) {
            if (param.index != i) {
                context.error(element, "Inconsistent index of type parameter ${param.index} != $i")
            }
        }
    }
}