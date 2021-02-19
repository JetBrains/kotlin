/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.idea.frontend.api.assertIsValidAndAccessible
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.trackers.createProjectWideOutOfBlockModificationTracker
import java.util.concurrent.ConcurrentHashMap

@OptIn(InvalidWayOfUsingAnalysisSession::class)
class KtFirAnalysisSessionProvider(project: Project) : KtAnalysisSessionProvider() {
    private val analysisSessionByModuleInfoCache =
        CachedValuesManager.getManager(project).createCachedValue {
            CachedValueProvider.Result(
                ConcurrentHashMap<ModuleInfo, KtAnalysisSession>(),
                PsiModificationTracker.MODIFICATION_COUNT,
                ProjectRootModificationTracker.getInstance(project),
                project.createProjectWideOutOfBlockModificationTracker()
            )
        }

    override fun getAnalysisSessionFor(contextElement: KtElement): KtAnalysisSession {
        return analysisSessionByModuleInfoCache.value.getOrPut(contextElement.getModuleInfo()) {
            @Suppress("DEPRECATION")
            KtFirAnalysisSession.createForElement(contextElement)
        }
    }

    internal fun getAnalysisSessionByResolveState(firModuleResolveState: FirModuleResolveState): KtAnalysisSession =
        analysisSessionByModuleInfoCache.value.getOrPut(firModuleResolveState.moduleInfo) {
            KtFirAnalysisSession.createAnalysisSessionByResolveState(firModuleResolveState)
        }

    @TestOnly
    fun clearCaches() {
        analysisSessionByModuleInfoCache.value.clear()
    }
}