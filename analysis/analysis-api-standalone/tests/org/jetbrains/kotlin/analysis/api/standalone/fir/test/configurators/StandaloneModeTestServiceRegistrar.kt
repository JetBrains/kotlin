/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Registers services specific to Standalone mode *tests*, in addition to the Standalone production services registered by
 * [FirStandaloneServiceRegistrar][org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.FirStandaloneServiceRegistrar].
 */
object StandaloneModeTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {
    }
}
