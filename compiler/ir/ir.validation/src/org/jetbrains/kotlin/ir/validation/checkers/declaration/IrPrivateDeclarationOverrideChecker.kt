/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.declaration

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

object IrPrivateDeclarationOverrideChecker : IrElementChecker<IrOverridableDeclaration<*>>(IrOverridableDeclaration::class) {
    override fun check(element: IrOverridableDeclaration<*>, context: CheckerContext) {
        for (overriddenSymbol in element.overriddenSymbols) {
            val overriddenDeclaration = overriddenSymbol.owner as? IrDeclarationWithVisibility ?: continue
            if (overriddenDeclaration.visibility == DescriptorVisibilities.PRIVATE) {
                context.error(element, "Overrides private declaration ${overriddenDeclaration.render()}")
            }
        }
    }
}