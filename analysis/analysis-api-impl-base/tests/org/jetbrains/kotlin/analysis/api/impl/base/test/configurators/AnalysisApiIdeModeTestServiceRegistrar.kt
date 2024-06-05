/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinReadActionConfinementLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.platform.permissions.KotlinAnalysisPermissionOptions
import org.jetbrains.kotlin.analysis.api.platform.permissions.KotlinDefaultAnalysisPermissionOptions
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.services.TestServices

/**
 * An extension to [AnalysisApiBaseTestServiceRegistrar] for IDE mode tests. Standalone mode tests should remove this service registrar from
 * the list of [serviceRegistrars][org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator.serviceRegistrars].
 *
 * See `StandaloneSessionServiceRegistrar` for the Standalone counterpart to this service registrar.
 */
object AnalysisApiIdeModeTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(KotlinLifetimeTokenProvider::class.java, KotlinReadActionConfinementLifetimeTokenProvider::class.java)
        }
    }

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        application.apply {
            registerService(KotlinAnalysisPermissionOptions::class.java, KotlinDefaultAnalysisPermissionOptions::class.java)
        }
    }
}
