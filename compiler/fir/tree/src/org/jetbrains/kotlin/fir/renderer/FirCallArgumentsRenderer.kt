/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.name.Name

open class FirCallArgumentsRenderer {
    internal lateinit var components: FirRendererComponents
    protected val visitor: FirRenderer.Visitor get() = components.visitor
    protected val printer: FirPrinter get() = components.printer

    open fun renderArgumentMapping(argumentMapping: FirAnnotationArgumentMapping) {
        printer.print("(")
        argumentMapping.mapping.renderSeparated()
        printer.print(")")
    }

    open fun renderArguments(arguments: List<FirExpression>) {
        printer.print("(")
        printer.renderSeparated(arguments, visitor)
        printer.print(")")
    }

    private fun Map<Name, FirElement>.renderSeparated() {
        for ((index, element) in this.entries.withIndex()) {
            val (name, argument) = element
            if (index > 0) {
                printer.print(", ")
            }
            printer.print("$name = ")
            argument.accept(visitor)
        }
    }
}
