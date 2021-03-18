/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.idea.perf.forceUsingOldLightClassesForTest
import org.jetbrains.kotlin.idea.test.withCustomCompilerOptions

abstract class AbstractJavaAgainstKotlinSourceCheckerTest : AbstractJavaAgainstKotlinCheckerTest() {
    fun doTest(path: String) {
        fun doTest() {
            doTest(true, true, path.replace(".kt", ".java"), path)
        }

        val configFile = configFileText
        if (configFile != null) {
            withCustomCompilerOptions(configFile, project, module) {
                doTest()
            }
        } else {
            doTest()
        }
    }
}

abstract class AbstractJavaAgainstKotlinSourceCheckerWithoutUltraLightTest : AbstractJavaAgainstKotlinSourceCheckerTest() {
    override fun setUp() {
        super.setUp()
        forceUsingOldLightClassesForTest()
    }
}
