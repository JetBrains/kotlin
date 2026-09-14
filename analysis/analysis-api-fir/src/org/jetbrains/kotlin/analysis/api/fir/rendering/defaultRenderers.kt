/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.rendering

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.rendering.*
import org.jetbrains.kotlin.utils.exceptions.buildErrorWithAttachment

/** The default renderer, with the built-in piece renderer stacks assembled from the neighboring group files. */
internal val DEFAULT_RENDERER: KaRenderer = buildRenderer(null) {
    pushSymbolRenderers()
    pushAnnotationRenderers()
    pushNameRenderers()
    pushCallableRenderers()
    pushParameterRenderers()
    pushClassifierRenderers()
    pushTypeRenderers()
    push(UnknownRenderer)
}

private object UnknownRenderer : KaPieceRenderer<Pair<Any?, KaPiece<*>>>(KaPiece.Unknown) {
    private val LOG = logger<UnknownRenderer>()

    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: Pair<Any?, KaPiece<*>>, next: () -> Unit): Boolean {
        val [passedValue, piece] = value
        val error = buildErrorWithAttachment("Renderer is not set for value of type ${passedValue?.javaClass?.name}") {
            withEntry("value", passedValue) { it.toString() }
            withEntry("piece", piece) { it.toString() }
        }
        LOG.error(error)
        return true
    }
}
