/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import org.jetbrains.jet.plugin.refactoring.changeQualifiedName
import org.jetbrains.jet.lang.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.jet.lang.psi.psiUtil.getOutermostNonInterleavingQualifiedElement
import org.jetbrains.jet.plugin.codeInsight.addToShorteningWaitSet
import org.jetbrains.jet.plugin.refactoring.getKotlinFqName

public class JetSimpleNameReference(
        jetSimpleNameExpression: JetSimpleNameExpression
) : JetSimpleReference<JetSimpleNameExpression>(jetSimpleNameExpression) {

    override fun getRangeInElement(): TextRange = TextRange(0, getElement().getTextLength())

    public override fun handleElementRename(newElementName: String?): PsiElement? {
        if (newElementName == null) return null;

        val psiFactory = JetPsiFactory(expression)
        val element = when (expression.getReferencedNameElementType()) {
            JetTokens.FIELD_IDENTIFIER -> psiFactory.createFieldIdentifier(newElementName)
            JetTokens.LABEL_IDENTIFIER -> psiFactory.createClassLabel(newElementName)
            else -> psiFactory.createNameIdentifier(newElementName)
        }

        return expression.getReferencedNameElement().replace(element)
    }

    public enum class ShorteningMode {
        NO_SHORTENING
        DELAYED_SHORTENING
        FORCED_SHORTENING
    }

    // By default reference binding is delayed
    override fun bindToElement(element: PsiElement): PsiElement {
        return element.getKotlinFqName()?.let { fqName -> bindToFqName(fqName) } ?: expression
    }

    public fun bindToFqName(fqName: FqName, shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING): PsiElement {
        if (fqName.isRoot()) return expression

        val newExpression = expression.changeQualifiedName(fqName).getQualifiedElementSelector() as JetSimpleNameExpression
        val newQualifiedElement = newExpression.getOutermostNonInterleavingQualifiedElement()

        if (shorteningMode == ShorteningMode.NO_SHORTENING) return newExpression

        val needToShorten =
                PsiTreeUtil.getParentOfType(expression, javaClass<JetImportDirective>(), javaClass<JetPackageDirective>()) == null
        if (needToShorten) {
            if (shorteningMode == ShorteningMode.FORCED_SHORTENING) {
                ShortenReferences.process(newQualifiedElement)
            }
            else {
                newQualifiedElement.addToShorteningWaitSet()
            }
        }

        return newExpression
    }

    override fun toString(): String {
        return javaClass<JetSimpleNameReference>().getSimpleName() + ": " + expression.getText()
    }
}
