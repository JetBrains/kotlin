/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.TargetInliner
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K1
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_INLINER
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_INLINER_K1
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_INLINER_K2
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.JVM_ABI_K1_K2_DIFF
import org.jetbrains.kotlin.test.directives.tryRetrieveIgnoredInliner
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.service
import org.jetbrains.kotlin.utils.bind

class AbiCheckerSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {

    companion object {
        fun ignoredByBackendOrInliner(testServices: TestServices) = testServices.ignoredByBackend || testServices.ignoredByInliner
    }

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(BlackBoxCodegenSuppressor::SuppressionChecker.bind(null)))


    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val isDifferenceExplained = testServices.moduleStructure.allDirectives.contains(JVM_ABI_K1_K2_DIFF)
        if (ignoredByBackendOrInliner(testServices)) {
            if (isDifferenceExplained) {
                return listOf(
                    AssertionError(
                        "No K1/K2 JVM ABI comparison done for this test. Please remove ${JVM_ABI_K1_K2_DIFF.name} directive."
                    ).wrap()
                )
            }
            return emptyList()
        }
        return failedAssertions
    }
}


private val TestServices.suppressionChecker: BlackBoxCodegenSuppressor.SuppressionChecker by TestServices.testServiceAccessor()

private val TestServices.ignoredByBackend: Boolean
    get() = listOf(IGNORE_BACKEND, IGNORE_BACKEND_K1, IGNORE_BACKEND_K2)
        .any { suppressionChecker.failuresInModuleAreIgnored(moduleStructure.modules.first(), it).testMuted }

private val TestServices.ignoredByInliner: Boolean
    get() = listOf(IGNORE_INLINER, IGNORE_INLINER_K1, IGNORE_INLINER_K2).any { tryRetrieveIgnoredInliner(it) == TargetInliner.BYTECODE }