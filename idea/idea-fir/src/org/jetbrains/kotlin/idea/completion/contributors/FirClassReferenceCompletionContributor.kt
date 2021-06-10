/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirCallableReferencePositionContext
import org.jetbrains.kotlin.idea.completion.createKeywordElement
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.platform.jvm.isJvm

internal class FirClassReferenceCompletionContributor(
    basicContext: FirBasicCompletionContext,
    priority: Int
) : FirCompletionContributorBase<FirCallableReferencePositionContext>(basicContext, priority) {
    override fun KtAnalysisSession.complete(positionContext: FirCallableReferencePositionContext) {
        if (positionContext.explicitReceiver == null) return
        sink.addElement(createKeywordElement("class"))
        if (targetPlatform.isJvm()) {
            sink.addElement(createKeywordElement("class", tail = ".java"))
        }
    }
}
