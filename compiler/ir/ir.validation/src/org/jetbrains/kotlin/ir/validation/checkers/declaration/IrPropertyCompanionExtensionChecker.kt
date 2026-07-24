/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.declaration

import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

object IrPropertyCompanionExtensionChecker : IrElementChecker<IrProperty>(IrProperty::class) {
    override fun check(element: IrProperty, context: CheckerContext) {
        if (element.getter == null || element.setter == null) return
        if (element.getter?.companionExtensionClass != element.setter?.companionExtensionClass) {
            context.error(
                element,
                "Getter and setter of property '${element.render()}' has an inconsistent companion extension class ${element.getter?.companionExtensionClass}"
            )
        }
    }
}
