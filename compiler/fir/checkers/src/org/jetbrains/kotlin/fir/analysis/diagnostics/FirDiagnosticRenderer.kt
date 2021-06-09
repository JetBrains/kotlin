/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext
import org.jetbrains.kotlin.diagnostics.rendering.renderParameter
import java.text.MessageFormat

sealed interface FirDiagnosticRenderer {
    fun render(diagnostic: FirDiagnostic): String
    fun renderParameters(diagnostic: FirDiagnostic): Array<out Any?>
}

class SimpleFirDiagnosticRenderer(private val message: String) : FirDiagnosticRenderer {
    override fun render(diagnostic: FirDiagnostic): String {
        require(diagnostic is FirSimpleDiagnostic)
        return message
    }

    override fun renderParameters(diagnostic: FirDiagnostic): Array<out Any?> {
        require(diagnostic is FirSimpleDiagnostic)
        return emptyArray()
    }
}

sealed class AbstractFirDiagnosticWithParametersRenderer(
    protected val message: String
) : FirDiagnosticRenderer {
    private val messageFormat = MessageFormat(message)

    final override fun render(diagnostic: FirDiagnostic): String {
        return messageFormat.format(renderParameters(diagnostic))
    }
}

class FirDiagnosticWithParameters1Renderer<A>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
) : AbstractFirDiagnosticWithParametersRenderer(message) {
    override fun renderParameters(diagnostic: FirDiagnostic): Array<out Any?> {
        require(diagnostic is FirDiagnosticWithParameters1<*>)
        val context = RenderingContext.of(diagnostic.a)
        @Suppress("UNCHECKED_CAST")
        return arrayOf(renderParameter(diagnostic.a as A, rendererForA, context))
    }
}

class FirDiagnosticWithParameters2Renderer<A, B>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
) : AbstractFirDiagnosticWithParametersRenderer(message) {
    override fun renderParameters(diagnostic: FirDiagnostic): Array<out Any?> {
        require(diagnostic is FirDiagnosticWithParameters2<*, *>)
        val context = RenderingContext.of(diagnostic.a, diagnostic.b)
        @Suppress("UNCHECKED_CAST")
        return arrayOf(
            renderParameter(diagnostic.a as A, rendererForA, context),
            renderParameter(diagnostic.b as B, rendererForB, context),
        )
    }
}

class FirDiagnosticWithParameters3Renderer<A, B, C>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
    private val rendererForC: DiagnosticParameterRenderer<C>?,
) : AbstractFirDiagnosticWithParametersRenderer(message) {
    override fun renderParameters(diagnostic: FirDiagnostic): Array<out Any?> {
        require(diagnostic is FirDiagnosticWithParameters3<*, *, *>)
        val context = RenderingContext.of(diagnostic.a, diagnostic.b, diagnostic.c)
        @Suppress("UNCHECKED_CAST")
        return arrayOf(
            renderParameter(diagnostic.a as A, rendererForA, context),
            renderParameter(diagnostic.b as B, rendererForB, context),
            renderParameter(diagnostic.c as C, rendererForC, context),
        )
    }
}

class FirDiagnosticWithParameters4Renderer<A, B, C, D>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
    private val rendererForC: DiagnosticParameterRenderer<C>?,
    private val rendererForD: DiagnosticParameterRenderer<D>?,
) : AbstractFirDiagnosticWithParametersRenderer(message) {
    override fun renderParameters(diagnostic: FirDiagnostic): Array<out Any?> {
        require(diagnostic is FirDiagnosticWithParameters4<*, *, *, *>)
        val context = RenderingContext.of(diagnostic.a, diagnostic.b, diagnostic.c, diagnostic.d)
        @Suppress("UNCHECKED_CAST")
        return arrayOf(
            renderParameter(diagnostic.a as A, rendererForA, context),
            renderParameter(diagnostic.b as B, rendererForB, context),
            renderParameter(diagnostic.c as C, rendererForC, context),
            renderParameter(diagnostic.d as D, rendererForD, context),
        )
    }
}
