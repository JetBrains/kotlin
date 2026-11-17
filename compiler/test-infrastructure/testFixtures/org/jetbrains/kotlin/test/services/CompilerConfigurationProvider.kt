/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.*

abstract class CompilerConfigurationProvider(val testServices: TestServices) : TestService {
    abstract val testRootDisposable: Disposable
    abstract val configurators: List<AbstractEnvironmentConfigurator>

    protected abstract fun getKotlinCoreEnvironment(module: TestModule): KotlinCoreEnvironment

    abstract fun getCompilerConfiguration(module: TestModule, compilationStage: CompilationStage): CompilerConfiguration

    context(handler: AnalysisHandler<*>)
    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration =
        getCompilerConfiguration(module, handler.compilationStage)

    context(_: FrontendFacade<*>)
    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration =
        getCompilerConfiguration(module, CompilationStage.FIRST)

    context(_: Frontend2BackendConverter<*, *>)
    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration =
        getCompilerConfiguration(module, CompilationStage.FIRST)

    context(_: IrPreSerializationLoweringFacade<*>)
    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration =
        getCompilerConfiguration(module, CompilationStage.FIRST)

    context(backendFacade: BackendFacade<*, *>)
    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration =
        getCompilerConfiguration(module, backendFacade.outputKind.producedBy)

    context(_: DeserializerFacade<BinaryArtifacts.KLib, *>)
    fun getCompilerConfiguration(module: TestModule): CompilerConfiguration = getCompilerConfiguration(module, CompilationStage.SECOND)

    open fun getProject(module: TestModule): Project {
        return getKotlinCoreEnvironment(module).project
    }

    fun registerCompilerExtensions(
        extensionStorage: CompilerPluginRegistrar.ExtensionStorage,
        module: TestModule,
        configuration: CompilerConfiguration,
    ) {
        for (configurator in configurators) {
            with(configurator) {
                extensionStorage.registerCompilerExtensions(module, configuration)
            }
        }
    }

    fun configureProject(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        for (configurator in configurators) {
            configurator.legacyRegisterCompilerExtensions(project, module, configuration)
        }
    }

    open fun getPackagePartProviderFactory(module: TestModule): (GlobalSearchScope) -> JvmPackagePartProvider {
        return getKotlinCoreEnvironment(module)::createPackagePartProvider
    }
}

val TestServices.compilerConfigurationProvider: CompilerConfigurationProvider by TestServices.testServiceAccessor()
