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

package org.jetbrains.kotlin.idea.codeInsight.postfix

import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File


abstract class AbstractPostfixTemplateProviderTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    override fun getTestDataPath() = KotlinTestUtils.getHomeDirectory()


    override fun setUp() {
        super.setUp()
        KtPostfixTemplateProvider.previouslySuggestedExpressions = emptyList()
    }

    protected fun doTest(fileName: String) {

        myFixture.configureByFile(fileName)
        myFixture.type("\t")

        val previouslySuggestedExpressions = KtPostfixTemplateProvider.previouslySuggestedExpressions
        if (previouslySuggestedExpressions.size > 1 && !InTextDirectivesUtils.isDirectiveDefined(File(fileName).readText(), "ALLOW_MULTIPLE_EXPRESSIONS")) {
            fail("Only one expression should be suggested, but $previouslySuggestedExpressions were found")
        }

        myFixture.checkResultByFile(fileName + ".after")
    }

    override fun tearDown() {
        super.tearDown()
        KtPostfixTemplateProvider.previouslySuggestedExpressions = emptyList()
    }
}
