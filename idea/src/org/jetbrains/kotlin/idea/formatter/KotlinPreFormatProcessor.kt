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

package org.jetbrains.kotlin.idea.formatter

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.codeStyle.PreFormatProcessor
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.nextSiblingOfSameType
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

private class Visitor(var range: TextRange) : KtTreeVisitorVoid() {
    override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
        fun PsiElement.containsToken(type: IElementType) = allChildren.any { it.node.elementType == type }

        if (!range.contains(declaration.textRange)) return

        val classBody = declaration.parent as? KtClassBody ?: return
        val klass = classBody.parent as? KtClass ?: return
        if (!klass.isEnum()) return

        var delta = 0

        val psiFactory = KtPsiFactory(klass)
        if (declaration is KtEnumEntry) {
            val comma = psiFactory.createComma()

            val nextEntry = declaration.nextSiblingOfSameType()
            if (nextEntry != null && !declaration.containsToken(KtTokens.COMMA)) {
                declaration.add(comma)
                delta += comma.textLength
            }
        } else {
            val lastEntry = klass.declarations.lastIsInstanceOrNull<KtEnumEntry>()
            if (lastEntry != null &&
                (lastEntry.containsToken(KtTokens.SEMICOLON) || lastEntry.nextSibling?.node?.elementType == KtTokens.SEMICOLON)
            ) return
            if (lastEntry == null && classBody.containsToken(KtTokens.SEMICOLON)) return

            val semicolon = psiFactory.createSemicolon()
            delta += if (lastEntry != null) {
                classBody.addAfter(semicolon, lastEntry)
                semicolon.textLength
            } else {
                val newLine = psiFactory.createNewLine()
                classBody.addAfter(semicolon, classBody.lBrace)
                classBody.addAfter(psiFactory.createNewLine(), classBody.lBrace)
                semicolon.textLength + newLine.textLength
            }
        }

        range = TextRange(range.startOffset, range.endOffset + delta)
    }
}

class KotlinPreFormatProcessor : PreFormatProcessor {
    override fun process(element: ASTNode, range: TextRange): TextRange {
        val psi = element.psi ?: return range
        if (!psi.isValid) return range
        if (psi.containingFile !is KtFile) return range
        return Visitor(range).apply { psi.accept(this) }.range
    }
}
