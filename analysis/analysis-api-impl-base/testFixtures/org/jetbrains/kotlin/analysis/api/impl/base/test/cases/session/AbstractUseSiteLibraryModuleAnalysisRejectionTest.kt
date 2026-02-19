/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.session

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.impl.base.sessions.KaBaseUseSiteLibraryModuleAnalysisException
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.standalone.base.KotlinStandalonePlatformSettings
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Checks that the Analysis API correctly rejects analysis of library modules based on the
 * [org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings.allowUseSiteLibraryModuleAnalysis] setting.
 */
abstract class AbstractUseSiteLibraryModuleAnalysisRejectionTest : AbstractAnalysisApiBasedTest() {
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + listOf(LibraryModuleAnalysisTestServiceRegistrar)

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        val allowLibraryModuleAnalysis =
            mainModule.testModule.directives.singleOrZeroValue(Directives.ALLOW_LIBRARY_MODULE_ANALYSIS).toBoolean()

        val platformSettings = mainModule.platformSettings
        platformSettings.allowLibraryModuleAnalysis = allowLibraryModuleAnalysis

        val rejectionExpected = mainModule.testModule.directives.contains(Directives.EXPECT_REJECTION)
        var exceptionOccurred = false
        try {
            val libraryModule = mainModule.ktModule as? KaLibraryModule
                ?: error("Expected a `${KaLibraryModule::class.simpleName}` main module.")

            analyze(libraryModule) {
                // Nothing to do here.
            }
        } catch (_: KaBaseUseSiteLibraryModuleAnalysisException) {
            exceptionOccurred = true
        }

        testServices.assertions.assertEquals(rejectionExpected, exceptionOccurred) {
            "Expected use-site library module to be rejected: $rejectionExpected\nUse-site library module was rejected: $exceptionOccurred"
        }
    }

    private val KtTestModule.platformSettings: SwitchableKotlinAnalysisPlatformSettings
        get() = SwitchableKotlinAnalysisPlatformSettings.getInstance(this.ktModule.project)

    private object Directives : SimpleDirectivesContainer() {
        val ALLOW_LIBRARY_MODULE_ANALYSIS by stringDirective("Whether library module analysis is allowed.")
        val EXPECT_REJECTION by directive("Whether analysis should be rejected.")
    }
}

private object LibraryModuleAnalysisTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    @Suppress("UnstableApiUsage")
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.picoContainer.unregisterComponent(KotlinPlatformSettings::class.java.name)
        project.registerService(KotlinPlatformSettings::class.java, SwitchableKotlinAnalysisPlatformSettings())
    }
}

class SwitchableKotlinAnalysisPlatformSettings : KotlinStandalonePlatformSettings() {
    var allowLibraryModuleAnalysis: Boolean = true

    override val allowUseSiteLibraryModuleAnalysis: Boolean
        get() = allowLibraryModuleAnalysis

    companion object {
        fun getInstance(project: Project): SwitchableKotlinAnalysisPlatformSettings =
            KotlinPlatformSettings.getInstance(project) as SwitchableKotlinAnalysisPlatformSettings
    }
}
