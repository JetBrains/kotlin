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

package org.jetbrains.jet.plugin.intentions.branchedTransformations.intentions

import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.jet.plugin.intentions.JetSelfTargetingIntention
import org.jetbrains.jet.plugin.intentions.branchedTransformations.BranchedFoldingUtils
import org.jetbrains.jet.plugin.intentions.branchedTransformations.FoldableKind
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetIfExpression
import org.jetbrains.kotlin.psi.JetWhenExpression

public open class FoldBranchedExpressionIntention<T: JetExpression>(
        val kind: FoldableKind, elementType: Class<T>
) : JetSelfTargetingIntention<T>(kind.getKey(), elementType) {
    override fun isApplicableTo(element: T): Boolean = BranchedFoldingUtils.getFoldableExpressionKind(element) == kind

    override fun applyTo(element: T, editor: Editor) {
        val file = element.getContainingFile()
        if (file is JetFile) {
            kind.transform(element, editor, file)
        }
    }
}

public class FoldIfToAssignmentIntention : FoldBranchedExpressionIntention<JetIfExpression>(FoldableKind.IF_TO_ASSIGNMENT, javaClass())
        
public class FoldIfToReturnAsymmetricallyIntention : FoldBranchedExpressionIntention<JetIfExpression>(
        FoldableKind.IF_TO_RETURN_ASYMMETRICALLY, javaClass()
)
        
public class FoldIfToReturnIntention : FoldBranchedExpressionIntention<JetIfExpression>(FoldableKind.IF_TO_RETURN, javaClass())
        
public class FoldWhenToAssignmentIntention : FoldBranchedExpressionIntention<JetWhenExpression>(
        FoldableKind.WHEN_TO_ASSIGNMENT, javaClass()
)
        
public class FoldWhenToReturnIntention : FoldBranchedExpressionIntention<JetWhenExpression>(FoldableKind.WHEN_TO_RETURN, javaClass())
