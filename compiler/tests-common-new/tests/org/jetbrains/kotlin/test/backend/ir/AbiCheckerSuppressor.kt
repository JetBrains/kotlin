/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.TargetInliner
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K1
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_INLINER
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_INLINER_K1
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_INLINER_K2
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.JVM_ABI_K1_K2_DIFF
import org.jetbrains.kotlin.test.directives.tryRetrieveIgnoredInliner
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.service

class AbiCheckerSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(BlackBoxCodegenSuppressor::SuppressionChecker.bind(null)))

    private val TestModule.ignoredByBackend: Boolean
        get() = listOf(IGNORE_BACKEND, IGNORE_BACKEND_K1, IGNORE_BACKEND_K2)
            .any { testServices.abiCheckerSuppressionChecker.failuresInModuleAreIgnored(this, it).testMuted }

    private val ignoredByInliner: Boolean
        get() = listOf(IGNORE_INLINER, IGNORE_INLINER_K1, IGNORE_INLINER_K2)
            .any { testServices.tryRetrieveIgnoredInliner(it) == TargetInliner.BYTECODE }


    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val module = testServices.moduleStructure.modules.first()
        val mutedAbiChecking = testServices.mutedAbiChecking
        if (module.ignoredByBackend || ignoredByInliner) {
            if (mutedAbiChecking) {
                return listOf(
                    AssertionError(
                        "No ABI comparison done for this test. Please remove ${JVM_ABI_K1_K2_DIFF.name} directive."
                    ).wrap()
                )
            }
            return emptyList()
        }
        return failedAssertions
    }
}

val TestServices.abiCheckerSuppressionChecker: BlackBoxCodegenSuppressor.SuppressionChecker by TestServices.testServiceAccessor()

val TestServices.mutedAbiChecking: Boolean get() = moduleStructure.allDirectives.contains(JVM_ABI_K1_K2_DIFF)