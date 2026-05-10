/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.type

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isTypeOfIntrinsicCall
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.validation.checkers.IrTypeChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.validation.checkers.context.ContextUpdater
import org.jetbrains.kotlin.ir.validation.checkers.context.TypeParameterScopeUpdater

/**
 * Makes sure that all the type parameter references are within the scope of the corresponding type parameters.
 */
object IrTypeParameterScopeChecker : IrTypeChecker {
    override val requiredContextUpdaters: Set<ContextUpdater>
        get() = setOf(TypeParameterScopeUpdater)

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
        if (shouldReportVisibilityError(context, element, typeParameterSymbol)) {
            context.error(
                element,
                "The following element references a type parameter '${typeParameterSymbol.owner.render()}' that is not available " +
                        "in the current scope."
            )
        }
    }

    private fun shouldReportVisibilityError(
        context: CheckerContext,
        element: IrElement,
        typeParameterSymbol: IrTypeParameterSymbol,
    ): Boolean {
        // typeOf intrinsic is a special case with allowed out-of-scope type parameters.
        if (element.isTypeOfIntrinsicCall()) return false
        // TODO(KT-85683): Temporary workaround for property backing field, remove once the issue is fixed.
        if ((context.parentChain + element).any { it.isBackingFieldWithGettersTypeParameter(typeParameterSymbol) }) return false
        return !context.typeParameterScopeStack.isVisibleInCurrentScope(typeParameterSymbol)
    }

    private fun IrElement.isBackingFieldWithGettersTypeParameter(typeParameterSymbol: IrTypeParameterSymbol): Boolean {
        if (this !is IrField || this.origin != IrDeclarationOrigin.PROPERTY_DELEGATE) return false
        val typeParameters = correspondingPropertySymbol?.owner?.getter?.typeParameters ?: return false
        return typeParameters.any { it.symbol == typeParameterSymbol }
    }
}
