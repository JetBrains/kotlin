/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractTypeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = analyseForTest(mainFile.declarations.first()) {
            val type = getType(mainFile, mainModule, testServices)

            buildString {
                appendLine(DebugSymbolRenderer(renderTypeByProperties = true).renderType(type))

                appendLine()
                appendLine("Rendered type:")
                appendLine(type.render(position = Variance.INVARIANT))
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    context(KtAnalysisSession)
    protected abstract fun getType(ktFile: KtFile, module: KtTestModule, testServices: TestServices): KtType
}
