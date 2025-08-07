/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.expression

import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.parentAsClass

object IrCallTypeArgumentCountChecker : IrElementChecker<IrFunctionAccessExpression>(IrFunctionAccessExpression::class) {
    override fun check(element: IrFunctionAccessExpression, context: CheckerContext) {
        val function = element.symbol.owner
        when (element) {
            is IrCall -> {
                if (element.typeArguments.size != function.typeParameters.size) {
                    context.error(
                        element, "The call provides ${element.typeArguments.size} type argument(s) " +
                                "but the called function has ${function.typeParameters.size} type parameter(s)"
                    )
                }
            }
            is IrConstructorCall -> {
                if (element.constructorTypeArgumentsCount != function.typeParameters.size) {
                    context.error(
                        element, "The constructor call provides ${element.constructorTypeArgumentsCount} type argument(s) " +
                                "but the called constructor has ${function.typeParameters.size} type parameter(s)"
                    )
                }

                val clazz = function.parentAsClass
                if (element.classTypeArgumentsCount != clazz.typeParameters.size) {
                    context.error(
                        element, "The constructor call provides ${element.classTypeArgumentsCount} type argument(s) " +
                                "but the constructed class has ${clazz.typeParameters.size} type parameter(s)"
                    )
                }
            }
            is IrDelegatingConstructorCall, is IrEnumConstructorCall -> {
                val clazz = function.parentAsClass
                val allTypeParameters = function.typeParameters.size + clazz.typeParameters.size
                if (element.typeArguments.size != allTypeParameters) {
                    context.error(
                        element, "The constructor call provides ${element.typeArguments.size} type argument(s) " +
                                "but expected $allTypeParameters"
                    )
                }
            }
        }
    }
}