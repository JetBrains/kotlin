/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.sessions.KaBaseSessionProvider
import org.jetbrains.kotlin.analysis.api.impl.base.sessions.KaGlobalSearchScope
import org.jetbrains.kotlin.analysis.api.impl.base.util.createSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.platform.modification.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.psi.KtElement

internal class KaFe10SessionProvider(project: Project) : KaBaseSessionProvider(project) {
    override fun getAnalysisSession(useSiteKtElement: KtElement): KaSession {
        val facade = Fe10AnalysisFacade.getInstance(project)
        val token = tokenFactory.create(project, project.createProjectWideOutOfBlockModificationTracker())
        val context = facade.getAnalysisContext(useSiteKtElement, token)
        val useSiteModule = ProjectStructureProvider.getModule(project, useSiteKtElement, contextualModule = null)
        return createSession(context, useSiteModule, token)
    }

    override fun getAnalysisSessionByUseSiteKtModule(useSiteKtModule: KtModule): KaSession {
        val facade = Fe10AnalysisFacade.getInstance(project)
        val token = tokenFactory.create(project, project.createProjectWideOutOfBlockModificationTracker())
        val context = facade.getAnalysisContext(useSiteKtModule, token)
        return createSession(context, useSiteKtModule, token)
    }

    private fun createSession(context: Fe10AnalysisContext, useSiteModule: KtModule, token: KaLifetimeToken): KaFe10Session {
        return createSession {
            val resolutionScope = KaGlobalSearchScope(shadowedScope = GlobalSearchScope.EMPTY_SCOPE, useSiteModule)
            KaFe10Session(context, useSiteModule, token, analysisSessionProvider, resolutionScope)
        }
    }

    override fun clearCaches() {}
}