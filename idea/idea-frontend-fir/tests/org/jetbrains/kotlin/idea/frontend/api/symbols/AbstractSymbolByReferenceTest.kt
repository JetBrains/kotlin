/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.analyseOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.test.framework.AbstractKtIdeaTest
import org.jetbrains.kotlin.idea.test.framework.TestFileStructure
import org.jetbrains.kotlin.idea.test.framework.TestStructureExpectedDataBlock
import org.jetbrains.kotlin.idea.test.framework.TestStructureRenderer
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.test.KotlinTestUtils

abstract class AbstractSymbolByReferenceTest : AbstractKtIdeaTest() {
    override fun doTestByFileStructure(fileStructure: TestFileStructure) {
        val referenceExpression = getElementOfTypeAtCaret<KtNameReferenceExpression>()
        val reference = referenceExpression.mainReference
        val renderedSymbol = analyseOnPooledThreadInReadAction(fileStructure.mainKtFile) {
            val symbol = reference.resolveToSymbol()
                ?: error("${referenceExpression.text} was not resolved to anything")
            DebugSymbolRenderer.render(symbol)
        }

        val actual = TestStructureRenderer.render(
            fileStructure,
            TestStructureExpectedDataBlock(renderedSymbol),
            renderingMode = TestStructureRenderer.RenderingMode.ALL_BLOCKS_IN_MULTI_LINE_COMMENT,
        )
        KotlinTestUtils.assertEqualsToFile(fileStructure.filePath.toFile(), actual)
    }
}