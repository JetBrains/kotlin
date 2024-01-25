/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.psi.KtElement


@OptIn(KtAnalysisApiInternals::class)
class KtFe10AnalysisSessionProvider(project: Project) : KtAnalysisSessionProvider(project) {
    override fun getAnalysisSession(useSiteKtElement: KtElement): KtAnalysisSession {
        val facade = Fe10AnalysisFacade.getInstance(project)
        val token = tokenFactory.create(project)
        val context = facade.getAnalysisContext(useSiteKtElement, token)
        val useSiteModule = ProjectStructureProvider.getModule(project, useSiteKtElement, contextualModule = null)
        return KtFe10AnalysisSession(context, useSiteModule, token)
    }

    override fun getAnalysisSessionByUseSiteKtModule(useSiteKtModule: KtModule): KtAnalysisSession {
        val facade = Fe10AnalysisFacade.getInstance(project)
        val token = tokenFactory.create(project)
        val context = facade.getAnalysisContext(useSiteKtModule, token)
        return KtFe10AnalysisSession(context, useSiteKtModule, token)
    }

    override fun clearCaches() {}
}