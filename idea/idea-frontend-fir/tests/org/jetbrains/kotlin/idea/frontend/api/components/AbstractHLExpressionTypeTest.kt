/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.test.framework.AbstractKtIdeaTest
import org.jetbrains.kotlin.idea.test.framework.TestFileStructure
import org.jetbrains.kotlin.idea.test.framework.TestStructureExpectedDataBlock
import org.jetbrains.kotlin.idea.test.framework.TestStructureRenderer
import org.jetbrains.kotlin.idea.util.application.executeOnPooledThread
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.test.KotlinTestUtils

abstract class AbstractHLExpressionTypeTest : AbstractKtIdeaTest() {
    override fun doTestByFileStructure(fileStructure: TestFileStructure) {
        val expression = fileStructure.mainFile.selectedExpression as KtExpression?
            ?: error("Selected expression was not provided")
        val type = executeOnPooledThreadInReadAction {
            analyse(expression) { expression.getKtType().render() }
        }
        val actual = TestStructureRenderer.render(
            fileStructure,
            TestStructureExpectedDataBlock(
                "expression: ${expression.text}",
                "type: $type"
            )
        )
        KotlinTestUtils.assertEqualsToFile(fileStructure.filePath.toFile(), actual)
    }
}