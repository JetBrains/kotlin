/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.idea.perf.forceUsingUltraLightClassesForTest

abstract class AbstractJavaAgainstKotlinSourceCheckerTest : AbstractJavaAgainstKotlinCheckerTest() {
    fun doTest(path: String) {
        doTest(true, true, path.replace(".kt", ".java"), path)
    }
}

abstract class AbstractJavaAgainstKotlinSourceCheckerWithUltraLightTest : AbstractJavaAgainstKotlinSourceCheckerTest() {
    override fun setUp() {
        super.setUp()
        forceUsingUltraLightClassesForTest()
    }
}
