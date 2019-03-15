/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.findUsages

import org.jetbrains.kotlin.idea.multiplatform.setupMppProjectFromDirStructure
import org.junit.Test
import java.io.File

class FindUsagesMultiModuleTest : AbstractFindUsagesMultiModuleTest() {

    @Test
    fun testFindActualInterface() {
        doTest()
    }

    @Test
    fun testFindCommonClassFromActual() {
        doTest()
    }

    @Test
    fun testFindCommonFromActual() {
        doTest()
    }

    @Test
    fun testFindCommonPropertyFromActual() {
        doTest()
    }

    @Test
    fun testFindCommonSuperclass() {
        doTest()
    }

    @Test
    fun testFindImplFromHeader() {
        doTest()
    }

    private fun doTest() {
        setupMppProjectFromDirStructure(File(testDataPath + getTestName(true).removePrefix("test")))
        doFindUsagesTest()
    }
}