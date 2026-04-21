/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.extractIgnoredDirectiveForTargetBackend
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.model.TestFailureSuppressor
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.bind

class BlackBoxCodegenSuppressor(
    testServices: TestServices,
    private val customIgnoreDirective: ValueDirective<TargetBackend>? = null,
    private val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>? = null,
) : TestFailureSuppressor(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::SuppressionChecker.bind(customIgnoreDirective, additionalIgnoreDirectives)))

    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
        val suppressionChecker = testServices.codegenSuppressionChecker
        val moduleStructure = testServices.moduleStructure
        val ignoreDirectives = suppressionChecker.extractIgnoreDirectives(moduleStructure.modules.firstOrNull()) ?: return failedAssertions
        return suppressionChecker.processAllDirectives(ignoreDirectives) { _, _ ->
            emptyList()
        } ?: failedAssertions
    }

    override fun checkIfTestShouldBeUnmuted() {
        val suppressionChecker = testServices.codegenSuppressionChecker
        val moduleStructure = testServices.moduleStructure
        val ignoreDirectives = suppressionChecker.extractIgnoreDirectives(moduleStructure.modules.firstOrNull()) ?: return
        val failures = mutableListOf<Throwable>()
        suppressionChecker.processAllDirectives(ignoreDirectives) { directive, suppressionResult ->
            try {
                suppressionChecker.throwThatTestCouldBeUnmuted(directive, suppressionResult)
            } catch (e: Throwable) {
                failures += e
            }
        }
        testServices.assertions.assertAll(failures.map { { throw it } })
    }

    class SuppressionChecker(
        val testServices: TestServices,
        private val customIgnoreDirective: ValueDirective<TargetBackend>?,
        private val additionalIgnoreDirectives: List<ValueDirective<TargetBackend>>? = null,
    ) : TestService {
        fun extractIgnoreDirectives(module: TestModule?): List<ValueDirective<TargetBackend>>? {
            if (module == null) return null
            val targetBackend = testServices.defaultsProvider.targetBackend ?: return null
            val ignoreDirective = extractIgnoredDirectiveForTargetBackend(testServices, module, targetBackend, customIgnoreDirective)
            return additionalIgnoreDirectives?.let { it + listOfNotNull(ignoreDirective) } ?: ignoreDirective?.let { listOf(it) }
        }

        fun failuresInModuleAreIgnored(module: TestModule): Boolean {
            val ignoreDirectives = extractIgnoreDirectives(module) ?: return false
            return failuresInModuleAreIgnored(module, ignoreDirectives).testMuted
        }

        fun failuresInModuleAreIgnored(module: TestModule, ignoreDirectives: List<ValueDirective<TargetBackend>>): SuppressionResult {
            val ignoredBackends = ignoreDirectives.flatMap { module.directives[it] }

            val targetBackend = testServices.defaultsProvider.targetBackend
            return when {
                ignoredBackends.isEmpty() -> SuppressionResult.NO_MUTE
                targetBackend in ignoredBackends -> SuppressionResult(true, targetBackend)
                TargetBackend.ANY in ignoredBackends -> SuppressionResult(true, null)
                else -> SuppressionResult.NO_MUTE
            }
        }

        /**
         * Finds the first directive from [ignoreDirectives] that mutes this test on the current backend, and
         * runs [processDirective] for that directive.
         *
         * Returns whatever [processDirective] returns, or `null` if this test will not be muted.
         */
        @PublishedApi
        internal inline fun <R> processAllDirectives(
            ignoreDirectives: List<ValueDirective<TargetBackend>>,
            processDirective: (ValueDirective<TargetBackend>, SuppressionResult) -> R,
        ): R? {
            val modules = testServices.moduleStructure.modules
            for (ignoreDirective in ignoreDirectives) {
                val suppressionResult = modules
                    .map { failuresInModuleAreIgnored(it, listOf(ignoreDirective)) }
                    .firstOrNull { it.testMuted }
                    ?: continue
                return processDirective(ignoreDirective, suppressionResult)
            }
            return null
        }

        /**
         * Throws an [AssertionError] with a message reminding to remove [directive] from the test to unmute it.
         */
        internal fun throwThatTestCouldBeUnmuted(
            directive: ValueDirective<TargetBackend>,
            suppressionResult: SuppressionResult,
        ): Nothing {
            val targetBackend = testServices.defaultsProvider.targetBackend
            val message = buildString {
                append("Looks like this test can be unmuted. Remove ")
                targetBackend?.name?.let {
                    append(it)
                    append(" from the ")
                }
                append(directive.name)
                append(" directive for ")
                append(testServices.defaultsProvider.frontendKind)

                assert(suppressionResult.testMuted)

                suppressionResult.matchedBackend?.let {
                    append(" for ")
                    append(it)
                }
            }
            throw AssertionError(message)
        }

        data class SuppressionResult(val testMuted: Boolean, val matchedBackend: TargetBackend?) {
            companion object {
                val NO_MUTE = SuppressionResult(false, null)
            }
        }
    }
}

val TestServices.codegenSuppressionChecker: BlackBoxCodegenSuppressor.SuppressionChecker by TestServices.testServiceAccessor()
