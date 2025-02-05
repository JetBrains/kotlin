/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.registerApplicationServices
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.registerProjectExtensionPoints
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.registerProjectServices
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.services.TestServices

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // Use `testServices` name instead of `data`
abstract class AnalysisApiTestServiceRegistrar : AnalysisApiServiceRegistrar<TestServices> {
    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {}

    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {}

    override fun registerProjectServices(project: MockProject, testServices: TestServices) {}

    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {}
}

@OptIn(TestInfrastructureInternals::class)
fun List<AnalysisApiServiceRegistrar<TestServices>>.registerProjectModelServices(project: MockProject, testServices: TestServices) {
    forEach { it.registerProjectModelServices(project, testServices.testConfiguration.rootDisposable, testServices) }
}

fun List<AnalysisApiServiceRegistrar<TestServices>>.registerAllServices(
    application: MockApplication,
    project: MockProject,
    testServices: TestServices,
) {
    registerApplicationServices(application, testServices)
    registerProjectExtensionPoints(project, testServices)
    registerProjectServices(project, testServices)
    registerProjectModelServices(project, testServices)
}
