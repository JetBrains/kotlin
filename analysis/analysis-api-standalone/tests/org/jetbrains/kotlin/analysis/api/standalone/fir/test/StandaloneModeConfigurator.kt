/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

object StandaloneModeConfigurator : AnalysisApiTestConfigurator() {
    override val analyseInDependentSession: Boolean get() = false

    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false).configureTest(builder, disposable)
    }

    override val serviceRegistrars: List<AnalysisApiTestServiceRegistrar>
        get() = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false).serviceRegistrars

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project
    ): KtModuleProjectStructure {
        return AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false).createModules(moduleStructure, testServices, project)
    }

    override fun doOutOfBlockModification(file: KtFile) {
        AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false).doOutOfBlockModification(file)
    }
}
