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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.resolveAllReferences
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

public class MoveAssignmentToInitializerIntention :
        JetSelfTargetingIntention<JetBinaryExpression>(javaClass(), "Move assignment to initializer") {

    override fun isApplicableTo(element: JetBinaryExpression, caretOffset: Int): Boolean {
        if (element.operationToken != JetTokens.EQ) {
            return false
        }
        val rhs = element.right ?: return false

        val block = PsiTreeUtil.getParentOfType(element,
                                                JetClassInitializer::class.java,
                                                JetSecondaryConstructor::class.java) ?: return false

        val target = findTargetProperty(element)
        return target != null && target.initializer == null && target.receiverTypeReference == null &&
               target.getNonStrictParentOfType<JetClassOrObject>() == element.getNonStrictParentOfType<JetClassOrObject>() &&
               hasNoLocalDependencies(rhs, block)

    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        val prop = findTargetProperty(element) ?: return
        val initializer = element.right ?: return
        prop.setInitializer(initializer)

        val initializerBlock = element.getStrictParentOfType<JetClassInitializer>()
        element.delete()
        if (initializerBlock != null && (initializerBlock.body as? JetBlockExpression)?.isEmpty() == true) {
            initializerBlock.delete()
        }
    }

    private fun findTargetProperty(expr: JetBinaryExpression): JetProperty? {
        val lhs = expr.left as? JetSimpleNameExpression ?: return null
        return lhs.resolveAllReferences().firstIsInstanceOrNull<JetProperty>()
    }

    fun JetBlockExpression.isEmpty(): Boolean {
        var node = node?.firstChildNode
        while (node != null) {
            if (node.elementType != TokenType.WHITE_SPACE &&
                node.elementType != JetTokens.LBRACE &&
                node.elementType != JetTokens.RBRACE) {
                return false
            }
            node = node.treeNext
        }
        return true
    }

    private fun hasNoLocalDependencies(element: JetElement, localContext: PsiElement): Boolean {
        return !element.anyDescendantOfType<PsiElement> { child ->
            child.resolveAllReferences().any { it != null && PsiTreeUtil.isAncestor(localContext, it, false) }
        }
    }
}
