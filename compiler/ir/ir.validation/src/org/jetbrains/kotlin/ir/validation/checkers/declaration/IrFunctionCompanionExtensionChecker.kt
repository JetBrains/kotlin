/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.declaration

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.isFacadeClass
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

object IrFunctionCompanionExtensionChecker : IrElementChecker<IrSimpleFunction>(IrSimpleFunction::class) {
    override fun check(element: IrSimpleFunction, context: CheckerContext) {
        val companionExtensionClass = element.companionExtensionClass
        if (companionExtensionClass != null && element.isStatic) {
            // On JVM we wrap everything in a file into a faced class, so technically companion extension function can be static by our rules.
            // We want to exclude such a case from validation.
            if (element.parentAsClass.isFacadeClass) return
            context.error(
                element,
                "Static function '${element.render()}' cannot have a companion extension class"
            )
        }
    }
}
