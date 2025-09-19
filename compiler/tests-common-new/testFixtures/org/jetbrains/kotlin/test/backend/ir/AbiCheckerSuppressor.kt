/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K1
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_JVM_ABI_K1_K2
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.JVM_ABI_K1_K2_DIFF
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.service
import org.jetbrains.kotlin.utils.bind

class AbiCheckerSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(BlackBoxCodegenSuppressor::SuppressionChecker.bind(null, null)))


    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val isDifferenceExplained = testServices.moduleStructure.allDirectives.contains(JVM_ABI_K1_K2_DIFF)
        if (testServices.ignoredByBackend) {
            if (isDifferenceExplained) {
                return listOf(
                    AssertionError(
                        "No K1/K2 JVM ABI comparison done for this test. Please remove ${JVM_ABI_K1_K2_DIFF.name} directive."
                    ).wrap()
                )
            }
            return emptyList()
        }

        // Some test might be both green in K1 and with a default language version in K2,
        // but K2 mode is effectively run with `supportsFeature` working just as the LV were set to 1.9
        // (see delegated anonymous object at org.jetbrains.kotlin.test.TestSetupUtilsKt.runWithEnablingFirUseOption).
        // And while the mission for JvmAbiConsistencyTest seems to be completed, it's ok sometimes to skip it for the newer test,
        // at least with trivial generated ABI.
        if (testServices.moduleStructure.allDirectives.contains(IGNORE_JVM_ABI_K1_K2)) {
            if (failedAssertions.isEmpty()) {
                return listOf(
                    AssertionError("Test contains ${IGNORE_JVM_ABI_K1_K2.name} both pipelines are successful.").wrap()
                )
            }
            return emptyList()
        }

        return failedAssertions
    }
}


private val TestServices.suppressionChecker: BlackBoxCodegenSuppressor.SuppressionChecker by TestServices.testServiceAccessor()

internal val TestServices.ignoredByBackend: Boolean
    get() = suppressionChecker.failuresInModuleAreIgnored(
        moduleStructure.modules.first(),
        listOf(IGNORE_BACKEND, IGNORE_BACKEND_K1, IGNORE_BACKEND_K2)
    ).testMuted
