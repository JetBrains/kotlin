/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField

class FirPropertyAccessorRenderer {

    internal lateinit var components: FirRendererComponents
    private val printer get() = components.printer
    private val visitor get() = components.visitor

    fun render(property: FirProperty) {
        printer.println()
        printer.pushIndent()

        if (property.hasExplicitBackingField) {
            property.backingField?.accept(visitor)
            printer.println()
        }

        property.getter?.accept(visitor)
        if (property.getter?.body == null) {
            printer.println()
        }
        if (property.isVar) {
            property.setter?.accept(visitor)
            if (property.setter?.body == null) {
                printer.println()
            }
        }
        printer.popIndent()
    }
}
