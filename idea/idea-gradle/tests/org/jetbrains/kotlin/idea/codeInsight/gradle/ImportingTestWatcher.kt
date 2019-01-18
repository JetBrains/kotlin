/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.testFramework.TestLoggerFactory
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class ImportingTestWatcher : TestWatcher() {

    override fun succeeded(description: Description) {
        TestLoggerFactory.onTestFinished(true)
    }

    override fun failed(e: Throwable, description: Description) {
        TestLoggerFactory.onTestFinished(false)
    }

    override fun starting(description: Description) {
        TestLoggerFactory.onTestStarted()
    }

}