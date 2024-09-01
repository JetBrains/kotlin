/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import junit.framework.TestCase
import org.jetbrains.kotlin.test.mutes.*
import org.junit.jupiter.api.extension.*
import java.lang.reflect.Method

internal fun wrapWithMuteInDatabase(testCase: TestCase, f: () -> Unit): (() -> Unit)? {
    return wrapWithMuteInDatabase(testCase.javaClass, testCase.name, f)
}

fun TestCase.runTest(test: () -> Unit) {
    (wrapWithMuteInDatabase(this, test) ?: test).invoke()
}

annotation class WithMutedInDatabaseRunTest

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
            DO_AUTO_MUTE?.muteTest(
                testKey(testClass, testMethod.name)
            )
        }
    }
}

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
        val testMethod = extensionContext.testMethodNullable
        if (testClass != null &&
            testMethod != null
        ) {
            val mutedTest = getMutedTest(testClass, testMethod.name)
            if (mutedTest != null &&
                isPresentedInDatabaseWithoutFailMarker(mutedTest)
            ) {
                if (mutedTest.isFlaky) {
                    invocation.proceed()
                    return
                } else {
                    invertMutedTestResultWithLog(
                        f = { invocation.proceed() },
                        testKey = testKey(testMethod.declaringClass, mutedTest.methodKey)
                    )
                    return
                }
            }
        }

        invocation.proceed()
    }
}
