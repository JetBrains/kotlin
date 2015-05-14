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
import org.jetbrains.kotlin.idea.project.TargetPlatform
import org.jetbrains.kotlin.idea.test.JetLightProjectDescriptor

public abstract class AbstractKeywordCompletionTest : JetFixtureCompletionBaseTestCase() {
    override fun getPlatform() = TargetPlatform.JVM

    override fun complete(invocationCount: Int): Array<LookupElement>? {
        val items = myFixture.complete(CompletionType.BASIC) ?: return null
        return items.filter { it.getObject() is KeywordLookupObject }.toTypedArray()
    }

    override fun getProjectDescriptor() = JetLightProjectDescriptor.INSTANCE

    override fun defaultInvocationCount() = 1
}