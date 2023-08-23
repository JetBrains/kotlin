/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.SymbolByFqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractSymbolByFqNameTest : AbstractSymbolTest() {
    override fun KtAnalysisSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData {
        val symbolData = SymbolByFqName.getSymbolDataFromFile(testDataPath)
        return SymbolsData(with(symbolData) { toSymbols(ktFile) })
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            defaultDirectives {
                +ConfigurationDirectives.WITH_STDLIB
            }
        }
    }
}