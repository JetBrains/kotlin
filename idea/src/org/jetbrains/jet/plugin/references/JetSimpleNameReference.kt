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
import org.jetbrains.jet.lang.psi.psiUtil.getFqName
import org.jetbrains.jet.plugin.refactoring.changeQualifiedName
import org.jetbrains.jet.lang.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.jet.lang.psi.psiUtil.getOutermostNonInterleavingQualifiedElement
import org.jetbrains.jet.plugin.codeInsight.addElementToShorteningWaitSet

public class JetSimpleNameReference(
        jetSimpleNameExpression: JetSimpleNameExpression
) : JetSimpleReference<JetSimpleNameExpression>(jetSimpleNameExpression) {

    override fun getRangeInElement(): TextRange = TextRange(0, getElement().getTextLength())

    public override fun handleElementRename(newElementName: String?): PsiElement? {
        if (newElementName == null) return null;

        val project = expression.getProject()
        val element = when (expression.getReferencedNameElementType()) {
            JetTokens.FIELD_IDENTIFIER -> JetPsiFactory.createFieldIdentifier(project, newElementName)
            JetTokens.LABEL_IDENTIFIER -> JetPsiFactory.createClassLabel(project, newElementName)
            else -> JetPsiFactory.createNameIdentifier(project, newElementName)
        }

        return expression.getReferencedNameElement().replace(element)
    }

    // By default reference binding is delayed
    override fun bindToElement(element: PsiElement): PsiElement = bindToElement(element, false)

    public fun bindToElement(element: PsiElement, bindImmediately: Boolean): PsiElement {
        return element.getFqName()?.let { fqName -> bindToFqName(fqName, bindImmediately) } ?: expression
    }

    public fun bindToFqName(fqName: FqName, forceImmediateBinding: Boolean): PsiElement {
        val newExpression = expression.changeQualifiedName(fqName).getQualifiedElementSelector() as JetSimpleNameExpression

        val needToShorten =
                PsiTreeUtil.getParentOfType(expression, javaClass<JetImportDirective>(), javaClass<JetPackageDirective>()) == null
        if (needToShorten) {
            if (forceImmediateBinding) {
                ShortenReferences.process(newExpression.getOutermostNonInterleavingQualifiedElement())
            }
            else {
                newExpression.getProject().addElementToShorteningWaitSet(newExpression)
            }
        }

        return newExpression
    }

    override fun toString(): String {
        return javaClass<JetSimpleNameReference>().getSimpleName() + ": " + expression.getText()
    }
}
