/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractExpressionTypeTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected fun doTest(path: String) {
        myFixture.configureByFile(fileName())
        val expressionTypeProvider = KotlinExpressionTypeProvider()
        val elementAtCaret = myFixture.file.findElementAt(myFixture.editor.caretModel.offset)!!
        val expressions = expressionTypeProvider.getExpressionsAt(elementAtCaret)
        val types = expressions.map { "${it.text.replace('\n', ' ')} -> ${expressionTypeProvider.getInformationHint(it)}" }
        val expectedTypes = InTextDirectivesUtils.findLinesWithPrefixesRemoved(myFixture.file.text, "// TYPE: ")
        UsefulTestCase.assertOrderedEquals(types, expectedTypes)
    }
}
