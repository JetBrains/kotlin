/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.findUsages

import org.jetbrains.kotlin.idea.multiplatform.setupMppProjectFromDirStructure
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
open class FindUsagesMultiModuleTest : AbstractFindUsagesMultiModuleTest() {

    fun testFindActualInterface() {
        doTest()
    }

    fun testFindCommonClassFromActual() {
        doTest()
    }

    fun testFindCommonFromActual() {
        doTest()
    }

    fun testFindCommonPropertyFromActual() {
        doTest()
    }

    fun testFindCommonSuperclass() {
        doTest()
    }

    fun testFindImplFromHeader() {
        doTest()
    }

    private fun doTest() {
        setupMppProjectFromDirStructure(File(testDataPath + getTestName(true).removePrefix("test")))
        doFindUsagesTest()
    }
}