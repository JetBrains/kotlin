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

import org.jetbrains.jet.plugin.intentions.JetSelfTargetingIntention
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression


public class BracketToGetIntention : JetSelfTargetingIntention<JetArrayAccessExpression>("bracket.to.get", javaClass()) {
    override fun isApplicableTo(element: JetArrayAccessExpression): Boolean {
        return element is JetArrayAccessExpression
    }

    override fun applyTo(element: JetArrayAccessExpression, editor: Editor) {
        val array = element.getArrayExpression()!!
        val indices = element.getIndicesNode()

        val arrayText = array.getText()
        val indicesText = indices.getText()!!.trim("[", "]")
        val transformation = "${arrayText}.get(${indicesText})"

        val transformed = JetPsiFactory.createExpression(element.getProject(), transformation)
        element.replace(transformed)
    }
}
