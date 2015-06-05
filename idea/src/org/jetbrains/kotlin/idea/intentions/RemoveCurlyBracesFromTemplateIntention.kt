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
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.JetBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetStringTemplateEntryWithExpression
import java.util.regex.Pattern

public class RemoveCurlyBracesFromTemplateInspection : IntentionBasedInspection<JetBlockStringTemplateEntry>(RemoveCurlyBracesFromTemplateIntention())

public class RemoveCurlyBracesFromTemplateIntention : JetSelfTargetingOffsetIndependentIntention<JetBlockStringTemplateEntry>(javaClass(), "Remove curly braces") {
    override fun isApplicableTo(element: JetBlockStringTemplateEntry): Boolean {
        if (element.getExpression() !is JetSimpleNameExpression) return false
        val nextSiblingText = element.getNextSibling()?.getText()
        return nextSiblingText == null || !pattern.matcher(nextSiblingText).matches()
    }

    override fun applyTo(element: JetBlockStringTemplateEntry, editor: Editor) {
        applyTo(element)
    }

    public fun applyTo(element: JetBlockStringTemplateEntry): JetStringTemplateEntryWithExpression {
        val name = (element.getExpression() as JetSimpleNameExpression).getReferencedName()
        val newEntry = JetPsiFactory(element).createSimpleNameStringTemplateEntry(name)
        return element.replaced(newEntry)
    }

    companion object {
        private val pattern = Pattern.compile("[a-zA-Z0-9_].*")
    }
}
