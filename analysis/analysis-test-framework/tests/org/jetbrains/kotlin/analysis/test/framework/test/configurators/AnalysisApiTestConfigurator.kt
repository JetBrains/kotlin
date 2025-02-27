/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.test.configurators

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.modification.publishGlobalModuleStateModificationEvent
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleStructure
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import java.nio.file.Path

abstract class AnalysisApiTestConfigurator {
    /**
     * @see org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest.assertEqualsToTestDataFileSibling
     */
    open val testPrefixes: List<String> get() = emptyList()

    abstract val frontendKind: FrontendKind

    abstract val analyseInDependentSession: Boolean

    /**
     * The platform used by default, in case if no platform is specified in the test data file.
     */
    open val defaultTargetPlatform: TargetPlatform
        get() = JvmPlatforms.defaultJvmPlatform

    abstract fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable)

    abstract val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>

    open fun prepareFilesInModule(ktTestModule: KtTestModule, testServices: TestServices) {}

    open fun doGlobalModuleStateModification(project: Project) {
        runWriteAction {
            project.publishGlobalModuleStateModificationEvent()
        }
    }

    open fun computeTestDataPath(path: Path): Path = path

    abstract fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): KtTestModuleStructure
}
