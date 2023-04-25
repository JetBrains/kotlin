/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement

class FirBodyRenderer {

    internal lateinit var components: FirRendererComponents

    private val annotationRenderer get() = components.annotationRenderer
    private val visitor get() = components.visitor
    private val printer get() = components.printer

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

    fun renderDelegatedConstructor(delegatedConstructor: FirDelegatedConstructorCall?) {
        if (delegatedConstructor != null) {
            printer.renderInBraces {
                delegatedConstructor.accept(visitor)
                printer.println()
            }
        } else {
            printer.println()
        }
    }
}
