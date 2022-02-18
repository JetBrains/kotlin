/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_FIR
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

class BlackBoxCodegenSuppressor(
    testServices: TestServices,
    val customIgnoreDirective: ValueDirective<TargetBackend>? = null
) : AfterAnalysisChecker(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::SuppressionChecker.bind(customIgnoreDirective)))

    @OptIn(ExperimentalStdlibApi::class)
    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val suppressionChecker = testServices.codegenSuppressionChecker
        val moduleStructure = testServices.moduleStructure
        val ignoreDirective = suppressionChecker.extractIgnoreDirective(moduleStructure.modules.first()) ?: return failedAssertions
        val suppressionResult = moduleStructure.modules.map { suppressionChecker.failuresInModuleAreIgnored(it, ignoreDirective) }
            .firstOrNull { it.testMuted } ?: return failedAssertions
        val additionalMessage = suppressionResult.matchedBackend?.let { "for $it" } ?: ""
        return processAssertions(failedAssertions, ignoreDirective, additionalMessage)
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

    class SuppressionChecker(val testServices: TestServices, val customIgnoreDirective: ValueDirective<TargetBackend>?) : TestService {
        fun extractIgnoreDirective(module: TestModule): ValueDirective<TargetBackend>? {
            return when (module.frontendKind) {
                FrontendKinds.ClassicFrontend -> customIgnoreDirective ?: IGNORE_BACKEND
                FrontendKinds.FIR -> customIgnoreDirective ?: IGNORE_BACKEND_FIR
                else -> null
            }
        }

        fun failuresInModuleAreIgnored(module: TestModule): Boolean {
            val ignoreDirective = extractIgnoreDirective(module) ?: return false
            return failuresInModuleAreIgnored(module, ignoreDirective).testMuted
        }

        fun failuresInModuleAreIgnored(module: TestModule, ignoreDirective: ValueDirective<TargetBackend>): SuppressionResult {
            val ignoredBackends = module.directives[ignoreDirective]

            val targetBackend = testServices.defaultsProvider.defaultTargetBackend ?: module.targetBackend
            return when {
                ignoredBackends.isEmpty() -> SuppressionResult.NO_MUTE
                targetBackend in ignoredBackends -> SuppressionResult(true, targetBackend)
                TargetBackend.ANY in ignoredBackends -> SuppressionResult(true, null)
                else -> SuppressionResult.NO_MUTE
            }
        }

        data class SuppressionResult(val testMuted: Boolean, val matchedBackend: TargetBackend?) {
            companion object {
                val NO_MUTE = SuppressionResult(false, null)
            }
        }
    }
}

val TestServices.codegenSuppressionChecker: BlackBoxCodegenSuppressor.SuppressionChecker by TestServices.testServiceAccessor()
