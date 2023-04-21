/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiBaseTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiDecompiledCodeTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.analysis.project.structure.allDirectDependenciesOfType
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtSourceModuleByCompilerConfiguration
import org.jetbrains.kotlin.analysis.test.framework.project.structure.TestModuleStructureFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind

object FirLowLevelCompilerBasedTestConfigurator : AnalysisApiTestConfigurator() {
    override val analyseInDependentSession: Boolean get() = false
    override val frontendKind: FrontendKind get() = FrontendKind.Fir

    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        builder.apply {
        }
    }

    override val serviceRegistrars: List<AnalysisApiTestServiceRegistrar> = listOf(
        AnalysisApiBaseTestServiceRegistrar,
        AnalysisApiDecompiledCodeTestServiceRegistrar,
        AnalysisApiFirTestServiceRegistrar,
    )

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project
    ): KtModuleProjectStructure {
        val mainModules = moduleStructure.modules.map { testModule ->
            val files = TestModuleStructureFactory.createSourcePsiFiles(testModule, testServices, project)
            KtModuleWithFiles(
                KtSourceModuleByCompilerConfiguration(project, testModule, files, testServices),
                files
            )
        }
        return KtModuleProjectStructure(
            mainModules = mainModules,
            binaryModules = mainModules.asSequence().flatMap { it.ktModule.allDirectDependenciesOfType<KtBinaryModule>() }.asIterable(),
        )
    }

    override fun doGlobalModuleStateModification(project: Project) {
        error("Should not be called for compiler based tests")
    }
}
