/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import kotlin.reflect.KClass
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirResolvedDeclarationStatus
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.renderer.FirDeclarationRendererWithAttributes
import org.jetbrains.kotlin.fir.renderer.FirFileAnnotationsContainerRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.renderer.FirResolvePhaseRenderer
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

internal fun FirBasedSymbol<*>.errorInLazyResolve(name: String, actualClass: KClass<*>, expected: KClass<*>): Nothing {
    throw KotlinExceptionWithAttachments("Unexpected $name. Expected is ${expected.simpleName}, but was ${actualClass.simpleName}").apply {
        withAttachment(
            "FirElement.txt",
            FirRenderer(
                resolvePhaseRenderer = FirResolvePhaseRenderer(),
                declarationRenderer = FirDeclarationRendererWithAttributes(),
                fileAnnotationsContainerRenderer = FirFileAnnotationsContainerRenderer(),
            ).renderElementAsString(fir),
        )

        withAttachment("FirBasedSymbol.txt", this@errorInLazyResolve::class.simpleName)
        withAttachment("KtSourceElementKind.txt", fir.source?.kind?.let { it::class.simpleName })
        withPsiAttachment("PsiElement.txt", fir.psi)
    }
}

internal fun FirMemberDeclaration.resolvedStatus(): FirResolvedDeclarationStatus {
    lazyResolveToPhase(FirResolvePhase.STATUS)

    val status = status
    if (status !is FirResolvedDeclarationStatus) {
        symbol.errorInLazyResolve("status", status::class, FirResolvedDeclarationStatus::class)
    }

    return status
}