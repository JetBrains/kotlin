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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.core.canDropBraces
import org.jetbrains.kotlin.idea.core.dropBraces
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry

class RemoveCurlyBracesFromTemplateInspection : IntentionBasedInspection<KtBlockStringTemplateEntry>(RemoveCurlyBracesFromTemplateIntention::class)

class RemoveCurlyBracesFromTemplateIntention : SelfTargetingOffsetIndependentIntention<KtBlockStringTemplateEntry>(KtBlockStringTemplateEntry::class.java, "Remove curly braces") {
    override fun isApplicableTo(element: KtBlockStringTemplateEntry) = element.canDropBraces()

    override fun applyTo(element: KtBlockStringTemplateEntry, editor: Editor?) {
        element.dropBraces()
    }
}
