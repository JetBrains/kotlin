/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.base

import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractAnalysisApiBasedSingleModuleTest : AbstractAnalysisApiBasedTest() {
    final override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val singleModule = moduleStructure.modules.single()
        val ktFiles = testServices.ktModuleProvider.getModuleFiles(singleModule).filterIsInstance<KtFile>()
        doTestByFileStructure(ktFiles, singleModule, testServices)
    }

    protected abstract fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices)
}