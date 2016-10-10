/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class ConvertEnumToSealedClassIntention : SelfTargetingRangeIntention<KtClass>(KtClass::class.java, "Convert to sealed class") {
    override fun applicabilityRange(element: KtClass): TextRange? {
        val nameIdentifier = element.nameIdentifier ?: return null
        val enumKeyword = element.modifierList?.getModifier(KtTokens.ENUM_KEYWORD) ?: return null
        return TextRange(enumKeyword.startOffset, nameIdentifier.endOffset)
    }

    override fun applyTo(element: KtClass, editor: Editor?) {
        element.removeModifier(KtTokens.ENUM_KEYWORD)
        element.addModifier(KtTokens.SEALED_KEYWORD)

        val psiFactory = KtPsiFactory(element)

        for (member in element.declarations) {
            if (member !is KtEnumEntry) continue

            val obj = psiFactory.createDeclaration<KtObjectDeclaration>("object ${member.name}")

            val initializers = member.initializerList?.initializers ?: emptyList()
            if (initializers.isNotEmpty()) {
                initializers.forEach { obj.addSuperTypeListEntry(psiFactory.createSuperTypeCallEntry("${element.name}${it.text}")) }
            }
            else {
                obj.addSuperTypeListEntry(psiFactory.createSuperTypeCallEntry("${element.name}()"))
            }

            member.getBody()?.let { body -> obj.add(body) }

            member.delete()
            element.addDeclaration(obj)
        }

        element.getBody()?.let { body ->
            val semicolon = body
                    .allChildren
                    .takeWhile { it !is KtDeclaration }
                    .firstOrNull { it.node.elementType == KtTokens.SEMICOLON }
            if (semicolon != null) {
                val nonWhiteSibling = semicolon.siblings(forward = true, withItself = false).firstOrNull { it !is PsiWhiteSpace }
                body.deleteChildRange(semicolon, nonWhiteSibling?.prevSibling ?: semicolon)
                if (nonWhiteSibling != null) {
                    CodeStyleManager.getInstance(element.project).reformat(nonWhiteSibling.firstChild ?: nonWhiteSibling)
                }
            }
        }
    }
}
