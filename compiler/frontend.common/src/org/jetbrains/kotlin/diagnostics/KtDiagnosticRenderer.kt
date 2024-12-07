/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.google.common.annotations.VisibleForTesting
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext
import org.jetbrains.kotlin.diagnostics.rendering.renderParameter
import java.text.MessageFormat

sealed interface KtDiagnosticRenderer {
    @VisibleForTesting val message: String
    fun render(diagnostic: KtDiagnostic): String
    fun renderParameters(diagnostic: KtDiagnostic): Array<out Any?>
}

class SimpleKtDiagnosticRenderer(override val message: String) : KtDiagnosticRenderer {
    override fun render(diagnostic: KtDiagnostic): String {
        require(diagnostic is KtSimpleDiagnostic)
        return message
    }

    override fun renderParameters(diagnostic: KtDiagnostic): Array<out Any?> {
        require(diagnostic is KtSimpleDiagnostic)
        return emptyArray()
    }
}

sealed class AbstractKtDiagnosticWithParametersRenderer(
    final override val message: String
) : KtDiagnosticRenderer {
    private val messageFormat = MessageFormat(message)

    final override fun render(diagnostic: KtDiagnostic): String {
        return messageFormat.format(renderParameters(diagnostic))
    }
}

class KtDiagnosticWithParameters1Renderer<A>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
) : AbstractKtDiagnosticWithParametersRenderer(message) {
    override fun renderParameters(diagnostic: KtDiagnostic): Array<out Any?> {
        require(diagnostic is KtDiagnosticWithParameters1<*>)
        val context = RenderingContext.of(diagnostic.a)
        @Suppress("UNCHECKED_CAST")
        return arrayOf(renderParameter(diagnostic.a as A, rendererForA, context))
    }
}

class KtDiagnosticWithParameters2Renderer<A, B>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
) : AbstractKtDiagnosticWithParametersRenderer(message) {
    override fun renderParameters(diagnostic: KtDiagnostic): Array<out Any?> {
        require(diagnostic is KtDiagnosticWithParameters2<*, *>)
        val context = RenderingContext.of(diagnostic.a, diagnostic.b)
        @Suppress("UNCHECKED_CAST")
        return arrayOf(
            renderParameter(diagnostic.a as A, rendererForA, context),
            renderParameter(diagnostic.b as B, rendererForB, context),
        )
    }
}

class KtDiagnosticWithParameters3Renderer<A, B, C>(
    message: String,
    private val rendererForA: DiagnosticParameterRenderer<A>?,
    private val rendererForB: DiagnosticParameterRenderer<B>?,
    private val rendererForC: DiagnosticParameterRenderer<C>?,
) : AbstractKtDiagnosticWithParametersRenderer(message) {
    override fun renderParameters(diagnostic: KtDiagnostic): Array<out Any?> {
        require(diagnostic is KtDiagnosticWithParameters3<*, *, *>)
        val context = RenderingContext.of(diagnostic.a, diagnostic.b, diagnostic.c)
        @Suppress("UNCHECKED_CAST")
        return arrayOf(
            renderParameter(diagnostic.a as A, rendererForA, context),
            renderParameter(diagnostic.b as B, rendererForB, context),
            renderParameter(diagnostic.c as C, rendererForC, context),
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
    override fun renderParameters(diagnostic: KtDiagnostic): Array<out Any?> {
        require(diagnostic is KtDiagnosticWithParameters4<*, *, *, *>)
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
