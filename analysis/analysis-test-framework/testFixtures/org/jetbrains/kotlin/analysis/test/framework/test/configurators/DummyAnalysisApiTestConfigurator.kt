/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.test.configurators

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleStructure
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

/**
 * A dummy configurator that does nothing. No files are configured by default.
 */
object DummyAnalysisApiTestConfigurator : AnalysisApiTestConfigurator {
    override val analysisApiMode: AnalysisApiMode get() = AnalysisApiMode.Ide
    override val frontendKind: FrontendKind get() = FrontendKind.Fir
    override val analyseInDependentSession: Boolean get() = false
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {}
    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> get() = emptyList()

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): KtTestModuleStructure = KtTestModuleStructure(
        testModuleStructure = moduleStructure,
        mainModules = emptyList(),
        binaryModules = emptyList(),
    )
}
