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

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.refactoring.fqName.changeQualifiedName
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.idea.intentions.OperatorToFunctionIntention
import org.jetbrains.kotlin.resolve.BindingContext
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.dataClassUtils.isComponentLike
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.psiUtil.*
import com.intellij.psi.impl.light.LightElement

public class JetSimpleNameReference(
        jetSimpleNameExpression: JetSimpleNameExpression
) : JetSimpleReference<JetSimpleNameExpression>(jetSimpleNameExpression) {

    override fun isReferenceTo(element: PsiElement?): Boolean {
       val resolvedElement = resolve()
       if (resolvedElement == null || element == null) {
           return false
       }
       if (isAndroidSyntheticElement(resolvedElement)) {
           if (element is ValueResourceElementWrapper) {
               val resource = element.getValue()!!
               return (resolvedElement as JetProperty).getName() == resource.substring(resource.indexOf('/')+1)
           }

       }
       else if (resolvedElement is LightElement && element is JetProperty) {
           return resolvedElement.getName() == element.getName()
       }
       return super.isReferenceTo(element)
   }

    override fun getRangeInElement(): TextRange = TextRange(0, getElement().getTextLength())

    override fun canRename(): Boolean {
        if (expression.getParentOfTypeAndBranch<JetWhenConditionInRange>(strict = true){ getOperationReference() } != null) return false

        val elementType = expression.getReferencedNameElementType()
        if (elementType == JetTokens.PLUSPLUS || elementType == JetTokens.MINUSMINUS) return false

        return true
    }

    public override fun handleElementRename(newElementName: String?): PsiElement? {
        if (!canRename()) throw IncorrectOperationException()
        if (newElementName == null) return expression;

        // Do not rename if the reference corresponds to synthesized component function
        val expressionText = expression.getText()
        if (expressionText != null && Name.isValidIdentifier(expressionText)) {
            if (isComponentLike(Name.identifier(expressionText)) && resolve() is JetParameter) {
                return expression
            }
        }

        val psiFactory = JetPsiFactory(expression)
        val element = when (expression.getReferencedNameElementType()) {
            JetTokens.FIELD_IDENTIFIER -> psiFactory.createFieldIdentifier(newElementName)
            JetTokens.LABEL_IDENTIFIER -> psiFactory.createClassLabel(newElementName)
            else -> {
                if (newElementName.startsWith("@+id/")) {
                    psiFactory.createNameIdentifier(newElementName.substring(newElementName.indexOf('/')+1))
                }
                else {
                    psiFactory.createNameIdentifier(newElementName)
                }
            }
        }

        var nameElement = expression.getReferencedNameElement()

        val elementType = nameElement.getNode()?.getElementType()
        val opExpression =
                PsiTreeUtil.getParentOfType<JetExpression>(expression, javaClass<JetUnaryExpression>(), javaClass<JetBinaryExpression>())
        if (elementType is JetToken && OperatorConventions.getNameForOperationSymbol(elementType) != null && opExpression != null) {
            val oldDescriptor = expression.analyze()[BindingContext.REFERENCE_TARGET, expression]
            val newExpression = OperatorToFunctionIntention.convert(opExpression)
            newExpression.accept(
                    object: JetTreeVisitorVoid() {
                        override fun visitCallExpression(expression: JetCallExpression) {
                            val callee = expression.getCalleeExpression() as? JetSimpleNameExpression
                            if (callee != null && callee.analyze()[BindingContext.REFERENCE_TARGET, callee] == oldDescriptor) {
                                nameElement = callee.getReferencedNameElement()
                            }
                            else {
                                super.visitCallExpression(expression)
                            }
                        }
                    }
            )
        }

        return nameElement.replace(element)
    }

    public enum class ShorteningMode {
        NO_SHORTENING
        DELAYED_SHORTENING
        FORCED_SHORTENING
    }

    // By default reference binding is delayed
    override fun bindToElement(element: PsiElement): PsiElement =
            bindToElement(element, ShorteningMode.DELAYED_SHORTENING)

    fun bindToElement(element: PsiElement, shorteningMode: ShorteningMode): PsiElement =
            element.getKotlinFqName()?.let { fqName -> bindToFqName(fqName, shorteningMode) } ?: expression

    public fun bindToFqName(fqName: FqName, shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING): PsiElement {
        if (fqName.isRoot()) return expression

        val newExpression = expression.changeQualifiedName(fqName).getQualifiedElementSelector() as JetSimpleNameExpression
        val newQualifiedElement = newExpression.getQualifiedElement()

        if (shorteningMode == ShorteningMode.NO_SHORTENING) return newExpression

        val needToShorten =
                PsiTreeUtil.getParentOfType(expression, javaClass<JetImportDirective>(), javaClass<JetPackageDirective>()) == null
        if (needToShorten) {
            if (shorteningMode == ShorteningMode.FORCED_SHORTENING) {
                ShortenReferences.DEFAULT.process(newQualifiedElement)
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
