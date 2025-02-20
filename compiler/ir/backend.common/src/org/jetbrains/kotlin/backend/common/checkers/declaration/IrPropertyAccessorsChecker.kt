/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.declaration

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.render

internal object IrPropertyAccessorsChecker : IrPropertyChecker {
    override fun check(
        declaration: IrProperty,
        context: CheckerContext,
    ) {
        declaration.getter?.let {
            if (it.correspondingPropertySymbol != declaration.symbol) {
                context.error(
                    declaration,
                    "Getter of property '${declaration.render()}' has an inconsistent corresponding property symbol."
                )
            }
        }
        declaration.setter?.let {
            if (it.correspondingPropertySymbol != declaration.symbol) {
                context.error(
                    declaration,
                    "Setter of property '${declaration.render()}' has an inconsistent corresponding property symbol."
                )
            }
        }
    }
}