/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.sessions.KaBaseSessionProvider
import org.jetbrains.kotlin.analysis.api.impl.base.util.createSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.platform.modification.createProjectWideSourceModificationTracker
import org.jetbrains.kotlin.psi.KtElement

internal class KaFe10SessionProvider(project: Project) : KaBaseSessionProvider(project) {
    override fun getAnalysisSession(useSiteElement: KtElement): KaSession {
        val useSiteModule = KotlinProjectStructureProvider.getModule(project, useSiteElement, useSiteModule = null)
        checkUseSiteModule(useSiteModule)

        val facade = Fe10AnalysisFacade.getInstance(project)
        val token = tokenFactory.create(project, project.createProjectWideSourceModificationTracker())
        val context = facade.getAnalysisContext(useSiteElement, token)
        return createSession(context, useSiteModule, token)
    }

    override fun getAnalysisSession(useSiteModule: KaModule): KaSession {
        checkUseSiteModule(useSiteModule)

        val facade = Fe10AnalysisFacade.getInstance(project)
        val token = tokenFactory.create(project, project.createProjectWideSourceModificationTracker())
        val context = facade.getAnalysisContext(useSiteModule, token)
        return createSession(context, useSiteModule, token)
    }

    private fun createSession(context: Fe10AnalysisContext, useSiteModule: KaModule, token: KaLifetimeToken): KaFe10Session {
        return createSession {
            val resolutionScope = KaResolutionScope.forModule(useSiteModule)
            KaFe10Session(context, useSiteModule, token, analysisSessionProvider, resolutionScope)
        }
    }

    override fun clearCaches() {}
}
