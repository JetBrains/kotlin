/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.psi.KtFile

@KaImplementationDetail
fun FirElementWithResolveState.getContainingFile(): FirFile? {
    val provider = moduleData.session.firProvider
    return when (this) {
        is FirFile -> this
        is FirScript -> provider.getFirScriptContainerFile(symbol)
        is FirReplSnippet -> provider.getFirReplSnippetContainerFile(symbol)
        is FirTypeParameter -> containingDeclarationSymbol.fir.getContainingFile()
        is FirPropertyAccessor -> propertySymbol.fir.getContainingFile()
        is FirValueParameter -> containingDeclarationSymbol.fir.getContainingFile()
        is FirBackingField -> propertySymbol.fir.getContainingFile()
        is FirCallableDeclaration -> provider.getFirCallableContainerFile(symbol) ?: getContainingFileByClassifier()
        is FirClassLikeDeclaration -> provider.getFirClassifierContainerFileIfAny(symbol)
        is FirAnonymousInitializer -> {
            if (getContainingClassSymbol()?.isLocal == true) {
                containingKtFileIfAny?.let {
                    val moduleComponents = llFirResolvableSession?.moduleComponents
                    moduleComponents?.cache?.getCachedFirFile(it)
                }
            } else {
                containingDeclarationSymbol.fir.getContainingFile()
            }
        }
        is FirDanglingModifierList, is FirCodeFragment -> {
            val ktFile = psi?.containingFile as? KtFile
                ?: error("File for dangling modifier list cannot be null")
            val moduleComponents = llFirResolvableSession?.moduleComponents
                ?: error("LLFirResolvableModuleSession for dangling modifier list cannot be null")
            moduleComponents.cache.getCachedFirFile(ktFile)
                ?: error("Fir file for dandling modifier list cannot be null")
        }
        is FirReceiverParameter -> containingDeclarationSymbol.fir.getContainingFile()
        else -> errorWithFirSpecificEntries("Unsupported declaration ${this::class}", fir = this)
    }
}

/**
 * The containing file of a callable which is synthesized by the session of the use site: a fake override, a delegated member or a SAM
 * constructor.
 *
 * Such a callable has the use-site [moduleData][FirCallableDeclaration.moduleData], while its source points to a declaration of a
 * potentially different module. As the [org.jetbrains.kotlin.fir.resolve.providers.FirProvider] of the use site knows nothing about files
 * of other modules, the file has to be looked up from the classifier the callable belongs to (KT-76840).
 *
 * For a SAM constructor that classifier is the functional interface it constructs; for a SAM constructor of a type alias it is the
 * expansion class, consistently with [functionalInterface][org.jetbrains.kotlin.analysis.api.symbols.functionalInterface]. For the rest it
 * is the containing class, consistently with the containing module of a callable
 * (see `org.jetbrains.kotlin.analysis.api.fir.utils.getContainingKtModule`).
 */
private fun FirCallableDeclaration.getContainingFileByClassifier(): FirFile? {
    val classifier = when (origin) {
        FirDeclarationOrigin.SamConstructor -> returnTypeRef.coneTypeOrNull?.toRegularClassSymbol(moduleData.session)?.fir
        else -> getContainingClass()
    }

    return classifier?.getContainingFile()
}
