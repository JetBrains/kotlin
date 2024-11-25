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
            DO_AUTO_MUTE.muteTest(
                testKey(testClass, context.displayName)
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
        val testMethod: Method? = extensionContext.testMethodNullable
        if (testClass != null &&
            testMethod != null
        ) {
            System.err.println("Expected failure: ${extensionContext.displayName}")
            throw Exception("Expected failure")
            /*val mutedTest = getMutedTest(testClass, testMethod.name)
            if (mutedTest != null //&&
                isPresentedInDatabaseWithoutFailMarker(mutedTest)
            ) {
                if (!mutedTest.isFlaky) {
                    invertMutedTestResultWithLog(
                        f = { invocation.proceed() },
                        testKey = testKey(testMethod.declaringClass, mutedTest.methodKey)
                    )
                    return
                } else if (DO_AUTO_MUTE.isMuted(testKey(testClass, extensionContext.displayName))) {
                    DO_AUTO_MUTE.muted(testKey(testClass, extensionContext.displayName))
                    invocation.skip()
                    return
                } else {
                    invocation.proceed()
                    return
                }
            }*/
        }
        System.err.println("CRISTIAN WTF")
        invocation.proceed()
    }
}
