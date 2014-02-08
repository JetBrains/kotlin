/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import org.jetbrains.jet.plugin.intentions.JetSelfTargetingIntention
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetValueArgument


public class GetToBracketIntention : JetSelfTargetingIntention<JetDotQualifiedExpression>("get.to.bracket", javaClass()) {
    override fun isApplicableTo(element: JetDotQualifiedExpression): Boolean {
        var selector = element.getSelectorExpression()

        if (selector is JetCallExpression) {
            var text = (selector as JetCallExpression).getText()
            var arguments = (selector as JetCallExpression).getValueArgumentList()?.getArguments()
            return text?.startsWith("get") as Boolean && !arguments!!.any { it.isNamed() }
        } else {
            return false
        }
    }

    override fun applyTo(element: JetDotQualifiedExpression, editor: Editor) {
        var selector = element.getSelectorExpression()
        var receiver = element.getReceiverExpression().getText()

        var arguments = (selector as JetCallExpression).getValueArgumentList()?.getArguments()
        var project = element.getProject()

        var brackets = arguments?.map { it.getText() } ?.makeString(", ", "[", "]")
        var transformation = receiver + brackets
        var transformed = JetPsiFactory.createExpression(project, transformation)
        element.replace(transformed)
    }
}
