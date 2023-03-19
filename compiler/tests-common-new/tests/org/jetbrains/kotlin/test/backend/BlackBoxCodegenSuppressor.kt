/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.extractIgnoredDirectivesForTargetBackend
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
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

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val suppressionChecker = testServices.codegenSuppressionChecker
        val moduleStructure = testServices.moduleStructure
        val ignoreDirectives = suppressionChecker.extractIgnoreDirectives(moduleStructure.modules.first()) ?: return failedAssertions
        for (ignoreDirective in ignoreDirectives) {
            val suppressionResult = moduleStructure.modules
                .map { suppressionChecker.failuresInModuleAreIgnored(it, ignoreDirective) }
                .firstOrNull { it.testMuted }
                ?: continue
            val additionalMessage = suppressionResult.matchedBackend?.let { "for $it" } ?: ""
            return processAssertions(failedAssertions, ignoreDirective, additionalMessage)
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
                val module = testServices.moduleStructure.modules.first()
                val targetBackend = testServices.defaultsProvider.defaultTargetBackend ?: module.targetBackend
                append("Looks like this test can be unmuted. Remove ${targetBackend?.name?.let { "$it from" } ?: "" } ${directive.name} directive for ${module.frontendKind}")
                if (additionalMessage.isNotEmpty()) {
                    append(" ")
                    append(additionalMessage)
                }
            }
            listOf(AssertionError(message).wrap())
        }
    }

    class SuppressionChecker(val testServices: TestServices, val customIgnoreDirective: ValueDirective<TargetBackend>?) : TestService {
        fun extractIgnoreDirectives(module: TestModule): List<ValueDirective<TargetBackend>>? {
            val targetBackend = testServices.defaultsProvider.defaultTargetBackend ?: module.targetBackend ?: return null
            return extractIgnoredDirectivesForTargetBackend(module, targetBackend, customIgnoreDirective)
        }

        fun failuresInModuleAreIgnored(module: TestModule): Boolean {
            val ignoreDirective = extractIgnoreDirectives(module) ?: return false
            return failuresInModuleAreIgnored(module, ignoreDirective).testMuted
        }

        private fun failuresInModuleAreIgnored(module: TestModule, ignoreDirectives: List<ValueDirective<TargetBackend>>): SuppressionResult {
            for (ignoreDirective in ignoreDirectives) {
                val result = failuresInModuleAreIgnored(module, ignoreDirective)
                if (result.testMuted) return result
            }
            return SuppressionResult.NO_MUTE
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
