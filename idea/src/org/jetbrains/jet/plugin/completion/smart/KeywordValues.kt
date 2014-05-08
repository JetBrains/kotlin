/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.jet.plugin.completion.ExpectedInfo
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.jet.lang.types.TypeUtils

object KeywordValues {
    public fun addToCollection(collection: MutableCollection<LookupElement>, expectedInfos: Collection<ExpectedInfo>) {
        if (expectedInfos.any { it.`type` == KotlinBuiltIns.getInstance().getBooleanType() }) {
            collection.add(LookupElementBuilder.create("true").bold())
            collection.add(LookupElementBuilder.create("false").bold())
        }

        if (expectedInfos.any { it.`type`.isNullable() }) {
            collection.add(LookupElementBuilder.create("null").bold())
        }
    }
}