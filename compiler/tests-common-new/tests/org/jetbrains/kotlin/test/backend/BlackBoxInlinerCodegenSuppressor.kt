/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.directives.tryRetrieveIgnoredInliner
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure

enum class TargetInliner {
    IR, BYTECODE
}

class BlackBoxInlinerCodegenSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val targetFrontend = testServices.defaultsProvider.defaultFrontend

        val commonResult = suppressForTargetFrontend(failedAssertions, CodegenTestDirectives.IGNORE_INLINER)
        if (commonResult != null) return commonResult

        return when (targetFrontend) {
            FrontendKinds.ClassicFrontend -> {
                suppressForTargetFrontend(failedAssertions, CodegenTestDirectives.IGNORE_INLINER_K1) ?: failedAssertions
            }
            FrontendKinds.FIR -> {
                suppressForTargetFrontend(failedAssertions, CodegenTestDirectives.IGNORE_INLINER_K2) ?: failedAssertions
            }
            else -> failedAssertions
        }
    }

    private fun suppressForTargetFrontend(
        failedAssertions: List<WrappedException>,
        directive: ValueDirective<TargetInliner>,
    ): List<WrappedException>? {
        val ignoreDirective = testServices.tryRetrieveIgnoredInliner(directive)
        val enabledIrInliner = LanguageSettingsDirectives.ENABLE_JVM_IR_INLINER in testServices.moduleStructure.allDirectives
        val unmuteError = listOf(AssertionError("Looks like this test can be unmuted. Please remove ${directive.name} directive.").wrap())

        if (ignoreDirective == TargetInliner.IR && enabledIrInliner || ignoreDirective == TargetInliner.BYTECODE && !enabledIrInliner) {
            return if (failedAssertions.isNotEmpty()) emptyList() else unmuteError
        }

        return null
    }
}