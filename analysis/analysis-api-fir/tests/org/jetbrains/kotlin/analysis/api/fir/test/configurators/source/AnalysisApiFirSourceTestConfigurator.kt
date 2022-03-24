/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.configurators.source

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.fir.utils.configureOptionalTestCompilerPlugin
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiBaseTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AnalysisApiFirTestServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.FirLowLevelAnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.project.structure.toKtSourceModules
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

class AnalysisApiFirSourceTestConfigurator(override val analyseInDependentSession: Boolean) : AnalysisApiTestConfigurator() {
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        FirLowLevelAnalysisApiTestConfigurator.configureTest(builder, disposable)
        builder.apply {
            configureOptionalTestCompilerPlugin()
        }
    }

    override val serviceRegistrars: List<AnalysisApiTestServiceRegistrar> = listOf(
        AnalysisApiBaseTestServiceRegistrar,
        AnalysisApiFirTestServiceRegistrar,
    )

    override fun createModules(moduleStructure: TestModuleStructure, testServices: TestServices, project: Project): List<KtModuleWithFiles> {
        return moduleStructure.toKtSourceModules(testServices, project)
    }

    override fun doOutOfBlockModification(file: KtFile) {
        FirLowLevelAnalysisApiTestConfigurator.doOutOfBlockModification(file)
    }
}