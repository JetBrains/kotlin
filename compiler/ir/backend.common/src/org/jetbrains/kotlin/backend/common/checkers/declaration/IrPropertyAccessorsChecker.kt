/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.declaration

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.backend.common.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.render

internal object IrPropertyAccessorsChecker : IrElementChecker<IrProperty>(IrProperty::class) {
    override fun check(element: IrProperty, context: CheckerContext) {
        element.getter?.let {
            if (it.correspondingPropertySymbol != element.symbol) {
                context.error(
                    element,
                    "Getter of property '${element.render()}' has an inconsistent corresponding property symbol."
                )
            }
        }
        element.setter?.let {
            if (it.correspondingPropertySymbol != element.symbol) {
                context.error(
                    element,
                    "Setter of property '${element.render()}' has an inconsistent corresponding property symbol."
                )
            }
        }
    }
}