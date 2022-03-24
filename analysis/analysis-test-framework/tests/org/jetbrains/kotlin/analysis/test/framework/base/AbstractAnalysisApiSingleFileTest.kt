/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.base

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractAnalysisApiSingleFileTest : AbstractAnalysisApiBasedSingleModuleTest() {
    final override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val singleFile = ktFiles.first()
        doTestByFileStructure(singleFile, module, testServices)
    }

    protected abstract fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices)
}