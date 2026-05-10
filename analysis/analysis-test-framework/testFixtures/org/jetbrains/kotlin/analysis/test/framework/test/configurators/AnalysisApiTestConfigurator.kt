/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.test.configurators

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.modification.publishGlobalModuleStateModificationEvent
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.test.data.manager.TestVariantChain
import org.jetbrains.kotlin.analysis.test.data.manager.withAdditionalVariant
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.services.MultiplatformTestOutputPrefixProvider
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import java.nio.file.Path

/**
 * Configures the test environment for Analysis API tests.
 *
 * Each configurator defines a specific combination of analysis mode and platform,
 * along with the services and module structure needed to run tests in that configuration.
 *
 * To create a modified version of an existing configurator (e.g., to add test prefixes),
 * use interface delegation:
 *
 * ```kotlin
 * object : AnalysisApiTestConfigurator by existingConfigurator {
 *     override val testPrefixes = listOf("myPrefix")
 * }
 * ```
 *
 * @see withAdditionalVariant
 */
interface AnalysisApiTestConfigurator {
    /**
     * Chain of test variant identifiers that determines output file naming and execution priority.
     *
     * @see org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest.assertEqualsToTestOutputFile
     * @see org.jetbrains.kotlin.analysis.test.data.manager.ManagedTest.variantChain
     */
    val testPrefixes: List<String>
        get() = MultiplatformTestOutputPrefixProvider.getPrefixes(defaultTargetPlatform)

    val analysisApiMode: AnalysisApiMode

    val analyseInDependentSession: Boolean

    /**
     * The platform used by default, in case if no platform is specified in the test data file.
     */
    val defaultTargetPlatform: TargetPlatform
        get() = defaultTargetPlatformValue

    fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable)

    val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>

    fun prepareFilesInModule(ktTestModule: KtTestModule, testServices: TestServices) {}

    fun doGlobalModuleStateModification(project: Project) {
        runWriteAction {
            project.publishGlobalModuleStateModificationEvent()
        }
    }

    fun computeTestDataPath(path: Path): Path = path

    fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): KtTestModuleStructure

    companion object {
        val defaultTargetPlatformValue: TargetPlatform
            get() = JvmPlatforms.defaultJvmPlatform
    }
}

/**
 * Returns a configurator that delegates everything to [this] but appends [variant] to [testPrefixes].
 *
 * @see TestVariantChain
 * @see TestVariantChain.withAdditionalVariant
 */
fun AnalysisApiTestConfigurator.withAdditionalVariant(variant: String): AnalysisApiTestConfigurator {
    val base = this
    return object : AnalysisApiTestConfigurator by base {
        override val testPrefixes: List<String>
            get() = base.testPrefixes.withAdditionalVariant(variant)
    }
}
