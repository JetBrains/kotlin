/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol

class FirPropertyAccessorRenderer {

    internal lateinit var components: FirRendererComponents
    private val printer get() = components.printer
    private val visitor get() = components.visitor

    fun render(property: FirProperty) {
        if (property.origin !is FirDeclarationOrigin.ScriptCustomization || property.hasExplicitBackingField ||
            property.getter != null || property.isVar && property.setter != null
        ) {
            printer.println()
        }
        printer.pushIndent()

        if (property.hasExplicitBackingField) {
            property.backingField?.accept(visitor)
            printer.println()
        }

        fun FirPropertyAccessor.render() {
            accept(visitor)
            if (body == null) {
                printer.println()
            }
        }

        property.getter?.render()
        if (property.isVar) {
            property.setter?.render()
        }
        printer.popIndent()
    }
}
