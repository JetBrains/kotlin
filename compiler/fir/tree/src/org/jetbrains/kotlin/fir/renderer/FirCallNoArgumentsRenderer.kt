/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.FirExpression

class FirCallNoArgumentsRenderer : FirCallArgumentsRenderer() {
    override fun renderArgumentMapping(argumentMapping: FirAnnotationArgumentMapping) {
        printer.print("(")
        if (argumentMapping.mapping.isNotEmpty()) {
            printer.print("...")
        }
        printer.print(")")
    }

    override fun renderArguments(arguments: List<FirExpression>) {
        printer.print("(")
        if (arguments.isNotEmpty()) {
            printer.print("...")
        }
        printer.print(")")
    }
}