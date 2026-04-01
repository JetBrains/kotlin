/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
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

    open fun renderArgumentsWithEvaluated(arguments: FirResolvedArgumentList, argumentMapping: FirAnnotationArgumentMapping) {
        printer.print("(")
        arguments.mapping.renderSeparatedWithEvaluatedValue(argumentMapping.mapping)
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

    private fun Map<FirExpression, FirValueParameter>.renderSeparatedWithEvaluatedValue(evaluated: Map<Name, FirExpression>) {
        for ((index, element) in this.entries.withIndex()) {
            val (expression, parameter) = element
            val name = parameter.name
            if (index > 0) {
                printer.print(", ")
            }
            printer.print("$name = ")
            expression.accept(visitor)
            if (evaluated.containsKey(name)) {
                printer.print(" [evaluated = ")
                evaluated[name]?.accept(visitor)
                printer.print("]")
            }
        }
    }
}
