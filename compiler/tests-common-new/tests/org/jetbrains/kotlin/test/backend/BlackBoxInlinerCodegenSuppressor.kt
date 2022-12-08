/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

enum class TargetInliner {
    IR, BYTECODE
}

class BlackBoxInlinerCodegenSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val ignoreDirectives = testServices.moduleStructure.allDirectives[CodegenTestDirectives.IGNORE_INLINER]
        if (ignoreDirectives.size > 1) {
            throw IllegalArgumentException("Directive should contain IGNORE_INLINER only one value")
        }

        val ignoreDirective = ignoreDirectives.singleOrNull() ?: return failedAssertions
        val enabledIrInliner = LanguageSettingsDirectives.ENABLE_JVM_IR_INLINER in testServices.moduleStructure.allDirectives
        val unmuteError = listOf(AssertionError("Looks like this test can be unmuted. Please remove IGNORE_INLINER directive.").wrap())

        return when {
            ignoreDirective == TargetInliner.IR && enabledIrInliner -> if (failedAssertions.isNotEmpty()) emptyList() else unmuteError
            ignoreDirective == TargetInliner.BYTECODE && !enabledIrInliner -> if (failedAssertions.isNotEmpty()) emptyList() else unmuteError
            else -> failedAssertions
        }
    }
}