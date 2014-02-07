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

import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetValueArgumentList
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetCallExpression

public class QualifiedGetToBracketsIntention : JetSelfTargetingIntention<JetDotQualifiedExpression>("qualified.get.to.brackets", javaClass()) {

    override fun isApplicableTo(element: JetDotQualifiedExpression): Boolean {
        val selector = element.getSelectorExpression()

        return selector is JetCallExpression &&
               selector.getCalleeExpression()?.getText() == "get" &&
               selector.getValueArguments().isNotEmpty() &&
               selector.getValueArguments().all({arg -> arg?.isNamed() == false})

    }

    override fun applyTo(element: JetDotQualifiedExpression, editor: Editor) {

        val receiver = element.getReceiverExpression()
        val selector = element.getSelectorExpression()
        if (selector is JetCallExpression) {
            val params = selector.getValueArgumentList()
            params?.getLeftParenthesis()?.delete()
            params?.getRightParenthesis()?.delete()
            val bracketAccessor = JetPsiFactory.createExpression(params?.getProject(), receiver.getText() + "[" + params?.getText() + "]")
            element.replace(bracketAccessor)
        }
    }
}
