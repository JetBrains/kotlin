/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

fun IdSignature.render(renderer: IdSignatureRenderer = IdSignatureRenderer.DEFAULT): String = renderer.render(this)

class IdSignatureRenderer private constructor(private val showDescriptionForPublicSignatures: Boolean) {
    fun render(signature: IdSignature): String = buildString { render(signature) }

    private fun StringBuilder.render(signature: IdSignature): StringBuilder = when (signature) {
        is IdSignature.CommonSignature -> render(signature)
        is IdSignature.AccessorSignature -> render(signature)
        is IdSignature.CompositeSignature -> render(signature)
        is IdSignature.FileSignature -> render(signature)
        is IdSignature.LocalSignature -> render(signature)
        is IdSignature.FileLocalSignature -> render(signature)
        is IdSignature.ScopeLocalDeclaration -> render(signature)
        is IdSignature.SpecialFakeOverrideSignature -> render(signature)
        is IdSignature.LoweredDeclarationSignature -> render(signature)
    }

    private fun StringBuilder.render(signature: IdSignature.CommonSignature): StringBuilder = with(signature) {
        append(packageFqName).append('/').append(declarationFqName).append('|')
        append(if (showDescriptionForPublicSignatures) signature.description ?: id else id)
        append('[').append(mask.toString(2)).append(']')
    }

    private fun StringBuilder.render(signature: IdSignature.AccessorSignature): StringBuilder =
        render(signature.accessorSignature)

    private fun StringBuilder.render(signature: IdSignature.CompositeSignature): StringBuilder =
        append("[ ").render(signature.container).append(" <- ").render(signature.inner).append(" ]")

    private fun StringBuilder.render(signature: IdSignature.FileSignature): StringBuilder =
        append("File '").append(signature.fileName).append('\'')

    private fun StringBuilder.render(signature: IdSignature.LocalSignature): StringBuilder = with(signature) {
        append("Local[").append(localFqn)
        hashSig?.let { append(",").append(hashSig) }
        renderDescriptionForLocalSignature(description) // Always include description for local signatures if there is any.
        append(']')
    }

    private fun StringBuilder.render(signature: IdSignature.FileLocalSignature): StringBuilder = with(signature) {
        render(container).append(':').append(id)
        renderDescriptionForLocalSignature(description) // Always include description for local signatures if there is any.
    }

    private fun StringBuilder.render(signature: IdSignature.ScopeLocalDeclaration): StringBuilder = with(signature) {
        append('#').append(id)
        renderDescriptionForLocalSignature(description) // Always include description for local signatures if there is any.
    }

    private fun StringBuilder.render(signature: IdSignature.SpecialFakeOverrideSignature): StringBuilder =
        render(signature.memberSignature)

    private fun StringBuilder.render(signature: IdSignature.LoweredDeclarationSignature): StringBuilder = with(signature) {
        append("ic#").append(stage).append(':')
        render(original)
        append('-').append(index)
    }

    private fun StringBuilder.renderDescriptionForLocalSignature(description: String?): StringBuilder {
        description?.let { append('|').append(description) }
        return this
    }

    companion object {
        val DEFAULT = IdSignatureRenderer(showDescriptionForPublicSignatures = true)
        val LEGACY = IdSignatureRenderer(showDescriptionForPublicSignatures = false)
    }
}
