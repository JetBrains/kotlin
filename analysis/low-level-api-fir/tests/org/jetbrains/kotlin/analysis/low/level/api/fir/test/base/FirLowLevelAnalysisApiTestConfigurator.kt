/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.SealedClassesInheritorsCaclulatorPreAnalysisHandler
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.test.framework.project.structure.toKtSourceModules
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiBaseTestServiceRegistrar

object FirLowLevelAnalysisApiTestConfigurator : AnalysisApiTestConfigurator() {
    override val analyseInDependentSession: Boolean get() = false

    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        builder.apply {
            usePreAnalysisHandlers(::SealedClassesInheritorsCaclulatorPreAnalysisHandler)
        }
    }

    override val serviceRegistrars: List<AnalysisApiTestServiceRegistrar> = listOf(
        AnalysisApiBaseTestServiceRegistrar,
        AnalysisApiFirTestServiceRegistrar,
    )

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project
    ): List<KtModuleWithFiles> {
        return moduleStructure.toKtSourceModules(testServices, project)
    }

    override fun doOutOfBlockModification(file: KtFile) {
        ServiceManager.getService(file.project, KotlinModificationTrackerFactory::class.java)
            .incrementModificationsCount()
    }
}
