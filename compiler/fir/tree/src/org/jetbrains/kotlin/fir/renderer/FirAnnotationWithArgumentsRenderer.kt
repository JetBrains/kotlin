/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.name.Name

class FirAnnotationWithArgumentsRenderer internal constructor(components: FirRendererComponents) : FirAnnotationRenderer(components) {
    override fun FirAnnotation.renderArgumentMapping() {
        printer.print("(")
        argumentMapping.mapping.renderSeparated()
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