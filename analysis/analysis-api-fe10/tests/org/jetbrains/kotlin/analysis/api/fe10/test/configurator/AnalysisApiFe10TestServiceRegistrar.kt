/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fe10.test.configurator

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.descriptors.CliFe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisHandlerExtension
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.PluginStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.cli.common.CliModuleVisibilityManagerImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.services.TestServices

@OptIn(KtAnalysisNonPublicApi::class)
object AnalysisApiFe10TestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    private const val PLUGIN_RELATIVE_PATH = "/META-INF/analysis-api/analysis-api-fe10.xml"

    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
        AnalysisHandlerExtension.registerExtensionPoint(project)
        PluginStructureProvider.registerProjectExtensionPoints(project, PLUGIN_RELATIVE_PATH)
    }

    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        PluginStructureProvider.registerProjectServices(project, PLUGIN_RELATIVE_PATH)
        PluginStructureProvider.registerProjectListeners(project, PLUGIN_RELATIVE_PATH)
    }

    @OptIn(KtAnalysisApiInternals::class, TestInfrastructureInternals::class)
    override fun registerProjectModelServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(Fe10AnalysisFacade::class.java, CliFe10AnalysisFacade())
            registerService(ModuleVisibilityManager::class.java, CliModuleVisibilityManagerImpl(enabled = true))
        }

        testServices.ktTestModuleStructure.mainModules.forEach { ktTestModule ->
            val sourceModule = ktTestModule.ktModule as? KtSourceModule ?: return@forEach
            AnalysisHandlerExtension.registerExtension(project, KtFe10AnalysisHandlerExtension(sourceModule))
        }

        KotlinCoreEnvironment.registerKotlinLightClassSupport(project)
    }
}
