/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fe10.test.configurator

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.api.descriptors.CliFe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10AnalysisHandlerExtension
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.PluginStructureProvider
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.cli.common.CliModuleVisibilityManagerImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.references.fe10.base.DummyKtFe10ReferenceResolutionHelper
import org.jetbrains.kotlin.references.fe10.base.KtFe10ReferenceResolutionHelper
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiFe10TestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    private const val PLUGIN_RELATIVE_PATH = "/META-INF/analysis-api/analysis-api-fe10.xml"

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        PluginStructureProvider.registerApplicationServices(application, PLUGIN_RELATIVE_PATH)
        application.registerService(KtFe10ReferenceResolutionHelper::class.java, DummyKtFe10ReferenceResolutionHelper)
    }

    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
        AnalysisHandlerExtension.registerExtensionPoint(project)
        PluginStructureProvider.registerProjectExtensionPoints(project, PLUGIN_RELATIVE_PATH)
    }

    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        PluginStructureProvider.registerProjectServices(project, PLUGIN_RELATIVE_PATH)
        PluginStructureProvider.registerProjectListeners(project, PLUGIN_RELATIVE_PATH)
    }

    @OptIn(TestInfrastructureInternals::class)
    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {
        project.apply {
            registerService(Fe10AnalysisFacade::class.java, CliFe10AnalysisFacade())
            registerService(ModuleVisibilityManager::class.java, CliModuleVisibilityManagerImpl(enabled = true))
        }

        testServices.ktTestModuleStructure.mainModules.forEach { ktTestModule ->
            val sourceModule = ktTestModule.ktModule as? KaSourceModule ?: return@forEach
            AnalysisHandlerExtension.registerExtension(project, KaFe10AnalysisHandlerExtension(sourceModule))
        }

        KotlinCoreEnvironment.registerKotlinLightClassSupport(project)
    }
}
