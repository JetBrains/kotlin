/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.impl.base.projectStructure.KaBuiltinsModuleImpl
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInDecompilationInterceptor
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
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

@OptIn(KaAnalysisApiInternals::class)
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
        KotlinCoreEnvironment.underApplicationLock {
            _projectEnvironment.environment.application.apply {
                if (getServiceIfCreated(BuiltinsVirtualFileProvider::class.java) == null) {
                    registerService(BuiltinsVirtualFileProvider::class.java, BuiltinsVirtualFileProviderTestImpl())
                }
                if (getServiceIfCreated(KotlinBuiltInDecompilationInterceptor::class.java) == null) {
                    registerService(
                        KotlinBuiltInDecompilationInterceptor::class.java,
                        KotlinBuiltInDecompilationInterceptorTestImpl::class.java
                    )
                }
            }
        }
    }

    override fun initializeProjectStructure() {
        val ktTestModuleStructure = testServices.ktTestModuleStructure
        val useSiteModule = testServices.moduleStructure.modules.first()
        val useSiteCompilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(useSiteModule)
        val builtinsModule = KaBuiltinsModuleImpl(useSiteModule.targetPlatform, getProject())

        val globalLanguageVersionSettings = useSiteModule.languageVersionSettings

        StandaloneProjectFactory.registerServicesForProjectEnvironment(
            _projectEnvironment,
            KotlinTestProjectStructureProvider(globalLanguageVersionSettings, builtinsModule, ktTestModuleStructure),
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
