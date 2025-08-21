/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

open class FirClassMemberRenderer {
    internal lateinit var components: FirRendererComponents
    protected val visitor: FirRenderer.Visitor get() = components.visitor
    protected val printer: FirPrinter get() = components.printer

    @OptIn(DirectDeclarationsAccess::class)
    open fun render(regularClass: FirRegularClass) {
        render(regularClass.declarations)
    }

    open fun render(declarations: List<FirDeclaration>) {
        printer.renderInBraces {
            for (declaration in declarations) {
                declaration.accept(visitor)
                printer.println()
            }
        }
    }
}
