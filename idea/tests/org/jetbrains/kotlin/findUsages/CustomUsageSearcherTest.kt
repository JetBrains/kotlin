/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.findUsages

import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCaseBase

class CustomUsageSearcherTest : KotlinLightCodeInsightFixtureTestCaseBase() {

    fun testAddCustomUsagesForKotlin() {
//        TODO("[VD] There are diff API for ExtensionTestUtil in 191, 192, 193")
//        val customUsageSearcher = object : CustomUsageSearcher() {
//            override fun processElementUsages(element: PsiElement, processor: Processor<Usage>, options: FindUsagesOptions) {
//                runReadAction { processor.process(UsageInfo2UsageAdapter(UsageInfo(element))) }
//            }
//        }
//        // ExtensionTestUtil.maskExtensions(CustomUsageSearcher.EP_NAME, listOf(customUsageSearcher), testRootDisposable)
//        myFixture.configureByText(KotlinFileType.INSTANCE, """val <caret>selfUsed = 1""")
//
//        val usages = myFixture.getUsageViewTreeTextRepresentation(myFixture.elementAtCaret)
//        assertTrue(usages.contains("val selfUsed"))
    }
}