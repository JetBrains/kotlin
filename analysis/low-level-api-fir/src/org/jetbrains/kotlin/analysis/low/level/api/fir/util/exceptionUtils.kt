/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.utils.errors.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.withKtModuleEntry
import org.jetbrains.kotlin.analysis.utils.errors.withPsiEntry
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.renderer.ConeTypeRendererForDebugging
import org.jetbrains.kotlin.fir.renderer.FirDeclarationRendererWithAttributes
import org.jetbrains.kotlin.fir.renderer.FirFileAnnotationsContainerRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.renderer.FirResolvePhaseRenderer
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType


fun ExceptionAttachmentBuilder.withFirEntry(name: String, fir: FirElement) {
    withEntry(name, fir) {
        FirRenderer(
            resolvePhaseRenderer = FirResolvePhaseRenderer(),
            declarationRenderer = FirDeclarationRendererWithAttributes(),
            fileAnnotationsContainerRenderer = FirFileAnnotationsContainerRenderer(),
        ).renderElementAsString(it)
    }
    withEntry("${name}FirSourceElementKind", fir.source?.kind?.let { it::class.simpleName })
    if (fir is FirElementWithResolveState) {
        withKtModuleEntry("${name}KtModule", fir.llFirModuleData.ktModule)
    }
    withPsiEntry("${name}Psi", fir.psi)
}


fun ExceptionAttachmentBuilder.withFirSymbolEntry(name: String, symbol: FirBasedSymbol<*>) {
    withFirEntry("${name}Fir", symbol.fir)
}

fun ExceptionAttachmentBuilder.withConeTypeEntry(name: String, coneType: ConeKotlinType) {
    withEntry(name, coneType) {
        buildString { ConeTypeRendererForDebugging(this).render(it) }
    }
}


fun errorWithFirSpecificEntries(
    message: String,
    cause: Exception? = null,
    fir: FirElement? = null,
    coneType: ConeKotlinType? = null,
    psi: PsiElement? = null,
    additionalInfos: ExceptionAttachmentBuilder.() -> Unit = {}
): Nothing {
    buildErrorWithAttachment(message, cause) {
        if (fir != null) {
            withFirEntry("fir", fir)
        }

        if (psi != null) {
            withPsiEntry("psi", psi)
        }

        if (coneType != null) {
            withConeTypeEntry("coneType", coneType)
        }
        additionalInfos()
    }
}
