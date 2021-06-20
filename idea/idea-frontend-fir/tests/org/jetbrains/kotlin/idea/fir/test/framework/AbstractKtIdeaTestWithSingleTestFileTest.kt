/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.test.framework

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractKtIdeaTestWithSingleTestFileTest : AbstractKtSingleModuleTest() {
    final override fun doTestByFileStructure(ktFiles: List<KtFile>, moduleStructure: TestModuleStructure, testServices: TestServices) {
        doTestByFileStructure(ktFiles.first(), moduleStructure, testServices)
    }

    abstract fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices)
}