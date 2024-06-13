/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.ApplicationServiceRegistration
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalModificationService
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModuleStructure
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

abstract class AnalysisApiTestConfigurator {
    open val testPrefix: String? get() = null

    abstract val frontendKind: FrontendKind

    abstract val analyseInDependentSession: Boolean

    abstract fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable)

    abstract val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>

    open fun prepareFilesInModule(ktTestModule: KtTestModule, testServices: TestServices) {}

    open fun doGlobalModuleStateModification(project: Project) {
        KotlinGlobalModificationService.getInstance(project).publishGlobalModuleStateModification()
    }

    open fun computeTestDataPath(path: Path): Path = path

    abstract fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): KtTestModuleStructure

    fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
        serviceRegistrars.forEach { it.registerProjectExtensionPoints(project, testServices) }
    }

    fun registerProjectServices(project: MockProject, testServices: TestServices) {
        serviceRegistrars.forEach { it.registerProjectServices(project, testServices) }
    }

    @OptIn(TestInfrastructureInternals::class)
    fun registerProjectModelServices(project: MockProject, testServices: TestServices) {
        serviceRegistrars.forEach { it.registerProjectModelServices(project, testServices.testConfiguration.rootDisposable, testServices) }
    }

    fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        ApplicationServiceRegistration.register(application, serviceRegistrars, testServices)
    }
}
