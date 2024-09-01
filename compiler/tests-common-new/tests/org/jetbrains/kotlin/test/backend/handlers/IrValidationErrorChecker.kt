/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.backend.common.IrValidationError
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider

/**
 * Produces a helpful hint in case a test fails due to [IrValidationError].
 */
class IrValidationErrorChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val targetBackend = testServices.defaultsProvider.defaultTargetBackend ?: TargetBackend.ANY
        return failedAssertions.map {
            if (it.cause is IrValidationError) {
                IrValidationError(
                    "IR validation failed. The errors stream should contain more information about which IR nodes caused this failure.\n" +
                            "If the validation errors are caused by visibility violations which are intentional (for example, " +
                            "if '@Suppress(\"INVISIBLE_REFERENCE\")' is used in the test), you can disable the visibility checks by " +
                            "specifying the '// DISABLE_IR_VISIBILITY_CHECKS: ${targetBackend}' test directive.",
                    it.cause,
                ).wrap()
            } else {
                it
            }
        }
    }
}