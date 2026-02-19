/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.nordstrom.automation.junit.*
import org.jetbrains.kotlin.test.mutes.getMutedTest
import org.jetbrains.kotlin.test.mutes.mutedMessage
import org.jetbrains.kotlin.test.mutes.testKey
import org.junit.AssumptionViolatedException
import org.junit.internal.runners.model.ReflectiveCallable
import org.junit.runner.Description
import org.junit.runners.model.FrameworkMethod

class MuteWithDatabaseWatcher : MethodWatcher<FrameworkMethod> {
    override fun beforeInvocation(
        runner: Any?,
        child: FrameworkMethod?,
        callable: ReflectiveCallable?,
    ) {
    }

    override fun afterInvocation(
        runner: Any?,
        child: FrameworkMethod?,
        callable: ReflectiveCallable?,
        thrown: Throwable?,
    ) {
        val description: Description? = LifecycleHooks.describeChild(runner, child)
        if (description != null && description.isTest) {
            val testClass = description.testClass
            val testMethod = description.methodName
            val testKey = testKey(testClass, testMethod)
            val mutedTest = getMutedTest(testClass, testMethod)

            if (thrown != null) {
                if (mutedTest != null) {
                    throw AssumptionViolatedException(mutedMessage(testClass, testMethod))
                }
            } else if (mutedTest?.isFlaky == false) {
                throw Exception("Muted non-flaky test $testKey finished successfully. Please remove it from csv file")
            }
        }
    }

    override fun supportedType(): Class<FrameworkMethod> = FrameworkMethod::class.java
}
