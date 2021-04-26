/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateForCompletion
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateImpl
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

object LowLevelFirApiFacadeForCompletion {
    fun getResolveStateForCompletion(originalState: FirModuleResolveState): FirModuleResolveState {
        check(originalState is FirModuleResolveStateImpl)
        return FirModuleResolveStateForCompletion(originalState.project, originalState)
    }

    fun recordCompletionContextForDeclaration(
        firFile: FirFile,
        fakeKtDeclaration: KtDeclaration,
        originalKtDeclaration: KtDeclaration,
        fakeContainingFile: KtFile,
        state: FirModuleResolveState,
    ) {
        val originalFirDeclaration = originalKtDeclaration.getOrBuildFirOfType<FirDeclaration>(state)
        val fakeFirDeclaration = DeclarationCopyBuilder.createDeclarationCopy(originalFirDeclaration, fakeKtDeclaration, state)

        state.lazyResolveDeclarationForCompletion(fakeFirDeclaration, firFile)
        state.recordPsiToFirMappingsForCompletionFrom(fakeFirDeclaration, firFile, fakeContainingFile)
    }
}
