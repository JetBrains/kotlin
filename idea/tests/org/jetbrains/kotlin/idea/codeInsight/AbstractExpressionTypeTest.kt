/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractExpressionTypeTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getBasePath() = PluginTestCaseBase.TEST_DATA_PROJECT_RELATIVE + "/codeInsight/expressionType"

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected fun doTest(path: String) {
        myFixture.configureByFile(path)
        val expressionTypeProvider = KotlinExpressionTypeProvider()
        val elementAtCaret = myFixture.file.findElementAt(myFixture.editor.caretModel.offset)!!
        val expressions = expressionTypeProvider.getExpressionsAt(elementAtCaret)
        val types = expressions.map { "${it.text.replace('\n', ' ')} -> ${expressionTypeProvider.getInformationHint(it)}" }
        val expectedTypes = InTextDirectivesUtils.findLinesWithPrefixesRemoved(myFixture.file.text, "// TYPE: ")
        UsefulTestCase.assertOrderedEquals(types, expectedTypes)
    }
}
