/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import com.intellij.openapi.components.service
import org.jetbrains.kotlin.idea.frontend.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.EntityWasGarbageCollectedException
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol

@OptIn(InvalidWayOfUsingAnalysisSession::class)
internal inline fun <R> analyzeWithSymbolAsContext(
    contextSymbol: KtSymbol,
    action: KtAnalysisSession.() -> R
): R {
    require(contextSymbol is KtFirSymbol<*>)
    val resolveState = contextSymbol.firRef.resolveState
    val token = contextSymbol.token
    val analysisSessionProvider = resolveState.project.service<KtAnalysisSessionProvider>()
    check(analysisSessionProvider is KtFirAnalysisSessionProvider)
    val analysisSession = analysisSessionProvider.getCachedAnalysisSession(resolveState, token)
        ?: throw EntityWasGarbageCollectedException("KtAnalysisSession")
    return action(analysisSession)
}
