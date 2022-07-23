/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.utils.errors.withPsiAttachment
import org.jetbrains.kotlin.analysis.utils.printer.getElementTextInContext
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.renderer.ConeTypeRendererForDebugging
import org.jetbrains.kotlin.fir.renderer.FirDeclarationRendererWithAttributes
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.renderer.FirResolvePhaseRenderer
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import org.jetbrains.kotlin.utils.errorWithAttachment
import org.jetbrains.kotlin.utils.withAttachmentDetailed


fun KotlinExceptionWithAttachments.withFirAttachment(name: String, fir: FirElement): KotlinExceptionWithAttachments {
    withAttachmentDetailed(name, fir, debugFirRenderer::renderElementAsString)
    withPsiAttachment("${name}Psi", fir.psi)
    return this
}

fun KotlinExceptionWithAttachments.withFirSymbolAttachment(name: String, symbol: FirBasedSymbol<*>): KotlinExceptionWithAttachments {
    withAttachment("${name}Class", symbol::class.java.name)
    withFirAttachment("${name}Fir", symbol.fir)
    return this
}

fun KotlinExceptionWithAttachments.withConeTypeAttachment(name: String, coneType: ConeKotlinType): KotlinExceptionWithAttachments {
    withAttachmentDetailed(name, coneType) {
        buildString { ConeTypeRendererForDebugging(this).render(it) }
    }
    return this
}




fun firErrorWithAttachment(
    message: String,
    cause: Throwable? = null,
    fir: FirElement? = null,
    coneType: ConeKotlinType? = null,
    psi: PsiElement? = null,
    attachments: KotlinExceptionWithAttachments.() -> Unit = {}
): Nothing {
    errorWithAttachment(message, cause) {
        if (fir != null) {
            withFirAttachment("fir", fir)
        }

        if (psi != null) {
            withPsiAttachment("psi", psi)
        }

        if (coneType != null) {
            withConeTypeAttachment("coneType", coneType)
        }
        attachments ()
    }
}


val debugFirRenderer =
    FirRenderer(resolvePhaseRenderer = FirResolvePhaseRenderer(), declarationRenderer = FirDeclarationRendererWithAttributes())
