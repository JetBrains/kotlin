/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.*
import java.text.MessageFormat

sealed interface FirDiagnosticRenderer<D : FirDiagnostic<*>> {
    fun render(diagnostic: D): String
    fun renderParameters(diagnostic: D): Array<out Any?>
}

class SimpleFirDiagnosticRenderer(private val message: String) : FirDiagnosticRenderer<FirSimpleDiagnostic<*>> {
    override fun render(diagnostic: FirSimpleDiagnostic<*>): String {
        return message
    }

    override fun renderParameters(diagnostic: FirSimpleDiagnostic<*>): Array<out Any?> {
        return emptyArray()
    }
}

sealed class AbstractFirDiagnosticWithParametersRenderer<D : FirDiagnostic<*>>(
    protected val message: String
) : FirDiagnosticRenderer<D> {
    private val messageFormat = MessageFormat(message)

    final override fun render(diagnostic: D): String {
        return messageFormat.format(renderParameters(diagnostic))
    }
}

class FirDiagnosticWithParameters1Renderer<A>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
) : AbstractFirDiagnosticWithParametersRenderer<FirDiagnosticWithParameters1<*, A>>(message) {
    override fun renderParameters(diagnostic: FirDiagnosticWithParameters1<*, A>): Array<out Any?> {
        val context = RenderingContext.of(diagnostic.a)
        return arrayOf(renderParameter(diagnostic.a, rendererForA, context))
    }
}

class FirDiagnosticWithParameters2Renderer<A, B>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
) : AbstractFirDiagnosticWithParametersRenderer<FirDiagnosticWithParameters2<*, A, B>>(message) {
    override fun renderParameters(diagnostic: FirDiagnosticWithParameters2<*, A, B>): Array<out Any?> {
        val context = RenderingContext.of(diagnostic.a, diagnostic.b)
        return arrayOf(
            renderParameter(diagnostic.a, rendererForA, context),
            renderParameter(diagnostic.b, rendererForB, context),
        )
    }
}

class FirDiagnosticWithParameters3Renderer<A, B, C>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
    private val rendererForC: DiagnosticParameterRenderer<C>?,
) : AbstractFirDiagnosticWithParametersRenderer<FirDiagnosticWithParameters3<*, A, B, C>>(message) {
    override fun renderParameters(diagnostic: FirDiagnosticWithParameters3<*, A, B, C>): Array<out Any?> {
        val context = RenderingContext.of(diagnostic.a, diagnostic.b, diagnostic.c)
        return arrayOf(
            renderParameter(diagnostic.a, rendererForA, context),
            renderParameter(diagnostic.b, rendererForB, context),
            renderParameter(diagnostic.c, rendererForC, context),
        )
    }
}

class FirDiagnosticWithParameters4Renderer<A, B, C, D>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
    private val rendererForC: DiagnosticParameterRenderer<C>?,
    private val rendererForD: DiagnosticParameterRenderer<D>?,
) : AbstractFirDiagnosticWithParametersRenderer<FirDiagnosticWithParameters4<*, A, B, C, D>>(message) {
    override fun renderParameters(diagnostic: FirDiagnosticWithParameters4<*, A, B, C, D>): Array<out Any?> {
        val context = RenderingContext.of(diagnostic.a, diagnostic.b, diagnostic.c)
        return arrayOf(
            renderParameter(diagnostic.a, rendererForA, context),
            renderParameter(diagnostic.b, rendererForB, context),
            renderParameter(diagnostic.c, rendererForC, context),
            renderParameter(diagnostic.d, rendererForD, context),
        )
    }
}
