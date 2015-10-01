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
import org.jetbrains.kotlin.parsing.JetExpressionParsing
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.TreeElement
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.expressions.OperatorConventions

public class JetOperationReferenceExpression(node: ASTNode) : JetSimpleNameExpressionImpl(node) {
    override fun getReferencedNameElement() = (findChildByType(JetExpressionParsing.ALL_OPERATIONS): PsiElement?) ?: this

    fun getNameForConventionalOperation(): Name? {
        val operator = (firstChild as? TreeElement)?.elementType as? JetToken ?: return null
        return OperatorConventions.getNameForOperationSymbol(operator)
    }

}
