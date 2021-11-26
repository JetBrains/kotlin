/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.codegen.jdk

import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.Description
import org.junit.runner.manipulation.Filter
import org.junit.runner.notification.RunNotifier
import java.io.File

annotation class RunOnlyJdk6Test

class JUnitPlatformRunnerForJdk6(testClass: Class<*>) : JUnitPlatform(testClass) {
    init {
        if (testClass.getAnnotation(RunOnlyJdk6Test::class.java) != null) {
            this.filter(object : Filter() {
                override fun shouldRun(description: Description): Boolean {
                    if (description.isTest) {
                        @Suppress("NAME_SHADOWING")
                        val testClass = description.testClass ?: return true
                        val methodName = description.methodName ?: return true

                        val testClassAnnotation = testClass.getAnnotation(TestMetadata::class.java) ?: return true
                        val method = testClass.getMethod(methodName)
                        val methodAnnotation = method.getAnnotation(TestMetadata::class.java) ?: return true
                        val path = "${testClassAnnotation.value}/${methodAnnotation.value}"
                        val fileText = File(path).readText()
                        return !InTextDirectivesUtils.isDirectiveDefined(fileText, "// JVM_TARGET:") &&
                                !InTextDirectivesUtils.isDirectiveDefined(fileText, "// SKIP_JDK6")
                    }
                    return true
                }

                override fun describe(): String {
                    return "skipped on JDK 6"
                }
            })
        }
    }

    override fun run(notifier: RunNotifier?) {
        SeparateJavaProcessHelper.setUp()
        try {
            super.run(notifier)
        } finally {
            SeparateJavaProcessHelper.tearDown()
        }
    }
}
