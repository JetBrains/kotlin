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
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedUnfoldingUtils
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

public class UnfoldAssignmentToIfIntention : JetSelfTargetingOffsetIndependentIntention<JetBinaryExpression>(javaClass(), "Replace assignment with 'if' expression") {
    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        if (element.getOperationToken() !in JetTokens.ALL_ASSIGNMENTS) return false
        if (element.getLeft() == null) return false
        return element.getRight() is JetIfExpression
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        BranchedUnfoldingUtils.unfoldAssignmentToIf(element, editor)
    }
}

public class UnfoldPropertyToIfIntention : JetSelfTargetingOffsetIndependentIntention<JetProperty>(javaClass(), "Replace property initializer with 'if' expression") {
    override fun isApplicableTo(element: JetProperty): Boolean {
        if (!element.isLocal()) return false
        return element.getInitializer() is JetIfExpression
    }

    override fun applyTo(element: JetProperty, editor: Editor) {
        BranchedUnfoldingUtils.unfoldPropertyToIf(element, editor)
    }
}

public class UnfoldAssignmentToWhenIntention : JetSelfTargetingOffsetIndependentIntention<JetBinaryExpression>(javaClass(), "Replace assignment with 'when' expression" ) {
    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        if (element.getOperationToken() !in JetTokens.ALL_ASSIGNMENTS) return false
        if (element.getLeft() == null) return false
        val right = element.getRight()
        return right is JetWhenExpression && JetPsiUtil.checkWhenExpressionHasSingleElse(right)
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        BranchedUnfoldingUtils.unfoldAssignmentToWhen(element, editor)
    }
}

public class UnfoldPropertyToWhenIntention : JetSelfTargetingOffsetIndependentIntention<JetProperty>(javaClass(), "Replace property initializer with 'when' expression") {
    override fun isApplicableTo(element: JetProperty): Boolean {
        if (!element.isLocal()) return false
        val initializer = element.getInitializer()
        return initializer is JetWhenExpression && JetPsiUtil.checkWhenExpressionHasSingleElse(initializer)
    }

    override fun applyTo(element: JetProperty, editor: Editor) {
        BranchedUnfoldingUtils.unfoldPropertyToWhen(element, editor)
    }
}

public class UnfoldReturnToIfIntention : JetSelfTargetingOffsetIndependentIntention<JetReturnExpression>(javaClass(), "Replace return with 'if' expression") {
    override fun isApplicableTo(element: JetReturnExpression): Boolean {
        return element.getReturnedExpression() is JetIfExpression
    }

    override fun applyTo(element: JetReturnExpression, editor: Editor) {
        BranchedUnfoldingUtils.unfoldReturnToIf(element)
    }
}

public class UnfoldReturnToWhenIntention : JetSelfTargetingOffsetIndependentIntention<JetReturnExpression>(javaClass(), "Replace return with 'when' expression") {
    override fun isApplicableTo(element: JetReturnExpression): Boolean {
        val expr = element.getReturnedExpression()
        return expr is JetWhenExpression && JetPsiUtil.checkWhenExpressionHasSingleElse(expr)
    }

    override fun applyTo(element: JetReturnExpression, editor: Editor) {
        BranchedUnfoldingUtils.unfoldReturnToWhen(element)
    }
}
