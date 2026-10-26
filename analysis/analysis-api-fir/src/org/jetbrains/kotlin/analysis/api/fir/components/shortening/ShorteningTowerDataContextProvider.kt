/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.shortening

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector
import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

internal class ShorteningTowerDataContextProvider private constructor(
    private val contextProvider: ContextCollector.ContextProvider
) {
    companion object {
        fun create(resolutionFacade: LLResolutionFacade, targetElement: KtElement): ShorteningTowerDataContextProvider {
            val firFile = targetElement.containingKtFile.getOrBuildFirFile(resolutionFacade)

            val contextProvider = ContextCollector.process(
                firFile,
                targetElement,
                preferBodyContext = false, // we only query SELF context
                shouldTriggerBodyAnalysis = true,
                filter = { ContextCollector.FilterResponse.CONTINUE }
            )

            return ShorteningTowerDataContextProvider(contextProvider)
        }
    }

    fun getClosestAvailableParentContext(ktElement: KtElement): FirTowerDataContext? {
        for (parent in ktElement.parentsWithSelf) {
            val context = contextProvider[parent, ContextCollector.ContextKind.SELF]

            if (context != null) {
                return context.towerDataContext
            }
        }

        return null
    }
}
