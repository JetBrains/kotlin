/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.idea.completion.KeywordLookupObject
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinProjectDescriptorWithFacet
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms

abstract class AbstractKeywordCompletionTest : KotlinFixtureCompletionBaseTestCase() {
    override fun getPlatform() = DefaultBuiltInPlatforms.jvmPlatform

    override fun defaultCompletionType() = CompletionType.BASIC

    override fun complete(completionType: CompletionType, invocationCount: Int): Array<LookupElement>? {
        val items = myFixture.complete(completionType) ?: return null
        return items.filter { it.`object` is KeywordLookupObject }.toTypedArray()
    }

    override fun getProjectDescriptor(): KotlinLightProjectDescriptor {
        when {
            "LangLevel10" in fileName() -> return KotlinProjectDescriptorWithFacet.KOTLIN_10
            "LangLevel11" in fileName() -> return KotlinProjectDescriptorWithFacet.KOTLIN_11
            else -> return KotlinProjectDescriptorWithFacet.KOTLIN_STABLE_WITH_MULTIPLATFORM
        }
    }

    override fun defaultInvocationCount() = 1
}