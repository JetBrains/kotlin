/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.mutes.wrapWithMuteInDatabase
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MuteableTestRule : TestRule {
    override fun apply(
        base: Statement,
        description: Description,
    ): Statement? = object : Statement() {
        override fun evaluate() {
            val testClass = description.testClass
            val testMethod = description.methodName
            return if (testClass == null || testMethod == null) {
                base.evaluate()
            } else {
                wrapWithMuteInDatabase(testClass, testMethod, base::evaluate).invoke()
            }
        }
    }
}