/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.TestJdkKind

class Java8MultiModuleHighlightingTest : AbstractMultiModuleHighlightingTest() {
    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleHighlighting/"

    fun testDifferentJdk() {
        val module1 = module("jdk8", TestJdkKind.FULL_JDK)
        val module2 = module("mockJdk")

        module1.addDependency(module2)

        checkHighlightingInProject()
    }
}
