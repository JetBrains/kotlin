/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractLowLevelApiLastModuleFirstFileTest : AbstractAnalysisApiBasedTest() {
    final override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val lastModule = moduleStructure.modules.last()
        val firstKtFileFile = testServices.ktModuleProvider.getModuleFiles(lastModule).firstNotNullOf { it as? KtFile }
        doTestByFileStructure(firstKtFileFile, lastModule, testServices)
    }

    abstract fun doTestByFileStructure(ktFile: KtFile, testModule: TestModule, testServices: TestServices)
}
