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
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetWhileExpression
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.lang.psi.JetDoWhileExpression
import org.jetbrains.jet.lang.psi.JetForExpression

public class RemoveBracesIntention : JetSelfTargetingIntention<JetBlockExpression>("remove.braces", javaClass()) {

    override fun isApplicableTo(element: JetBlockExpression): Boolean {
        if (element.getStatements().size() != 1 ) {
            return false
        }

        val parentContext = element.getParent()?.getContext()

        var conditionText = when (parentContext) {
            is JetIfExpression -> "if"
            is JetWhileExpression -> "while"
            is JetDoWhileExpression -> "do...while"
            is JetForExpression -> "for"
            else -> return false
        }

        setText("Remove braces from '$conditionText' statement")

        return true
    }

    override fun applyTo(element: JetBlockExpression, editor: Editor) {
        val newElement = element.replace(JetPsiFactory.createExpression(element.getProject(), element.getStatements().first?.getText()))

        if (newElement.getParent()?.getContext() is JetDoWhileExpression) {
            newElement.getParent()?.addAfter(JetPsiFactory.createNewLine(element.getProject()), newElement)
        }
    }
}