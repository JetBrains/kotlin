/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.project.structure.KtBuiltinsModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.test.getAnalyzerServices
import org.jetbrains.kotlin.test.services.*

abstract class AnalysisApiEnvironmentManager : TestService {
    abstract val testServices: TestServices
    abstract val testRootDisposable: Disposable
    abstract fun initializeEnvironment()

    fun getProject(): Project =
        getProjectEnvironment().project

    fun getApplication(): Application =
        getApplicationEnvironment().application

    abstract fun initializeProjectStructure()
    abstract fun getProjectEnvironment(): KotlinCoreProjectEnvironment
    abstract fun getApplicationEnvironment(): KotlinCoreApplicationEnvironment
}

class AnalysisApiEnvironmentManagerImpl(
    override val testServices: TestServices,
    override val testRootDisposable: Disposable,
) : AnalysisApiEnvironmentManager() {
    private val _projectEnvironment: KotlinCoreProjectEnvironment by lazy {
        StandaloneProjectFactory.createProjectEnvironment(
            testRootDisposable,
            KotlinCoreApplicationEnvironmentMode.UnitTest,
        )
    }

    override fun initializeEnvironment() {
        testServices.disposableProvider.registerDisposables(
            projectDisposable = _projectEnvironment.parentDisposable,
            applicationDisposable = _projectEnvironment.environment.parentDisposable,
        )
    }

    override fun initializeProjectStructure() {
        val ktModuleProjectStructure = testServices.ktModuleProvider.getModuleStructure()
        val useSiteModule = testServices.moduleStructure.modules.first()
        val useSiteCompilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(useSiteModule)
        val builtinsModule = KtBuiltinsModule(
            useSiteModule.targetPlatform,
            useSiteModule.targetPlatform.getAnalyzerServices(),
            getProject()
        )

        val globalLanguageVersionSettings = useSiteModule.languageVersionSettings

        StandaloneProjectFactory.registerServicesForProjectEnvironment(
            _projectEnvironment,
            KtTestProjectStructureProvider(globalLanguageVersionSettings, builtinsModule, ktModuleProjectStructure),
            useSiteCompilerConfiguration.languageVersionSettings,
            useSiteCompilerConfiguration.get(JVMConfigurationKeys.JDK_HOME)?.toPath(),
        )

        testServices.compilerConfigurationProvider.registerCompilerExtensions(getProject(), useSiteModule, useSiteCompilerConfiguration)
    }

    override fun getProjectEnvironment(): KotlinCoreProjectEnvironment =
        _projectEnvironment

    override fun getApplicationEnvironment(): KotlinCoreApplicationEnvironment =
        _projectEnvironment.environment as KotlinCoreApplicationEnvironment

}

val TestServices.environmentManager: AnalysisApiEnvironmentManager by TestServices.testServiceAccessor()
