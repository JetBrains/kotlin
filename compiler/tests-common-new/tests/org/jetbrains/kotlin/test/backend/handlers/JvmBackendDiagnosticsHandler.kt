/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.DiagnosticsService
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.service

class JvmBackendDiagnosticsHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::DiagnosticsService))

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        val ktDiagnosticReporter = info.classFileFactory.generationState.diagnosticReporter as BaseDiagnosticsCollector
        reportKtDiagnostics(module, ktDiagnosticReporter)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        checkFullDiagnosticRender()
    }
}
