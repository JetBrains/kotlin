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
import java.util.regex.Pattern

public class QualifiedGetToBracketsIntention : JetSelfTargetingIntention<JetDotQualifiedExpression>("qualified.get.to.brackets", javaClass()) {
    val assignPattern = Pattern.compile("[^=]=[^=]")

    override fun isApplicableTo(element: JetDotQualifiedExpression): Boolean {
        val selector = element.getSelectorExpression();
        if (selector?.getFirstChild()?.getText() != "get") return false
        val params = selector?.getLastChild()

        return when (params) {
            is JetValueArgumentList -> {
                if (!params.getArguments().isEmpty() && params.getRightParenthesis() != null) {
                    val paramsCharSeq = params.getText()?.get(0,params.getTextLength())
                    when (paramsCharSeq) {
                        is CharSequence -> {
                            val containsAssignment = assignPattern?.matcher(paramsCharSeq).find()
                            !containsAssignment
                        }
                        else -> { false }
                    }
                } else {
                    false
                }
            }
            else -> { false }
        }
    }

    override fun applyTo(element: JetDotQualifiedExpression, editor: Editor) {
    }
}
