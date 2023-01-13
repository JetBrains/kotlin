/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractLowLevelApiSingleFileTest : AbstractLowLevelApiSingleModuleTest() {
    final override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val singleModule = moduleStructure.modules.single()
        val singleFile = testServices.ktModuleProvider.getModuleFiles(singleModule).filterIsInstance<KtFile>().single()
        doTestByFileStructure(singleFile, moduleStructure, testServices)
    }

    abstract fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices)
}