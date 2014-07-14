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

package org.jetbrains.jet.plugin.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetBlockStringTemplateEntry
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import java.util.regex.*

public class RemoveCurlyBracesFromTemplateIntention : JetSelfTargetingIntention<JetBlockStringTemplateEntry>(
        "remove.unnecessary.curly.brackets.from.string.template", javaClass()) {

    class object {
        val pattern = Pattern.compile("[a-zA-Z0-9_].*")
    }

    override fun isApplicableTo(element: JetBlockStringTemplateEntry): Boolean {
        val nextSiblingText = element.getNextSibling()?.getText()
        if (nextSiblingText != null && pattern.matcher(nextSiblingText).matches()) return false
        return element.getExpression()?.getOriginalElement() is JetSimpleNameExpression
    }

    override fun applyTo(element: JetBlockStringTemplateEntry, editor: Editor) {
        val parent = element.getParent()
        if (parent == null) return
        val sb = StringBuilder()
        for (ch in parent.getChildren()) {
            val newText = if (ch == element) "\$${element.getExpression()?.getText()}" else "${ch.getText()}"
            sb.append(newText)
        }
        val tripleQuotes = parent.getFirstChild()?.getText()?.startsWith("\"\"\"")
        if (tripleQuotes == null) return
        val newExpression = if (tripleQuotes) "\"\"\"${sb.toString()}\"\"\"" else "\"${sb.toString()}\""
        parent.replace(JetPsiFactory(element).createExpression(newExpression))
    }
}
