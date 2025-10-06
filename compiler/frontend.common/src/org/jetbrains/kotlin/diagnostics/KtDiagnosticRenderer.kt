/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.ParameterWithTail
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext
import org.jetbrains.kotlin.diagnostics.rendering.renderParameter
import org.jetbrains.kotlin.diagnostics.rendering.renderTailsJoined
import java.text.MessageFormat

sealed class KtDiagnosticRenderer {
    abstract val message: String
    abstract fun render(diagnostic: KtDiagnostic): String
    abstract fun renderParameters(diagnostic: KtDiagnostic): Parameters

    class Parameters(
        val parameters: List<Any?>,
        val tail: String?,
    ) {
        constructor(vararg parameters: Any?) : this(listOf(*parameters), null)
    }
}

class SimpleKtDiagnosticRenderer(override val message: String) : KtDiagnosticRenderer() {
    override fun render(diagnostic: KtDiagnostic): String {
        require(diagnostic is KtSimpleDiagnostic)
        return message
    }

    override fun renderParameters(diagnostic: KtDiagnostic): Parameters {
        require(diagnostic is KtSimpleDiagnostic)
        return Parameters()
    }
}

sealed class AbstractKtDiagnosticWithParametersRenderer(
    final override val message: String
) : KtDiagnosticRenderer() {
    private val messageFormat = MessageFormat(message)

    final override fun render(diagnostic: KtDiagnostic): String {
        val parameters = renderParameters(diagnostic)
        return messageFormat.format(parameters.parameters.toTypedArray()) + (parameters.tail ?: "")
    }
}

class KtSourcelessDiagnosticRenderer(message: String) : AbstractKtDiagnosticWithParametersRenderer(message) {
    override fun renderParameters(diagnostic: KtDiagnostic): Parameters {
        require(diagnostic is KtDiagnosticWithoutSource)
        return Parameters(diagnostic.message)
    }
}

private fun <P> renderAndDispatchParameter(
    parameter: P,
    renderer: DiagnosticParameterRenderer<P>?,
    context: RenderingContext,
): Pair<Any?, List<String>?> {
    val rendered = renderParameter(parameter, renderer, context)
    if (rendered is ParameterWithTail) return (rendered.parameter to rendered.tail)
    return rendered to null
}

class KtDiagnosticWithParameters1Renderer<A>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
) : AbstractKtDiagnosticWithParametersRenderer(message) {
    override fun renderParameters(diagnostic: KtDiagnostic): Parameters {
        require(diagnostic is KtDiagnosticWithParameters1<*>)
        val context = RenderingContext.of(diagnostic.a)

        val (renderedA, tailA) = @Suppress("UNCHECKED_CAST")
        renderAndDispatchParameter(diagnostic.a as A, rendererForA, context)

        return Parameters(
            listOf(renderedA),
            renderTailsJoined(tailA),
        )
    }
}

class KtDiagnosticWithParameters2Renderer<A, B>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
) : AbstractKtDiagnosticWithParametersRenderer(message) {
    override fun renderParameters(diagnostic: KtDiagnostic): Parameters {
        require(diagnostic is KtDiagnosticWithParameters2<*, *>)
        val context = RenderingContext.of(diagnostic.a, diagnostic.b)

        val (renderedA, tailA) = @Suppress("UNCHECKED_CAST")
        renderAndDispatchParameter(diagnostic.a as A, rendererForA, context)

        val (renderedB, tailB) = @Suppress("UNCHECKED_CAST")
        renderAndDispatchParameter(diagnostic.b as B, rendererForB, context)

        return Parameters(
            listOf(renderedA, renderedB),
            renderTailsJoined(tailA, tailB),
        )
    }
}

class KtDiagnosticWithParameters3Renderer<A, B, C>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
    private val rendererForC: DiagnosticParameterRenderer<C>?,
) : AbstractKtDiagnosticWithParametersRenderer(message) {
    override fun renderParameters(diagnostic: KtDiagnostic): Parameters {
        require(diagnostic is KtDiagnosticWithParameters3<*, *, *>)
        val context = RenderingContext.of(diagnostic.a, diagnostic.b, diagnostic.c)

        val (renderedA, tailA) = @Suppress("UNCHECKED_CAST")
        renderAndDispatchParameter(diagnostic.a as A, rendererForA, context)

        val (renderedB, tailB) = @Suppress("UNCHECKED_CAST")
        renderAndDispatchParameter(diagnostic.b as B, rendererForB, context)

        val (renderedC, tailC) = @Suppress("UNCHECKED_CAST")
        renderAndDispatchParameter(diagnostic.c as C, rendererForC, context)

        return Parameters(
            listOf(renderedA, renderedB, renderedC),
            renderTailsJoined(tailA, tailB, tailC),
        )
    }
}

class KtDiagnosticWithParameters4Renderer<A, B, C, D>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
    private val rendererForC: DiagnosticParameterRenderer<C>?,
    private val rendererForD: DiagnosticParameterRenderer<D>?,
) : AbstractKtDiagnosticWithParametersRenderer(message) {
    override fun renderParameters(diagnostic: KtDiagnostic): Parameters {
        require(diagnostic is KtDiagnosticWithParameters4<*, *, *, *>)
        val context = RenderingContext.of(diagnostic.a, diagnostic.b, diagnostic.c, diagnostic.d)

        val (renderedA, tailA) = @Suppress("UNCHECKED_CAST")
        renderAndDispatchParameter(diagnostic.a as A, rendererForA, context)

        val (renderedB, tailB) = @Suppress("UNCHECKED_CAST")
        renderAndDispatchParameter(diagnostic.b as B, rendererForB, context)

        val (renderedC, tailC) = @Suppress("UNCHECKED_CAST")
        renderAndDispatchParameter(diagnostic.c as C, rendererForC, context)

        val (renderedD, tailD) = @Suppress("UNCHECKED_CAST")
        renderAndDispatchParameter(diagnostic.d as D, rendererForD, context)

        return Parameters(
            listOf(renderedA, renderedB, renderedC, renderedD),
            renderTailsJoined(tailA, tailB, tailC, tailD),
        )
    }
}
