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

package org.jetbrains.jet.plugin.editor.fixers

import org.jetbrains.jet.lang.psi.JetIfExpression
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetWhileExpression

public class KotlinWhileConditionFixer: MissingConditionFixer<JetWhileExpression>() {
    override val keyword = "while"
    override fun getElement(element: PsiElement?) = element as? JetWhileExpression
    override fun getCondition(element: JetWhileExpression) = element.getCondition()
    override fun getLeftParenthesis(element: JetWhileExpression) = element.getLeftParenthesis()
    override fun getRightParenthesis(element: JetWhileExpression) = element.getRightParenthesis()
    override fun getBody(element: JetWhileExpression) = element.getBody()
}