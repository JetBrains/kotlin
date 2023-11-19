/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

object StandaloneModeConfigurator : StandaloneModeConfiguratorBase() {

    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        sourceConfigurator.configureTest(builder, disposable)
    }

    private val sourceConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override val serviceRegistrars: List<AnalysisApiTestServiceRegistrar>
        get() = sourceConfigurator.serviceRegistrars + listOf(StandaloneModeTestServiceRegistrar)

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project
    ): KtModuleProjectStructure {
        return sourceConfigurator.createModules(moduleStructure, testServices, project)
    }
}
