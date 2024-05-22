/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.sessions.KaBaseSessionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.psi.KtElement

internal class KaFe10SessionProvider(project: Project) : KaBaseSessionProvider(project) {
    override fun getAnalysisSession(useSiteKtElement: KtElement): KaSession {
        val facade = Fe10AnalysisFacade.getInstance(project)
        val token = tokenFactory.create(project, project.createProjectWideOutOfBlockModificationTracker())
        val context = facade.getAnalysisContext(useSiteKtElement, token)
        val useSiteModule = ProjectStructureProvider.getModule(project, useSiteKtElement, contextualModule = null)
        return KaFe10Session(context, useSiteModule, token)
    }

    override fun getAnalysisSessionByUseSiteKtModule(useSiteKtModule: KtModule): KaSession {
        val facade = Fe10AnalysisFacade.getInstance(project)
        val token = tokenFactory.create(project, project.createProjectWideOutOfBlockModificationTracker())
        val context = facade.getAnalysisContext(useSiteKtModule, token)
        return KaFe10Session(context, useSiteKtModule, token)
    }

    override fun clearCaches() {}
}