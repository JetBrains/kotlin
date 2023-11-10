/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fe10.test.configurator

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.descriptors.CliFe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisHandlerExtension
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.descriptors.references.ReadWriteAccessCheckerDescriptorsImpl
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.cli.common.CliModuleVisibilityManagerImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.idea.references.KotlinReferenceProviderContributor
import org.jetbrains.kotlin.idea.references.ReadWriteAccessChecker
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.references.fe10.base.KtFe10KotlinReferenceProviderContributor
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiFe10TestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
        AnalysisHandlerExtension.registerExtensionPoint(project)
    }

    @OptIn(KtAnalysisApiInternals::class)
    override fun registerProjectModelServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(KtAnalysisSessionProvider::class.java, KtFe10AnalysisSessionProvider(project))
            registerService(Fe10AnalysisFacade::class.java, CliFe10AnalysisFacade())
            registerService(ModuleVisibilityManager::class.java, CliModuleVisibilityManagerImpl(enabled = true))

            registerService(ReadWriteAccessChecker::class.java, ReadWriteAccessCheckerDescriptorsImpl())
            registerService(KotlinReferenceProviderContributor::class.java, KtFe10KotlinReferenceProviderContributor::class.java)
        }
        testServices.ktModuleProvider.getModuleStructure().mainModules.forEach { module ->
            val sourceModule = module.ktModule as? KtSourceModule ?: return@forEach
            AnalysisHandlerExtension.registerExtension(project, KtFe10AnalysisHandlerExtension(sourceModule))
        }
        KotlinCoreEnvironment.registerKotlinLightClassSupport(project)
    }
}
