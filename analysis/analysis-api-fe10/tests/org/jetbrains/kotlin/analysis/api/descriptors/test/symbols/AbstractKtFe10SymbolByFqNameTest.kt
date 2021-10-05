/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.test.symbols

import org.jetbrains.kotlin.analysis.api.descriptors.test.KtFe10FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.descriptors.test.KtFe10TestWithOutOfBlockModification
import org.jetbrains.kotlin.analysis.api.descriptors.test.analyzeTestFiles
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.impl.base.test.symbols.AbstractSymbolByFqNameTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractKtFe10SymbolByFqNameTest : AbstractSymbolByFqNameTest() {
    override val testPrefix: String?
        get() = "descriptors"

    override val configurator: FrontendApiTestConfiguratorService
        get() = KtFe10FrontendApiTestConfiguratorService

    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        analyzeTestFiles(listOf(ktFile), module, testServices)
        super.doTestByFileStructure(ktFile, module, testServices)
    }

    override fun doOutOfBlockModification(ktFile: KtFile) {
        KtFe10TestWithOutOfBlockModification.doOutOfBlockModification(ktFile)
    }
}