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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations

import com.google.common.base.Predicate
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*

import java.util.ArrayList

object BranchedFoldingUtils {
    private val CHECK_ASSIGNMENT = object : Predicate<JetElement> {
        override fun apply(input: JetElement): Boolean {
            if (!JetPsiUtil.isAssignment(input)) return false

            val assignment = input as JetBinaryExpression

            val left = assignment.getLeft() as? JetSimpleNameExpression ?: return false
            if (assignment.getRight() == null) return false

            val parent = assignment.getParent()
            if (parent is JetBlockExpression) {
                return !JetPsiUtil.checkVariableDeclarationInBlock(parent, left.getText())
            }

            return true
        }
    }

    public fun getFoldableBranchedAssignment(branch: JetExpression?): JetBinaryExpression? {
        return JetPsiUtil.getOutermostLastBlockElement(branch, CHECK_ASSIGNMENT) as JetBinaryExpression?
    }

    public fun getFoldableBranchedReturn(branch: JetExpression?): JetReturnExpression? {
        return JetPsiUtil.getOutermostLastBlockElement(branch) {
            (it as? JetReturnExpression)?.getReturnedExpression() != null
        } as JetReturnExpression?
    }

    public fun checkAssignmentsMatch(a1: JetBinaryExpression, a2: JetBinaryExpression): Boolean {
        return a1.getLeft()?.getText() == a2.getLeft()?.getText() && a1.getOperationToken() == a2.getOperationToken()
    }
}
