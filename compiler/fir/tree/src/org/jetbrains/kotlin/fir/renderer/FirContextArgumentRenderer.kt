/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.expressions.FirContextArgumentListOwner

open class FirContextArgumentRenderer {
    internal lateinit var components: FirRendererComponents
    protected val visitor: FirRenderer.Visitor get() = components.visitor
    protected val printer: FirPrinter get() = components.printer

    open fun renderContextArguments(contextArgumentListOwner: FirContextArgumentListOwner) {
        val contextArguments = contextArgumentListOwner.contextArguments.ifEmpty { return }
        printer.print("context(")
        printer.renderSeparated(contextArguments, visitor)
        printer.print(") ")
    }
}
