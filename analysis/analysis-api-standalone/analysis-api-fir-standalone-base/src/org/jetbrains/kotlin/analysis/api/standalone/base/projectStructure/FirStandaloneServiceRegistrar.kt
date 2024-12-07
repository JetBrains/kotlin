/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.impl.PsiElementFinderImpl
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder

@OptIn(KaImplementationDetail::class)
object FirStandaloneServiceRegistrar : AnalysisApiSimpleServiceRegistrar() {
    private const val PLUGIN_RELATIVE_PATH = "/META-INF/analysis-api/analysis-api-fir-standalone-base.xml"

    override fun registerApplicationServices(application: MockApplication) {
        PluginStructureProvider.registerApplicationServices(application, PLUGIN_RELATIVE_PATH)
    }

    override fun registerProjectExtensionPoints(project: MockProject) {
        PluginStructureProvider.registerProjectExtensionPoints(project, PLUGIN_RELATIVE_PATH)
    }

    override fun registerProjectServices(project: MockProject) {
        PluginStructureProvider.registerProjectServices(project, PLUGIN_RELATIVE_PATH)
        PluginStructureProvider.registerProjectListeners(project, PLUGIN_RELATIVE_PATH)
    }

    @Suppress("TestOnlyProblems")
    override fun registerProjectModelServices(project: MockProject, disposable: Disposable) {
        with(PsiElementFinder.EP.getPoint(project)) {
            registerExtension(JavaElementFinder(project), disposable)
            registerExtension(PsiElementFinderImpl(project), disposable)
        }
    }
}
