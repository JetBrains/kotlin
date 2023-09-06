/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.ENABLE_IR_FAKE_OVERRIDE_GENERATION
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

class CodegenWithIrFakeOverrideGeneratorSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        return when {
            !mainDirectiveEnabled -> failedAssertions
            suppressDirectiveEnabled ->
                if (failedAssertions.isEmpty())
                    listOf(
                        AssertionError("Looks like this test can be unmuted. Remove $IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION directive.").wrap()
                    )
                else emptyList()
            else -> failedAssertions
        }
    }

    private val mainDirectiveEnabled: Boolean
        get() = ENABLE_IR_FAKE_OVERRIDE_GENERATION in testServices.moduleStructure.allDirectives

    private val suppressDirectiveEnabled: Boolean
        get() = IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION in testServices.moduleStructure.allDirectives
}
