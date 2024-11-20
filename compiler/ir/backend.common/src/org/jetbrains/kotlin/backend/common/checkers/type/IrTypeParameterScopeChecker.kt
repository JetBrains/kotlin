/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.type

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.render

/**
 * Makes sure that all the type parameter references are within the scope of the corresponding type parameters.
 */
internal object IrTypeParameterScopeChecker : IrTypeChecker {
    override fun check(
        type: IrType,
        container: IrElement,
        context: CheckerContext,
    ) {
        ((type as? IrSimpleType)?.classifier as? IrTypeParameterSymbol)?.let {
            checkTypeParameterReference(context, container, it)
        }
    }

    private fun checkTypeParameterReference(
        context: CheckerContext,
        element: IrElement,
        typeParameterSymbol: IrTypeParameterSymbol,
    ) {
        if (!context.typeParameterScopeStack.isVisibleInCurrentScope(typeParameterSymbol)) {
            context.error(
                element,
                "The following element references a type parameter '${typeParameterSymbol.owner.render()}' that is not available " +
                        "in the current scope."
            )
        }
    }
}