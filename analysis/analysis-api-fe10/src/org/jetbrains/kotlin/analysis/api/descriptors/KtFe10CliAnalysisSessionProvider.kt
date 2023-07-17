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
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenFactory
import org.jetbrains.kotlin.psi.KtElement
import java.lang.UnsupportedOperationException


@OptIn(KtAnalysisApiInternals::class)
class KtFe10AnalysisSessionProvider(project: Project) : KtAnalysisSessionProvider(project) {
    override fun getAnalysisSession(useSiteKtElement: KtElement): KtAnalysisSession {
        return KtFe10AnalysisSession(project, useSiteKtElement, tokenFactory.create(project))
    }

    override fun getAnalysisSessionByUseSiteKtModule(useSiteKtModule: KtModule): KtAnalysisSession {
        throw UnsupportedOperationException("getAnalysisSessionByModule() should not be used on KtFe10AnalysisSession")
    }

    override fun clearCaches() {}
}