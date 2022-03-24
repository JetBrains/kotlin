/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fe10.test.configurator

import com.intellij.core.CoreJavaFileManager
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.psi.impl.file.impl.JavaFileManager
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.descriptors.CliFe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisHandlerExtension
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.descriptors.references.base.KtFe10KotlinReferenceProviderContributor
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.cli.common.CliModuleVisibilityManagerImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.idea.references.KotlinReferenceProviderContributor
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiFe10TestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
        AnalysisHandlerExtension.registerExtensionPoint(project)
    }

    @OptIn(InvalidWayOfUsingAnalysisSession::class)
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(KtAnalysisSessionProvider::class.java, KtFe10AnalysisSessionProvider())
            registerService(Fe10AnalysisFacade::class.java, CliFe10AnalysisFacade(project))
            registerService(ModuleVisibilityManager::class.java, CliModuleVisibilityManagerImpl(enabled = true))
            registerService(CoreJavaFileManager::class.java, project.getService(JavaFileManager::class.java) as CoreJavaFileManager)
        }
        AnalysisHandlerExtension.registerExtension(project, KtFe10AnalysisHandlerExtension())
        KotlinCoreEnvironment.registerKotlinLightClassSupport(project)
    }

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        application.apply {
            registerService(KotlinReferenceProviderContributor::class.java, KtFe10KotlinReferenceProviderContributor::class.java)
        }
    }
}