/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.declaration

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration

internal object IrPrivateDeclarationOverrideChecker : IrDeclarationChecker<IrDeclaration> {
    override fun check(
        declaration: IrDeclaration,
        context: CheckerContext,
    ) {
        if (declaration is IrOverridableDeclaration<*>) {
            for (overriddenSymbol in declaration.overriddenSymbols) {
                val overriddenDeclaration = overriddenSymbol.owner as? IrDeclarationWithVisibility ?: continue
                if (overriddenDeclaration.visibility == DescriptorVisibilities.PRIVATE) {
                    context.error(declaration, "Overrides private declaration $overriddenDeclaration")
                }
            }
        }
    }
}