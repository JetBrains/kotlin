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
        return suppressionChecker.processAllDirectives(ignoreDirectives) { ignoreDirective, suppressionResult ->
            listOfNotNull(
                suppressionChecker.processMutedTest(
                    failed = failedAssertions.isNotEmpty(),
                    ignoreDirective,
                    suppressionResult,
                )?.wrap()
            )
        } ?: failedAssertions
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
                    .map { failuresInModuleAreIgnored(it, ignoreDirective) }
                    .firstOrNull { it.testMuted }
                    ?: continue
                return processDirective(ignoreDirective, suppressionResult)
            }
            return null
        }

        /**
         * Returns `null` if [failed] is `true`, otherwise returns an [AssertionError] with a message reminding to remove [directive]
         * from the test to unmute it.
         */
        @PublishedApi
        internal fun processMutedTest(
            failed: Boolean,
            directive: ValueDirective<TargetBackend>,
            suppressionResult: SuppressionResult,
        ): AssertionError? {
            if (failed) return null

            val firstModule = testServices.moduleStructure.modules.first()
            val targetBackend = testServices.defaultsProvider.defaultTargetBackend ?: firstModule.targetBackend
            val message = buildString {
                append("Looks like this test can be unmuted. Remove ")
                targetBackend?.name?.let {
                    append(it)
                    append(" from the ")
                }
                append(directive.name)
                append(" directive for ")
                append(firstModule.frontendKind)

                assert(suppressionResult.testMuted)

                suppressionResult.matchedBackend?.let {
                    append(" for ")
                    append(it)
                }
            }
            return AssertionError(message)
        }

        /**
         * Runs [block]. If this test has been muted by one of [ignoreDirectives] **and** [block] returns without throwing an exception,
         * throws an [AssertionError] reminding you to unmute the test.
         *
         * If this test has been muted by one of [ignoreDirectives] **and** [block] throws an exception of type [ExpectedError],
         * catches that exception and returns normally.
         *
         * If [block] throws an exception of some other type, rethrows it.
         *
         * If this test hasn't been muted **and** [block] throws any exception, rethrows that exception as well.
         */
        inline fun <reified ExpectedError : Throwable> checkMuted(
            ignoreDirectives: List<ValueDirective<TargetBackend>>,
            block: () -> Unit,
        ) {
            val expectedError: ExpectedError? = try {
                block()
                null
            } catch (e: Throwable) {
                e as? ExpectedError ?: throw e
            }

            processAllDirectives<Unit>(ignoreDirectives) { ignoreDirective, suppressionResult ->
                processMutedTest(
                    failed = expectedError != null,
                    ignoreDirective,
                    suppressionResult
                )?.let { throw it }
                return
            }

            expectedError?.let { throw it }
        }

        data class SuppressionResult(val testMuted: Boolean, val matchedBackend: TargetBackend?) {
            companion object {
                val NO_MUTE = SuppressionResult(false, null)
            }
        }
    }
}

val TestServices.codegenSuppressionChecker: BlackBoxCodegenSuppressor.SuppressionChecker by TestServices.testServiceAccessor()
