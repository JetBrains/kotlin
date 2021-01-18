/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import junit.framework.TestCase
import org.jetbrains.kotlin.test.mutes.*
import org.junit.internal.runners.statements.InvokeMethod
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters
import org.junit.runners.parameterized.ParametersRunnerFactory
import org.junit.runners.parameterized.TestWithParameters

internal fun wrapWithMuteInDatabase(testCase: TestCase, f: () -> Unit): (() -> Unit)? {
    return wrapWithMuteInDatabase(testCase.javaClass, testCase.name, f)
}

class RunnerFactoryWithMuteInDatabase : ParametersRunnerFactory {
    override fun createRunnerForTestWithParameters(testWithParameters: TestWithParameters?): Runner {
        return object : BlockJUnit4ClassRunnerWithParameters(testWithParameters) {
            override fun isIgnored(child: FrameworkMethod): Boolean {
                val methodWithParametersKey = parametrizedMethodKey(child, name)

                return super.isIgnored(child)
                        || isMutedInDatabaseWithLog(child.declaringClass, child.name)
                        || isMutedInDatabaseWithLog(child.declaringClass, methodWithParametersKey)
            }

            override fun runChild(method: FrameworkMethod, notifier: RunNotifier) {
                val testKey = testKey(method.declaringClass, parametrizedMethodKey(method, name))
                notifier.withAutoMuteListener(testKey) {
                    super.runChild(method, notifier)
                }
            }

            override fun methodInvoker(method: FrameworkMethod, test: Any?): Statement {
                return MethodInvokerWithMutedTests(method, test, mainMethodKey = parametrizedMethodKey(method, name))
            }
        }
    }

    private fun parametrizedMethodKey(child: FrameworkMethod, parametersName: String) = "${child.method.name}$parametersName"
}

class MethodInvokerWithMutedTests(
    val method: FrameworkMethod,
    val test: Any?,
    val mainMethodKey: String? = null
) : InvokeMethod(method, test) {
    override fun evaluate() {
        val methodClass = method.declaringClass
        val mutedTest =
            mainMethodKey?.let { getMutedTest(methodClass, it) }
                ?: getMutedTest(methodClass, method.method.name)

        if (mutedTest != null && isPresentedInDatabaseWithoutFailMarker(mutedTest)) {
            if (mutedTest.isFlaky) {
                super.evaluate()
                return
            } else {
                val testKey = testKey(methodClass, mutedTest.methodKey)
                invertMutedTestResultWithLog({ super.evaluate() }, testKey)
                return
            }
        }
        super.evaluate()
    }
}

class RunnerWithMuteInDatabase(klass: Class<*>?) : BlockJUnit4ClassRunner(klass) {
    override fun isIgnored(child: FrameworkMethod): Boolean {
        return super.isIgnored(child) || isMutedInDatabaseWithLog(child.declaringClass, child.name)
    }

    override fun runChild(method: FrameworkMethod, notifier: RunNotifier) {
        val testKey = testKey(method.declaringClass, method.name)
        notifier.withAutoMuteListener(testKey) {
            super.runChild(method, notifier)
        }
    }

    override fun methodInvoker(method: FrameworkMethod, test: Any?): Statement {
        return MethodInvokerWithMutedTests(method, test)
    }
}

fun TestCase.runTest(test: () -> Unit) {
    (wrapWithMuteInDatabase(this, test) ?: test).invoke()
}

annotation class WithMutedInDatabaseRunTest
