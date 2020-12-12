/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.findUsages

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor

abstract class AbstractKotlinFindUsagesWithLibraryFirTest : AbstractKotlinFindUsagesWithLibraryTest() {
    override fun isFirPlugin(): Boolean = true

    override fun <T : PsiElement> doTest(path: String) = doTestWithFIRFlagsByPath(path) {
        super.doTest<T>(path)
    }
}