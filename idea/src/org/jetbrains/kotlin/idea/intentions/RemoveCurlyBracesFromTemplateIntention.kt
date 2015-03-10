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
import org.jetbrains.kotlin.psi.JetBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import java.util.regex.*
import org.jetbrains.kotlin.psi.JetStringTemplateEntryWithExpression

public class RemoveCurlyBracesFromTemplateIntention : JetSelfTargetingIntention<JetBlockStringTemplateEntry>(
        "remove.unnecessary.curly.brackets.from.string.template", javaClass()) {

    default object {
        val INSTANCE = RemoveCurlyBracesFromTemplateIntention()
        val pattern = Pattern.compile("[a-zA-Z0-9_].*")
    }

    override fun isApplicableTo(element: JetBlockStringTemplateEntry): Boolean {
        val nextSiblingText = element.getNextSibling()?.getText()
        if (nextSiblingText != null && pattern.matcher(nextSiblingText).matches()) return false
        return element.getExpression() is JetSimpleNameExpression
    }

    fun convertIfApplicable(element: JetBlockStringTemplateEntry): JetStringTemplateEntryWithExpression {
        if (!isApplicableTo(element)) return element

        val name = (element.getExpression() as JetSimpleNameExpression).getReferencedName()
        val newEntry = JetPsiFactory(element).createSimpleNameStringTemplateEntry(name)
        return element.replace(newEntry) as JetStringTemplateEntryWithExpression
    }

    override fun applyTo(element: JetBlockStringTemplateEntry, editor: Editor) {
        convertIfApplicable(element)
    }
}
