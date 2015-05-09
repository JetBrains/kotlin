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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedFoldingUtils
import org.jetbrains.kotlin.psi.JetIfExpression
import org.jetbrains.kotlin.psi.JetPsiFactory

public class FoldIfToReturnIntention : JetSelfTargetingOffsetIndependentIntention<JetIfExpression>(javaClass(), "Replace 'if' expression with return") {
    override fun isApplicableTo(element: JetIfExpression): Boolean {
        return BranchedFoldingUtils.getFoldableBranchedReturn(element.getThen()) != null
               && BranchedFoldingUtils.getFoldableBranchedReturn(element.getElse()) != null
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val newReturnExpression = JetPsiFactory(element).createReturn(element)
        val newIfExpression = newReturnExpression.getReturnedExpression() as JetIfExpression

        val thenReturn = BranchedFoldingUtils.getFoldableBranchedReturn(newIfExpression.getThen()!!)!!
        val elseReturn = BranchedFoldingUtils.getFoldableBranchedReturn(newIfExpression.getElse()!!)!!

        val thenExpr = thenReturn.getReturnedExpression()!!
        val elseExpr = elseReturn.getReturnedExpression()!!

        thenReturn.replace(thenExpr)
        elseReturn.replace(elseExpr)

        element.replace(newReturnExpression)
    }
}