/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.mutes.*
import org.junit.jupiter.api.extension.*
import java.lang.reflect.Method

/**
 * Extension for JUnit 5 tests adding mute-in database support to ignore flaky/failed tests.
 *
 * Just add it to the test class or test method.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(MuteInCondition::class, MuteInTestWatcher::class, MuteInInvocationInterceptor::class)
annotation class WithMuteInDatabase

private val ExtensionContext.testClassNullable get() = testClass.orElseGet { null }
private val ExtensionContext.testMethodNullable get() = testMethod.orElseGet { null }

class MuteInCondition : ExecutionCondition {
    override fun evaluateExecutionCondition(
        context: ExtensionContext
    ): ConditionEvaluationResult {
        val testClass = context.testClassNullable
        val testMethod = context.testMethodNullable

        return if (testClass != null &&
            testMethod != null &&
            isMutedInDatabaseWithLog(testClass, testMethod.name)
        ) {
            ConditionEvaluationResult.disabled("Muted")
        } else {
            enabled
        }
    }

    companion object {
        private val enabled = ConditionEvaluationResult.enabled("Not found in mute-in database")
    }
}

class MuteInTestWatcher : TestWatcher {
    override fun testFailed(
        context: ExtensionContext,
        cause: Throwable
    ) {
        val testClass = context.testClassNullable
        val testMethod = context.testMethodNullable
        if (testClass != null &&
            testMethod != null
        ) {
            val methodKey = getMethodKey(testMethod, context.displayName)
            val mutedTest = getMutedTest(testClass, methodKey)
            if (mutedTest == null) {
                println(
                    """FAILED TEST: if it's a cross-push add to mute-common.csv:
                    ${testClass.canonicalName}.$methodKey,KT-XXXX,STABLE
                    or if you consider it's flaky:
                    ${testClass.canonicalName}.$methodKey,KT-XXXX,FLAKY""".trimIndent()
                )
            }
        }
    }
}

private fun getMethodKey(testMethod: Method, displayName: String): String = testMethod.name + if (displayName == "${testMethod.name}()") "" else "[${displayName}]"

class MuteInInvocationInterceptor : InvocationInterceptor {
    override fun interceptTestTemplateMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        interceptWithMuteInDatabase(invocation, extensionContext)
    }

    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        interceptWithMuteInDatabase(invocation, extensionContext)
    }

    private fun interceptWithMuteInDatabase(
        invocation: InvocationInterceptor.Invocation<Void>,
        extensionContext: ExtensionContext
    ) {
        val testClass = extensionContext.testClassNullable
        val testMethod: Method? = extensionContext.testMethodNullable
        if (testClass != null &&
            testMethod != null
        ) {
            val mutedTest = getMutedTest(testClass, getMethodKey(testMethod, extensionContext.displayName)) ?: getMutedTest(testClass, testMethod.name)
            if (mutedTest != null) {
                if (!mutedTest.isFlaky) {
                    invertMutedTestResultWithLog(
                        f = { invocation.proceed() },
                        testKey = testKey(testMethod.declaringClass, getMethodKey(testMethod, extensionContext.displayName))
                    )
                    return
                } else {
                    System.err.println(
                        "MUTED TEST: ${
                            testKey(
                                testMethod.declaringClass,
                                getMethodKey(testMethod, extensionContext.displayName)
                            )
                        }"
                    )
                    invocation.skip()
                    return
                }
            }
        }

        invocation.proceed()
    }
}
