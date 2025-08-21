/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.restrictedAnalysis

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.restrictedAnalysis.KotlinRestrictedAnalysisService
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractRestrictedAnalysisTest : AbstractAnalysisApiBasedTest() {
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + listOf(RestrictedAnalysisTestServiceRegistrar)

    protected val KtTestModule.restrictedAnalysisService: SwitchableRestrictedAnalysisService
        get() = SwitchableRestrictedAnalysisService.getInstance(this.ktModule.project)
}

private object RestrictedAnalysisTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.registerService(KotlinRestrictedAnalysisService::class.java, SwitchableRestrictedAnalysisService())
    }
}

class SwitchableRestrictedAnalysisService : KotlinRestrictedAnalysisService {
    var enableRestrictedAnalysisMode: Boolean = true
    var allowRestrictedAnalysis: Boolean = true

    override val isAnalysisRestricted: Boolean get() = enableRestrictedAnalysisMode
    override val isRestrictedAnalysisAllowed: Boolean get() = allowRestrictedAnalysis

    override fun rejectRestrictedAnalysis(): Nothing {
        throw RestrictedAnalysisNotAllowedException()
    }

    class RestrictedAnalysisNotAllowedException : RuntimeException("Restricted analysis is not allowed.")

    override fun <R> runWithRestrictedDataAccess(action: () -> R): R = action()

    companion object {
        fun getInstance(project: Project): SwitchableRestrictedAnalysisService =
            KotlinRestrictedAnalysisService.getInstance(project) as SwitchableRestrictedAnalysisService
    }
}
