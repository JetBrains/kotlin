/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_FIR
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.joinToArrayString

class BlackBoxCodegenSuppressor(
    testServices: TestServices,
    val customIgnoreDirective: ValueDirective<TargetBackend>? = null
) : AfterAnalysisChecker(testServices) {
    override val directives: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    @OptIn(ExperimentalStdlibApi::class)
    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val moduleStructure = testServices.moduleStructure
        val targetBackends = buildList {
            testServices.defaultsProvider.defaultTargetBackend?.let {
                add(it)
                return@buildList
            }
            moduleStructure.modules.mapNotNullTo(this) { it.targetBackend }
        }
        val ignoreDirective = when (moduleStructure.modules.map { it.frontendKind }.first()) {
            FrontendKinds.ClassicFrontend -> customIgnoreDirective ?: IGNORE_BACKEND
            FrontendKinds.FIR -> IGNORE_BACKEND_FIR
            else -> return failedAssertions
        }
        return processIgnoreBackend(moduleStructure, ignoreDirective, targetBackends, failedAssertions)
    }

    private fun processIgnoreBackend(
        moduleStructure: TestModuleStructure,
        directive: ValueDirective<TargetBackend>,
        targetBackends: List<TargetBackend>,
        failedAssertions: List<WrappedException>
    ): List<WrappedException> {
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
        failedAssertions: List<WrappedException>,
        directive: ValueDirective<TargetBackend>,
        additionalMessage: String = ""
    ): List<WrappedException> {
        return if (failedAssertions.isNotEmpty()) emptyList()
        else {
            val message = buildString {
                append("Looks like this test can be unmuted. Remove ${directive.name} directive")
                if (additionalMessage.isNotEmpty()) {
                    append(" ")
                    append(additionalMessage)
                }
            }
            listOf(AssertionError(message).wrap())
        }
    }
}
