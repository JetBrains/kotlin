/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.FirStandaloneServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.FirSealedClassInheritorsProcessorFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.LLFirSealedClassInheritorsProcessorFactoryForTests
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.NoOpKtCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.PackagePartProviderTestImpl
import org.jetbrains.kotlin.analysis.project.structure.KtCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.providers.PackagePartProviderFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiFirTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
        FirStandaloneServiceRegistrar.registerProjectExtensionPoints(project)
    }

    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            FirStandaloneServiceRegistrar.registerProjectServices(project)

            registerService(FirSealedClassInheritorsProcessorFactory::class.java, LLFirSealedClassInheritorsProcessorFactoryForTests())
            registerService(PackagePartProviderFactory::class.java, PackagePartProviderTestImpl(testServices))
            registerService(KtCompilerPluginsProvider::class.java, NoOpKtCompilerPluginsProvider)
        }
    }

    @OptIn(TestInfrastructureInternals::class)
    override fun registerProjectModelServices(project: MockProject, testServices: TestServices) {
        FirStandaloneServiceRegistrar.registerProjectModelServices(project, testServices.testConfiguration.rootDisposable)
    }

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        FirStandaloneServiceRegistrar.registerApplicationServices(application)
    }
}
