/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktTestModuleStructure
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AbstractEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.CompilerConfigurationProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.createCompilerConfiguration

class AnalysisApiTestCompilerConfiguratorProvider(
    testServices: TestServices,
    override val testRootDisposable: Disposable,
    override val configurators: List<AbstractEnvironmentConfigurator>
) : CompilerConfigurationProvider(testServices) {
    private val configurationCache: MutableMap<TestModule, CompilerConfiguration> = mutableMapOf()

    private val allProjectBinaryRoots by lazy {
        StandaloneProjectFactory.getAllBinaryRoots(
            testServices.ktTestModuleStructure.mainAndBinaryKtModules,
            testServices.environmentManager.getProjectEnvironment()
        )
    }

    override fun getProject(module: TestModule): Project {
        return testServices.environmentManager.getProject()
    }

    override fun getCompilerConfiguration(module: TestModule): CompilerConfiguration {
        return configurationCache.getOrPut(module) {
            createKotlinCompilerConfiguration(module)
        }
    }

    override fun getPackagePartProviderFactory(module: TestModule): (GlobalSearchScope) -> JvmPackagePartProvider {
        val configuration = getCompilerConfiguration(module)

        return { scope ->
            JvmPackagePartProvider(configuration.languageVersionSettings, scope).apply {
                addRoots(allProjectBinaryRoots, configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY))
            }
        }
    }

    override fun getKotlinCoreEnvironment(module: TestModule): KotlinCoreEnvironment {
        error("Should not be called")
    }

    @OptIn(TestInfrastructureInternals::class)
    private fun createKotlinCompilerConfiguration(module: TestModule): CompilerConfiguration {
        return createCompilerConfiguration(module, configurators)
    }
}