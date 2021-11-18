/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.test.framework

import org.jetbrains.kotlin.analysis.api.impl.barebone.test.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractHLApiSingleFileTest(configurator: FrontendApiTestConfiguratorService) : AbstractHLApiSingleModuleTest(configurator) {
    final override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val singleFile = ktFiles.singleOrNull() ?: ktFiles.first { it.name == "main.kt" }
        doTestByFileStructure(singleFile, module, testServices)
    }

    protected open fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        configurator.prepareTestFiles(listOf(ktFile), module, testServices)
    }
}