/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions.declarations

import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.JavaElementType
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class ConvertSealedSubClassToObjectIntention :
        SelfTargetingRangeIntention<KtElement>(KtElement::class.java, "Convert Sealed Sub-class to Object") {

    companion object {
        val JAVA_LANG = Language.findLanguageByID("JAVA")
        val KOTLIN_LANG = Language.findLanguageByID("kotlin")
    }

    override fun applyTo(element: KtElement, editor: Editor?) {
        val klass = element.getParentOfType<KtClass>(false)?: return

        changeInstances(klass)
        changeDeclaration(klass)
    }

    override fun applicabilityRange(element: KtElement): TextRange? {
        return element?.textRange
    }

    /**
     * Changes declaration of class to object.
     */
    private inline fun changeDeclaration(element: KtClass) {
        val declaration = KtPsiFactory(element).buildDeclaration {
            appendFixedText("${KtTokens.OBJECT_KEYWORD.value} ")
            appendName(element.nameAsSafeName)
            appendFixedText(" ${KtTokens.COLON.value} ")
            appendFixedText(element.getSuperTypeList()?.text ?: "")
        }

        element.replace(declaration)
    }

    /**
     * Replace instantiations of the class with links to the singleton instance of the object.
     */
    private inline fun changeInstances(klass: KtClass) {
        mapReferencesByLanguage(klass)
                .apply {
                    replaceKotlin(klass)
                    replaceJava(klass)
                }
    }

    /**
     * Map references to this class by language
     */
    private inline fun mapReferencesByLanguage(klass: KtClass) = ReferencesSearch.search(klass)
            .groupBy({ it.element.language }, { it.element.parent })

    /**
     * Replace Kotlin instantiations to a straightforward call to the singleton.
     */
    private inline fun Map<Language, List<PsiElement>>.replaceKotlin(klass: KtClass) {
        val list = this[KOTLIN_LANG]?: return
        val singletonCall = KtPsiFactory(klass).buildExpression { appendName(klass.nameAsSafeName) }

        list.filter { it.node.elementType == KtNodeTypes.CALL_EXPRESSION }
                .forEach { it.replace(singletonCall) }
    }

    /**
     * Replace Java instantiations to an instance of the object, unless it is the only thing
     * done in the statement, in which IDEA will consider wrong, so I delete the line.
     */
    private inline fun Map<Language, List<PsiElement>>.replaceJava(klass: KtClass) {
        val list = this[JAVA_LANG]?: return
        val first = list.firstOrNull() ?: return
        val elementFactory = JavaPsiFacade.getElementFactory(klass.project)
        val javaSingletonCall = elementFactory.createExpressionFromText("${klass.name}.INSTANCE", first)

        list.filter { it.node.elementType == JavaElementType.NEW_EXPRESSION }
                .forEach {
                    when (it.parent.node.elementType) {
                        JavaElementType.EXPRESSION_STATEMENT -> it.delete()
                        else -> it.replace(javaSingletonCall)
                    }
                }
    }
}