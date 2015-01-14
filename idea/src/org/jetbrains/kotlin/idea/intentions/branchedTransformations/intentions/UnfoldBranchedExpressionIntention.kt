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

import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingIntention
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetReturnExpression
import org.jetbrains.kotlin.psi.JetProperty

public open class UnfoldBranchedExpressionIntention<T: JetExpression>(
        val kind: UnfoldableKind, elementType: Class<T>
) : JetSelfTargetingIntention<T>(kind.getKey(), elementType) {
    override fun isApplicableTo(element: T): Boolean = BranchedUnfoldingUtils.getUnfoldableExpressionKind(element) == kind

    override fun applyTo(element: T, editor: Editor) {
        val file = element.getContainingFile()
        if (file is JetFile) {
            kind.transform(element, editor, file)
        }
    }
}

public class UnfoldAssignmentToIfIntention : UnfoldBranchedExpressionIntention<JetBinaryExpression>(
        UnfoldableKind.ASSIGNMENT_TO_IF, javaClass()
)

public class UnfoldPropertyToIfIntention : UnfoldBranchedExpressionIntention<JetProperty>(
        UnfoldableKind.PROPERTY_TO_IF, javaClass()
)

public class UnfoldAssignmentToWhenIntention : UnfoldBranchedExpressionIntention<JetBinaryExpression>(
        UnfoldableKind.ASSIGNMENT_TO_WHEN, javaClass()
)

public class UnfoldPropertyToWhenIntention : UnfoldBranchedExpressionIntention<JetProperty>(
        UnfoldableKind.PROPERTY_TO_WHEN, javaClass()
)

public class UnfoldReturnToIfIntention : UnfoldBranchedExpressionIntention<JetReturnExpression>(
        UnfoldableKind.RETURN_TO_IF, javaClass()
)

public class UnfoldReturnToWhenIntention : UnfoldBranchedExpressionIntention<JetReturnExpression>(
        UnfoldableKind.RETURN_TO_WHEN, javaClass()
)
