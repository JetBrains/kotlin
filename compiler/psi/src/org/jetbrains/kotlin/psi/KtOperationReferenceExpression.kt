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

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.TreeElement
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.parsing.KotlinExpressionParsing
import org.jetbrains.kotlin.types.expressions.OperatorConventions

class KtOperationReferenceExpression(node: ASTNode) : KtSimpleNameExpressionImpl(node) {
    override fun getReferencedNameElement() = findChildByType<PsiElement?>(KotlinExpressionParsing.ALL_OPERATIONS) ?: this

    val operationSignTokenType: KtSingleValueToken?
        get() = (firstChild as? TreeElement)?.elementType as? KtSingleValueToken

    fun isConventionOperator(): Boolean {
        val tokenType = operationSignTokenType ?: return false
        return OperatorConventions.getNameForOperationSymbol(tokenType) != null
    }
}
