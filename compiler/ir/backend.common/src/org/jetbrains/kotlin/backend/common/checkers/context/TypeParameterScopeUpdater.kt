/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.context

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.LOCAL
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer

internal object TypeParameterScopeUpdater : ContextUpdater {
    override fun onEnterElement(context: CheckerContext, element: IrElement) {
        if (element is IrTypeParametersContainer) {
            context.typeParameterScopeStack.pushScope(
                outerScopesAreInvisible = element is IrClass && !element.isInner && element.visibility != LOCAL
            ) {
                element.typeParameters.mapTo(this) { it.symbol }
            }
        }
    }

    override fun onExitElement(context: CheckerContext, element: IrElement) {
        if (element is IrTypeParametersContainer) {
            context.typeParameterScopeStack.popScope()
        }
    }
}