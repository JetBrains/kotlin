/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.psi.KtFile

fun FirElementWithResolveState.getContainingFile(): FirFile? {
    val provider = moduleData.session.firProvider
    return when (this) {
        is FirFile -> this
        is FirScript -> provider.getFirScriptContainerFile(symbol)
        is FirFileAnnotationsContainer -> containingFileSymbol.fir
        is FirTypeParameter -> containingDeclarationSymbol.fir.getContainingFile()
        is FirPropertyAccessor -> propertySymbol.fir.getContainingFile()
        is FirValueParameter -> containingFunctionSymbol.fir.getContainingFile()
        is FirBackingField -> propertySymbol.fir.getContainingFile()
        is FirCallableDeclaration -> provider.getFirCallableContainerFile(symbol)
        is FirClassLikeDeclaration -> provider.getFirClassifierContainerFileIfAny(symbol)
        is FirAnonymousInitializer -> containingClass().getContainingFile()
        is FirDanglingModifierList, is FirCodeFragment -> {
            val ktFile = psi?.containingFile as? KtFile
                ?: error("File for dangling modifier list cannot be null")
            val moduleComponents = llFirResolvableSession?.moduleComponents
                ?: error("LLFirResolvableModuleSession for dangling modifier list cannot be null")
            moduleComponents.cache.getCachedFirFile(ktFile)
                ?: error("Fir file for dandling modifier list cannot be null")
        }
        else -> errorWithFirSpecificEntries("Unsupported declaration ${this::class}", fir = this)
    }
}
