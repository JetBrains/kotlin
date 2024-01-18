/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.base

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.project.structure.AnalysisApiKtModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.project.structure.AnalysisApiKtModuleProviderImpl
import org.jetbrains.kotlin.analysis.test.framework.project.structure.AnalysisApiTestCodeFragmentDirectives
import org.jetbrains.kotlin.analysis.test.framework.services.*
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.test.ExecutionListenerBasedDisposableProvider
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.ApplicationDisposableProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.jetbrains.kotlin.utils.bind

@OptIn(TestInfrastructureInternals::class)
fun TestConfigurationBuilder.registerAnalysisApiBaseTestServices(
    testDisposable: Disposable,
    configurator: AnalysisApiTestConfigurator,
) {
    useAdditionalService<TestDisposableProvider>(::TestDisposableProviderImpl)
    useAdditionalService<AnalysisApiKtModuleProvider>(::AnalysisApiKtModuleProviderImpl)
    useAdditionalService<AnalysisApiEnvironmentManager>(::AnalysisApiEnvironmentManagerImpl.bind(testDisposable))
    useAdditionalService<ApplicationDisposableProvider> { ExecutionListenerBasedDisposableProvider() }
    useAdditionalService<KotlinStandardLibrariesPathProvider> { StandardLibrariesPathProviderForKotlinProject }

    useCustomCompilerConfigurationProvider(::AnalysisApiTestCompilerConfiguratorProvider)
    usePreAnalysisHandlers(::ProjectStructureInitialisationPreAnalysisHandler.bind(configurator))
    useDirectives(AnalysisApiTestDirectives)
    useDirectives(AnalysisApiTestCodeFragmentDirectives)
}