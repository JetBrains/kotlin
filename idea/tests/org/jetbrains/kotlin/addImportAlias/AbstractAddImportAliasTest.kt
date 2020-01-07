/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.addImportAlias

import org.jetbrains.kotlin.AbstractImportsTest
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceImportAlias.KotlinIntroduceImportAliasHandler
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.sure

abstract class AbstractAddImportAliasTest : AbstractImportsTest() {
    override fun doTest(file: KtFile): String? {
        val element = findNameReferenceExpression()
        KotlinIntroduceImportAliasHandler.doRefactoring(project, editor, element)

        val importAliasNames = KotlinIntroduceImportAliasHandler.suggestedImportAliasNames.joinToString("\n")
        KotlinTestUtils.assertEqualsToFile(testDataFile(fileName().replace(".kt", ".expected.names")), importAliasNames)
        return null
    }

    private fun findNameReferenceExpression(): KtNameReferenceExpression {
        val offset = myFixture.caretOffset
        var element = file.findElementAt(offset)
        while (element != null) {
            if (element is KtNameReferenceExpression || element is KtFile) break
            if (offset !in element.range) break
            element = element.parent
        }
        return (element as? KtNameReferenceExpression).sure {
            "caret has to be placed at KtNameReferenceExpression, current position at $offset"
        }

    }
}