/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyBlock

class FirBodyRenderer internal constructor(components: FirRendererComponents) : FirRendererComponents by components {

    fun render(function: FirFunction) {
        renderBody(function.body)
    }

    fun renderBody(block: FirBlock?, additionalStatements: List<FirStatement> = emptyList()) {
        if (block == null) return
        when (block) {
            is FirLazyBlock -> {
                printer.println(" { LAZY_BLOCK }")
            }
            else -> {
                annotationRenderer?.render(block)
                printer.renderInBraces {
                    for (statement in additionalStatements + block.statements) {
                        statement.accept(visitor)
                        printer.println()
                    }
                }
            }
        }
    }
}
