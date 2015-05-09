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
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetIfExpression
import org.jetbrains.kotlin.psi.JetWhenExpression

public class FoldIfToAssignmentIntention : JetSelfTargetingOffsetIndependentIntention<JetIfExpression>(javaClass(), "Replace 'if' expression with assignment") {
    override fun isApplicableTo(element: JetIfExpression): Boolean {
        return BranchedFoldingUtils.checkFoldableIfExpressionWithAssignments(element)
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        BranchedFoldingUtils.foldIfExpressionWithAssignments(element)
    }
}
        
public class FoldIfToReturnAsymmetricallyIntention : JetSelfTargetingOffsetIndependentIntention<JetIfExpression>(javaClass(), "Replace 'if' expression with return") {
    override fun isApplicableTo(element: JetIfExpression): Boolean {
        return BranchedFoldingUtils.checkFoldableIfExpressionWithAsymmetricReturns(element)
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        BranchedFoldingUtils.foldIfExpressionWithAsymmetricReturns(element)
    }
}
        
public class FoldIfToReturnIntention : JetSelfTargetingOffsetIndependentIntention<JetIfExpression>(javaClass(), "Replace 'if' expression with return") {
    override fun isApplicableTo(element: JetIfExpression): Boolean {
        return BranchedFoldingUtils.checkFoldableIfExpressionWithReturns(element)
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        BranchedFoldingUtils.foldIfExpressionWithReturns(element)
    }
}
        
public class FoldWhenToAssignmentIntention : JetSelfTargetingOffsetIndependentIntention<JetWhenExpression>(javaClass(), "Replace 'when' expression with assignment") {
    override fun isApplicableTo(element: JetWhenExpression): Boolean {
        return BranchedFoldingUtils.checkFoldableWhenExpressionWithAssignments(element)
    }

    override fun applyTo(element: JetWhenExpression, editor: Editor) {
        BranchedFoldingUtils.foldWhenExpressionWithAssignments(element)
    }
}
        
public class FoldWhenToReturnIntention : JetSelfTargetingOffsetIndependentIntention<JetWhenExpression>(javaClass(), "Replace 'when' expression with return") {
    override fun isApplicableTo(element: JetWhenExpression): Boolean {
        return BranchedFoldingUtils.checkFoldableWhenExpressionWithReturns(element)
    }

    override fun applyTo(element: JetWhenExpression, editor: Editor) {
        BranchedFoldingUtils.foldWhenExpressionWithReturns(element)
    }
}