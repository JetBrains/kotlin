/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_FIR
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.util.joinToArrayString

class BlackBoxCodegenSuppressor(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override val directives: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun suppressIfNeeded(failedAssertions: List<Throwable>): List<Throwable> {
        val moduleStructure = testServices.moduleStructure
        val targetBackends = moduleStructure.modules.mapNotNull { it.targetBackend }
        return when (moduleStructure.modules.map { it.frontendKind }.first()) {
            FrontendKinds.ClassicFrontend -> processIgnoreBackend(moduleStructure, IGNORE_BACKEND, targetBackends, failedAssertions)
            FrontendKinds.FIR -> processIgnoreBackend(moduleStructure, IGNORE_BACKEND_FIR, targetBackends, failedAssertions)
            else -> failedAssertions
        }
    }

    private fun processIgnoreBackend(
        moduleStructure: TestModuleStructure,
        directive: ValueDirective<TargetBackend>,
        targetBackends: List<TargetBackend>,
        failedAssertions: List<Throwable>
    ): List<Throwable> {
        val ignoredBackends = moduleStructure.allDirectives[directive]
        if (ignoredBackends.isEmpty()) return failedAssertions
        val matchedBackend = ignoredBackends.intersect(targetBackends)
        if (ignoredBackends.contains(TargetBackend.ANY)) {
            return processAssertions(failedAssertions, directive)
        }
        if (matchedBackend.isNotEmpty()) {
            return processAssertions(failedAssertions, directive, "for ${matchedBackend.joinToArrayString()}")
        }
        return failedAssertions
    }


    private fun processAssertions(
        failedAssertions: List<Throwable>,
        directive: ValueDirective<TargetBackend>,
        additionalMessage: String = ""
    ): List<Throwable> {
        return if (failedAssertions.isNotEmpty()) emptyList()
        else {
            val message = buildString {
                append("Looks like this test can be unmuted. Remove ${directive.name} directive")
                if (additionalMessage.isNotEmpty()) {
                    append(" ")
                    append(additionalMessage)
                }
            }
            listOf(AssertionError(message))
        }
    }
}
