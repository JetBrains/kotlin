/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.idea.completion.KeywordLookupObject
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinProjectDescriptorWithFacet
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

abstract class AbstractKeywordCompletionTest : KotlinFixtureCompletionBaseTestCase() {
    override fun getPlatform() = JvmPlatform

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