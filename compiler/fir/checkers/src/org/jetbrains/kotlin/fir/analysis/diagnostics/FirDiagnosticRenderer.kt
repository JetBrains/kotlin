/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.*

interface FirDiagnosticRenderer<D : FirDiagnostic<*>> : DiagnosticRenderer<D> {
    override fun render(diagnostic: D): String
}

class SimpleFirDiagnosticRenderer(private val message: String) : FirDiagnosticRenderer<FirSimpleDiagnostic<*>> {
    override fun render(diagnostic: FirSimpleDiagnostic<*>): String {
        return message
    }
}

sealed class AbstractFirDiagnosticWithParametersRenderer<D : FirDiagnostic<*>>(
    protected val message: String
) : FirDiagnosticRenderer<D>, AbstractDiagnosticWithParametersRenderer<D>(message)

class FirDiagnosticWithParameters1Renderer<A : Any>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
) : AbstractFirDiagnosticWithParametersRenderer<FirDiagnosticWithParameters1<*, A>>(message) {
    override fun renderParameters(diagnostic: FirDiagnosticWithParameters1<*, A>): Array<out Any> {
        val context = RenderingContext.of(diagnostic.a)
        return arrayOf(renderParameter(diagnostic.a, rendererForA, context))
    }
}

class FirDiagnosticWithParameters2Renderer<A : Any, B : Any>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
) : AbstractFirDiagnosticWithParametersRenderer<FirDiagnosticWithParameters2<*, A, B>>(message) {
    override fun renderParameters(diagnostic: FirDiagnosticWithParameters2<*, A, B>): Array<out Any> {
        val context = RenderingContext.of(diagnostic.a, diagnostic.b)
        return arrayOf(
            renderParameter(diagnostic.a, rendererForA, context),
            renderParameter(diagnostic.b, rendererForB, context),
        )
    }
}

class FirDiagnosticWithParameters3Renderer<A : Any, B : Any, C : Any>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
    private val rendererForC: DiagnosticParameterRenderer<C>?,
) : AbstractFirDiagnosticWithParametersRenderer<FirDiagnosticWithParameters3<*, A, B, C>>(message) {
    override fun renderParameters(diagnostic: FirDiagnosticWithParameters3<*, A, B, C>): Array<out Any> {
        val context = RenderingContext.of(diagnostic.a, diagnostic.b, diagnostic.c)
        return arrayOf(
            renderParameter(diagnostic.a, rendererForA, context),
            renderParameter(diagnostic.b, rendererForB, context),
            renderParameter(diagnostic.c, rendererForC, context),
        )
    }
}
