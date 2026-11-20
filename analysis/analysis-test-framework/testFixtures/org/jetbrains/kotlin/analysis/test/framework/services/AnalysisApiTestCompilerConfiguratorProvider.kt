/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

class AnalysisApiTestCompilerConfiguratorProvider(
    testServices: TestServices,
    override val testRootDisposable: Disposable,
    override val configurators: List<AbstractEnvironmentConfigurator>
) : CompilerConfigurationProvider(testServices) {
    private val configurationCache: MutableMap<Pair<TestModule, CompilationStage>, CompilerConfiguration> = mutableMapOf()

    override fun getProject(module: TestModule): Project {
        return testServices.environmentManager.getProject()
    }

    override fun getCompilerConfiguration(module: TestModule, compilationStage: CompilationStage): CompilerConfiguration {
        return configurationCache.getOrPut(module to compilationStage) {
            createKotlinCompilerConfiguration(module, compilationStage)
        }
    }

    override fun getKotlinCoreEnvironment(module: TestModule): KotlinCoreEnvironment {
        error("Should not be called")
    }

    @OptIn(TestInfrastructureInternals::class)
    private fun createKotlinCompilerConfiguration(module: TestModule, compilationStage: CompilationStage): CompilerConfiguration {
        return createCompilerConfiguration(testServices, module, configurators, compilationStage)
    }
}
