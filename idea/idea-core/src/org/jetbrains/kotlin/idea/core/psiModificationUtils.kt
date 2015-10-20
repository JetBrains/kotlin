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

package org.jetbrains.kotlin.idea.core

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getFunctionLiteralArgumentName
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentsInParentheses

@Suppress("UNCHECKED_CAST")
public inline fun <reified T: PsiElement> PsiElement.replaced(newElement: T): T {
    val result = replace(newElement)
    return if (result is T)
        result
    else
        (result as KtParenthesizedExpression).getExpression() as T
}

@Suppress("UNCHECKED_CAST")
public fun <T: PsiElement> T.copied(): T = copy() as T

public fun KtFunctionLiteralArgument.moveInsideParentheses(bindingContext: BindingContext): KtCallExpression {
    return moveInsideParenthesesAndReplaceWith(this.getArgumentExpression(), bindingContext)
}

public fun KtFunctionLiteralArgument.moveInsideParenthesesAndReplaceWith(
        replacement: KtExpression,
        bindingContext: BindingContext
): KtCallExpression = moveInsideParenthesesAndReplaceWith(replacement, getFunctionLiteralArgumentName(bindingContext))

public fun KtFunctionLiteralArgument.moveInsideParenthesesAndReplaceWith(
        replacement: KtExpression,
        functionLiteralArgumentName: Name?
): KtCallExpression {
    val oldCallExpression = getParent() as KtCallExpression
    val newCallExpression = oldCallExpression.copy() as KtCallExpression

    val psiFactory = KtPsiFactory(getProject())
    val argument = if (newCallExpression.getValueArgumentsInParentheses().any { it.isNamed() }) {
        psiFactory.createArgument(replacement, functionLiteralArgumentName)
    }
    else {
        psiFactory.createArgument(replacement)
    }

    val functionLiteralArgument = newCallExpression.getFunctionLiteralArguments().firstOrNull()!!
    val valueArgumentList = newCallExpression.getValueArgumentList() ?: psiFactory.createCallArguments("()")

    valueArgumentList.addArgument(argument)

    (functionLiteralArgument.getPrevSibling() as? PsiWhiteSpace)?.delete()
    if (newCallExpression.getValueArgumentList() != null) {
        functionLiteralArgument.delete()
    }
    else {
        functionLiteralArgument.replace(valueArgumentList)
    }
    return oldCallExpression.replace(newCallExpression) as KtCallExpression
}

public fun KtCallExpression.moveFunctionLiteralOutsideParentheses() {
    assert(getFunctionLiteralArguments().isEmpty())
    val argumentList = getValueArgumentList()!!
    val argument = argumentList.getArguments().last()
    val expression = argument.getArgumentExpression()!!
    assert(expression.unpackFunctionLiteral() != null)

    val dummyCall = KtPsiFactory(this).createExpressionByPattern("foo()$0:'{}'", expression) as KtCallExpression
    val functionLiteralArgument = dummyCall.getFunctionLiteralArguments().single()
    this.add(functionLiteralArgument)
    if (argumentList.getArguments().size() > 1) {
        argumentList.removeArgument(argument)
    }
    else {
        argumentList.delete()
    }
}

public fun KtBlockExpression.appendElement(element: KtElement, addNewLine: Boolean = false): KtElement {
    val rBrace = getRBrace()
    val newLine = KtPsiFactory(this).createNewLine()
    val anchor = if (rBrace == null) {
        val lastChild = getLastChild()
        if (lastChild !is PsiWhiteSpace) addAfter(newLine, lastChild)!! else lastChild
    }
    else {
        rBrace.getPrevSibling()!!
    }
    val addedElement = addAfter(element, anchor)!! as KtElement
    if (addNewLine) {
        addAfter(newLine, addedElement)
    }
    return addedElement
}

//TODO: git rid of this method
public fun PsiElement.deleteElementAndCleanParent() {
    val parent = getParent()

    deleteElementWithDelimiters(this)
    deleteChildlessElement(parent, this.javaClass)
}

// Delete element if it doesn't contain children of a given type
private fun <T : PsiElement> deleteChildlessElement(element: PsiElement, childClass: Class<T>) {
    if (PsiTreeUtil.getChildrenOfType<T>(element, childClass) == null) {
        element.delete()
    }
}

// Delete given element and all the elements separating it from the neighboring elements of the same class
private fun deleteElementWithDelimiters(element: PsiElement) {
    val paramBefore = PsiTreeUtil.getPrevSiblingOfType<PsiElement>(element, element.javaClass)

    val from: PsiElement
    val to: PsiElement
    if (paramBefore != null) {
        from = paramBefore.getNextSibling()
        to = element
    }
    else {
        val paramAfter = PsiTreeUtil.getNextSiblingOfType<PsiElement>(element, element.javaClass)

        from = element
        to = if (paramAfter != null) paramAfter.getPrevSibling() else element
    }

    val parent = element.getParent()

    parent.deleteChildRange(from, to)
}

public fun PsiElement.deleteSingle() {
    CodeEditUtil.removeChild(getParent()?.getNode() ?: return, getNode() ?: return)
}

public fun KtClass.getOrCreateCompanionObject() : KtObjectDeclaration {
    getCompanionObjects().firstOrNull()?.let { return it }
    return addDeclaration(KtPsiFactory(this).createCompanionObject()) as KtObjectDeclaration
}

//TODO: code style option whether to insert redundant 'public' keyword or not
public fun KtDeclaration.setVisibility(visibilityModifier: KtModifierKeywordToken) {
    val defaultVisibilityKeyword = if (hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
        (resolveToDescriptor() as? CallableMemberDescriptor)
                ?.overriddenDescriptors
                ?.let { OverridingUtil.findMaxVisibility(it) }
                ?.toKeywordToken()
    }
    else {
        KtTokens.DEFAULT_VISIBILITY_KEYWORD
    }

    if (visibilityModifier == defaultVisibilityKeyword) {
        this.visibilityModifierType()?.let { removeModifier(it) }
        return
    }

    addModifier(visibilityModifier)
}
