/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
