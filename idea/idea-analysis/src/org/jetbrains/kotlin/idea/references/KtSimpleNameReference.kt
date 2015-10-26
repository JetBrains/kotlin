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

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.intentions.OperatorToFunctionIntention
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.isOneSegmentFQN
import org.jetbrains.kotlin.plugin.references.SimpleNameReferenceExtension
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElement
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.dataClassUtils.isComponentLike
import org.jetbrains.kotlin.types.expressions.OperatorConventions

class KtSimpleNameReference(expression: KtSimpleNameExpression) : KtSimpleReference<KtSimpleNameExpression>(expression) {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        return SmartList<DeclarationDescriptor>().apply {
            // Replace Java property with its accessor(s)
            for (descriptor in super.getTargetDescriptors(context)) {
                val sizeBefore = size

                if (descriptor !is JavaPropertyDescriptor) {
                    add(descriptor)
                    continue
                }

                val readWriteAccess = expression.readWriteAccess(true)
                descriptor.getter?.let {
                    if (readWriteAccess.isRead) add(it)
                }
                descriptor.setter?.let {
                    if (readWriteAccess.isWrite) add(it)
                }

                if (size == sizeBefore) {
                    add(descriptor)
                }
            }
        }
    }

    override fun isReferenceTo(element: PsiElement?): Boolean {
        if (element != null) {
            if (!canBeReferenceTo(element)) return false

            val extensions = Extensions.getArea(element.getProject()).getExtensionPoint(SimpleNameReferenceExtension.EP_NAME).getExtensions()
            for (extension in extensions) {
                val value = extension.isReferenceTo(this, element)
                if (value != null) {
                    return value
                }
            }
        }

        return super.isReferenceTo(element)
    }

    override fun getRangeInElement(): TextRange {
        val element = getElement().getReferencedNameElement()
        val startOffset = getElement().startOffset
        return element.getTextRange().shiftRight(-startOffset)
    }

    override fun canRename(): Boolean {
        if (expression.getParentOfTypeAndBranch<KtWhenConditionInRange>(strict = true){ getOperationReference() } != null) return false

        val elementType = expression.getReferencedNameElementType()
        if (elementType == KtTokens.PLUSPLUS || elementType == KtTokens.MINUSMINUS) return false

        return true
    }

    override fun handleElementRename(newElementName: String?): PsiElement {
        if (!canRename()) throw IncorrectOperationException()
        if (newElementName == null) return expression;

        // Do not rename if the reference corresponds to synthesized component function
        val expressionText = expression.getText()
        if (expressionText != null && Name.isValidIdentifier(expressionText)) {
            if (isComponentLike(Name.identifier(expressionText)) && resolve() is KtParameter) {
                return expression
            }
        }

        val psiFactory = KtPsiFactory(expression)
        val element = Extensions.getArea(expression.getProject()).getExtensionPoint(SimpleNameReferenceExtension.EP_NAME).extensions
                        .asSequence()
                        .map { it.handleElementRename(this, psiFactory, newElementName) }
                        .firstOrNull { it != null } ?: psiFactory.createNameIdentifier(newElementName)

        val nameElement = expression.getReferencedNameElement()

        val elementType = nameElement.getNode().getElementType()
        if (elementType is KtToken && OperatorConventions.getNameForOperationSymbol(elementType) != null) {
            val opExpression = expression.getParent() as? KtOperationExpression
            if (opExpression != null) {
                val (newExpression, newNameElement) = OperatorToFunctionIntention.convert(opExpression)
                newNameElement.replace(element)
                return newExpression
            }
        }

        nameElement.replace(element)
        return expression
    }

    public enum class ShorteningMode {
        NO_SHORTENING,
        DELAYED_SHORTENING,
        FORCED_SHORTENING
    }

    // By default reference binding is delayed
    override fun bindToElement(element: PsiElement): PsiElement =
            bindToElement(element, ShorteningMode.DELAYED_SHORTENING)

    fun bindToElement(element: PsiElement, shorteningMode: ShorteningMode): PsiElement =
            element.getKotlinFqName()?.let { fqName -> bindToFqName(fqName, shorteningMode) } ?: expression

    fun bindToFqName(fqName: FqName, shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING): PsiElement {
        val expression = expression
        if (fqName.isRoot) return expression

        // not supported for infix calls and operators
        if (expression !is KtNameReferenceExpression) return expression
        if (expression.parent is KtThisExpression || expression.parent is KtSuperExpression) return expression // TODO: it's a bad design of PSI tree, we should change it

        val newExpression = expression.changeQualifiedName(fqName).getQualifiedElementSelector() as KtNameReferenceExpression
        val newQualifiedElement = newExpression.getQualifiedElement()

        if (shorteningMode == ShorteningMode.NO_SHORTENING) return newExpression

        val needToShorten =
                PsiTreeUtil.getParentOfType(expression, javaClass<KtImportDirective>(), javaClass<KtPackageDirective>()) == null
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

    /**
     * Replace [[KtNameReferenceExpression]] (and its enclosing qualifier) with qualified element given by FqName
     * Result is either the same as original element, or [[KtQualifiedExpression]], or [[KtUserType]]
     * Note that FqName may not be empty
     */
    private fun KtNameReferenceExpression.changeQualifiedName(fqName: FqName): KtElement {
        assert(!fqName.isRoot()) { "Can't set empty FqName for element $this" }

        val shortName = fqName.shortName().asString()
        val psiFactory = KtPsiFactory(this)
        val fqNameBase = (getParent() as? KtCallExpression)?.let { parent ->
            val callCopy = parent.copy() as KtCallExpression
            callCopy.getCalleeExpression()!!.replace(psiFactory.createSimpleName(shortName)).getParent()!!.getText()
        } ?: shortName

        val text = if (!fqName.isOneSegmentFQN()) "${fqName.parent().asString()}.$fqNameBase" else fqNameBase

        val elementToReplace = getQualifiedElement()
        return when (elementToReplace) {
            is KtUserType -> {
                val typeText = "$text${elementToReplace.getTypeArgumentList()?.getText() ?: ""}"
                elementToReplace.replace(psiFactory.createType(typeText).typeElement!!)
            }
            else -> elementToReplace.replace(psiFactory.createExpression(text))
        } as KtElement
    }

    override fun getCanonicalText(): String = expression.getText()
}
