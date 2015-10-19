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

package org.jetbrains.kotlin.psi.psiUtil

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import java.util.*
import kotlin.test.assertTrue

// NOTE: in this file we collect only Kotlin-specific methods working with PSI and not modifying it

// ----------- Calls and qualified expressions ---------------------------------------------------------------------------------------------

public fun KtCallElement.getCallNameExpression(): KtSimpleNameExpression? {
    val calleeExpression = getCalleeExpression() ?: return null

    return when (calleeExpression) {
        is KtSimpleNameExpression -> calleeExpression
        is KtConstructorCalleeExpression -> calleeExpression.getConstructorReferenceExpression()
        else -> null
    }
}

/**
 * Returns enclosing qualifying element for given [[KtSimpleNameExpression]]
 * ([[KtQualifiedExpression]] or [[KtUserType]] or original expression)
 */
public fun KtSimpleNameExpression.getQualifiedElement(): KtElement {
    val baseExpression = (parent as? KtCallExpression) ?: this
    val parent = baseExpression.parent
    return when (parent) {
        is KtQualifiedExpression -> if (parent.selectorExpression == baseExpression) parent else baseExpression
        is KtUserType -> if (parent.referenceExpression == baseExpression) parent else baseExpression
        else -> baseExpression
    }
}

public fun KtSimpleNameExpression.getTopmostParentQualifiedExpressionForSelector(): KtQualifiedExpression? {
    return sequence<KtExpression>(this) {
        val parentQualified = it.getParent() as? KtQualifiedExpression
        if (parentQualified?.getSelectorExpression() == it) parentQualified else null
    }.last() as? KtQualifiedExpression
}

/**
 * Returns rightmost selector of the qualified element (null if there is no such selector)
 */
public fun KtElement.getQualifiedElementSelector(): KtElement? {
    return when (this) {
        is KtSimpleNameExpression -> this
        is KtCallExpression -> getCalleeExpression()
        is KtQualifiedExpression -> {
            val selector = getSelectorExpression()
            if (selector is KtCallExpression) selector.getCalleeExpression() else selector
        }
        is KtUserType -> getReferenceExpression()
        else -> null
    }
}

public fun KtSimpleNameExpression.getReceiverExpression(): KtExpression? {
    val parent = getParent()
    when {
        parent is KtQualifiedExpression -> {
            val receiverExpression = parent.getReceiverExpression()
            // Name expression can't be receiver for itself
            if (receiverExpression != this) {
                return receiverExpression
            }
        }
        parent is KtCallExpression -> {
            //This is in case `a().b()`
            val callExpression = parent
            val grandParent = callExpression.getParent()
            if (grandParent is KtQualifiedExpression) {
                val parentsReceiver = grandParent.getReceiverExpression()
                if (parentsReceiver != callExpression) {
                    return parentsReceiver
                }
            }
        }
        parent is KtBinaryExpression && parent.getOperationReference() == this -> {
            return if (parent.getOperationToken() in OperatorConventions.IN_OPERATIONS) parent.getRight() else parent.getLeft()
        }
        parent is KtUnaryExpression && parent.getOperationReference() == this -> {
            return parent.getBaseExpression()!!
        }
        parent is KtUserType -> {
            val qualifier = parent.getQualifier()
            if (qualifier != null) {
                return qualifier.getReferenceExpression()!!
            }
        }
    }
    return null
}

public fun KtElement.getQualifiedExpressionForSelector(): KtQualifiedExpression? {
    val parent = getParent()
    return if (parent is KtQualifiedExpression && parent.getSelectorExpression() == this) parent else null
}

public fun KtExpression.getQualifiedExpressionForSelectorOrThis(): KtExpression {
    return getQualifiedExpressionForSelector() ?: this
}

public fun KtExpression.getQualifiedExpressionForReceiver(): KtQualifiedExpression? {
    val parent = getParent()
    return if (parent is KtQualifiedExpression && parent.getReceiverExpression() == this) parent else null
}

public fun KtExpression.getQualifiedExpressionForReceiverOrThis(): KtExpression {
    return getQualifiedExpressionForReceiver() ?: this
}

public fun KtExpression.isDotReceiver(): Boolean =
        (getParent() as? KtDotQualifiedExpression)?.getReceiverExpression() == this

public fun KtElement.getCalleeHighlightingRange(): TextRange {
    val annotationEntry: KtAnnotationEntry =
            PsiTreeUtil.getParentOfType<KtAnnotationEntry>(
                    this, javaClass<KtAnnotationEntry>(), /* strict = */false, javaClass<KtValueArgumentList>()
            ) ?: return getTextRange()

    val startOffset = annotationEntry.getAtSymbol()?.getTextRange()?.getStartOffset()
                      ?: annotationEntry.getCalleeExpression()!!.startOffset

    return TextRange(startOffset, annotationEntry.getCalleeExpression()!!.endOffset)
}

// ---------- Block expression -------------------------------------------------------------------------------------------------------------

public fun KtElement.blockExpressionsOrSingle(): Sequence<KtElement> =
        if (this is KtBlockExpression) getStatements().asSequence() else sequenceOf(this)

public fun KtExpression.lastBlockStatementOrThis(): KtExpression
        = (this as? KtBlockExpression)?.getStatements()?.lastOrNull() ?: this

public fun KtBlockExpression.contentRange(): PsiChildRange {
    val first = (getLBrace()?.getNextSibling() ?: getFirstChild())
            ?.siblings(withItself = false)
            ?.firstOrNull { it !is PsiWhiteSpace }
    val rBrace = getRBrace()
    if (first == rBrace) return PsiChildRange.EMPTY
    val last = rBrace!!
            .siblings(forward = false, withItself = false)
            .first { it !is PsiWhiteSpace }
    return PsiChildRange(first, last)
}

// ----------- Inheritance -----------------------------------------------------------------------------------------------------------------

public fun KtClass.isInheritable(): Boolean {
    return isInterface() || hasModifier(KtTokens.OPEN_KEYWORD) || hasModifier(KtTokens.ABSTRACT_KEYWORD)
}

public fun KtDeclaration.isOverridable(): Boolean {
    val parent = getParent()
    if (!(parent is KtClassBody || parent is KtParameterList)) return false

    val klass = parent.getParent() as? KtClass ?: return false
    if (!klass.isInheritable() && !klass.isEnum()) return false

    if (hasModifier(KtTokens.FINAL_KEYWORD) || hasModifier(KtTokens.PRIVATE_KEYWORD)) return false

    return klass.isInterface() ||
           hasModifier(KtTokens.ABSTRACT_KEYWORD) || hasModifier(KtTokens.OPEN_KEYWORD) || hasModifier(KtTokens.OVERRIDE_KEYWORD)
}

public fun KtClass.isAbstract(): Boolean = isInterface() || hasModifier(KtTokens.ABSTRACT_KEYWORD)

/**
 * Returns the list of unqualified names that are indexed as the superclass names of this class. For the names that might be imported
 * via an aliased import, includes both the original and the aliased name (reference resolution during inheritor search will sort this out).
 *
 * @return the list of possible superclass names
 */
public fun StubBasedPsiElementBase<out KotlinClassOrObjectStub<out KtClassOrObject>>.getSuperNames(): List<String> {
    fun addSuperName(result: MutableList<String>, referencedName: String): Unit {
        result.add(referencedName)

        val file = getContainingFile()
        if (file is KtFile) {
            val directive = file.findImportByAlias(referencedName)
            if (directive != null) {
                var reference = directive.getImportedReference()
                while (reference is KtDotQualifiedExpression) {
                    reference = reference.getSelectorExpression()
                }
                if (reference is KtSimpleNameExpression) {
                    result.add(reference.getReferencedName())
                }
            }
        }
    }

    assertTrue(this is KtClassOrObject)

    val stub = getStub()
    if (stub != null) {
        return stub.getSuperNames()
    }

    val specifiers = (this as KtClassOrObject).getDelegationSpecifiers()
    if (specifiers.isEmpty()) return Collections.emptyList<String>()

    val result = ArrayList<String>()
    for (specifier in specifiers) {
        val superType = specifier.getTypeAsUserType()
        if (superType != null) {
            val referencedName = superType.getReferencedName()
            if (referencedName != null) {
                addSuperName(result, referencedName)
            }
        }
    }

    return result
}

// ------------ Annotations ----------------------------------------------------------------------------------------------------------------

// Annotations on labeled expression lies on it's base expression
public fun KtExpression.getAnnotationEntries(): List<KtAnnotationEntry> {
    val parent = getParent()
    return when (parent) {
        is KtAnnotatedExpression -> parent.getAnnotationEntries()
        is KtLabeledExpression -> parent.getAnnotationEntries()
        else -> emptyList<KtAnnotationEntry>()
    }
}

public fun KtAnnotationsContainer.collectAnnotationEntriesFromStubOrPsi(): List<KtAnnotationEntry> {
    return when (this) {
        is StubBasedPsiElementBase<*> -> getStub()?.collectAnnotationEntriesFromStubElement() ?: collectAnnotationEntriesFromPsi()
        else -> collectAnnotationEntriesFromPsi()
    }
}

private fun StubElement<*>.collectAnnotationEntriesFromStubElement(): List<KtAnnotationEntry> {
    return getChildrenStubs().flatMap {
        child ->
        when (child.getStubType()) {
            KtNodeTypes.ANNOTATION_ENTRY -> listOf(child.getPsi() as KtAnnotationEntry)
            KtNodeTypes.ANNOTATION -> (child.getPsi() as KtAnnotation).getEntries()
            else -> emptyList<KtAnnotationEntry>()
        }
    }
}

private fun KtAnnotationsContainer.collectAnnotationEntriesFromPsi(): List<KtAnnotationEntry> {
    return getChildren().flatMap { child ->
        when (child) {
            is KtAnnotationEntry -> listOf(child)
            is KtAnnotation -> child.getEntries()
            else -> emptyList<KtAnnotationEntry>()
        }
    }
}

// -------- Recursive tree visiting --------------------------------------------------------------------------------------------------------

// Calls `block` on each descendant of T type
// Note, that calls happen in order of DFS-exit, so deeper nodes are applied earlier
public inline fun <reified T : KtElement> forEachDescendantOfTypeVisitor(noinline block: (T) -> Unit): KtVisitorVoid {
    return object : KtTreeVisitorVoid() {
        override fun visitJetElement(element: KtElement) {
            super.visitJetElement(element)
            if (element is T) {
                block(element)
            }
        }
    }
}

public inline fun <reified T : KtElement, R> flatMapDescendantsOfTypeVisitor(accumulator: MutableCollection<R>, noinline map: (T) -> Collection<R>): KtVisitorVoid {
    return forEachDescendantOfTypeVisitor<T> { accumulator.addAll(map(it)) }
}

// ----------- Other -----------------------------------------------------------------------------------------------------------------------

public fun KtClassOrObject.effectiveDeclarations(): List<KtDeclaration> {
    return when(this) {
        is KtClass -> getDeclarations() + getPrimaryConstructorParameters().filter { p -> p.hasValOrVar() }
        else -> getDeclarations()
    }
}

public fun KtDeclaration.isExtensionDeclaration(): Boolean {
    val callable: KtCallableDeclaration? = when (this) {
        is KtNamedFunction, is KtProperty -> this as KtCallableDeclaration
        is KtPropertyAccessor -> getNonStrictParentOfType<KtProperty>()
        else -> null
    }

    return callable?.getReceiverTypeReference() != null
}

public fun KtClassOrObject.isObjectLiteral(): Boolean = this is KtObjectDeclaration && isObjectLiteral()

//TODO: strange method, and not only Kotlin specific (also Java)
public fun PsiElement.parameterIndex(): Int {
    val parent = getParent()
    return when {
        this is KtParameter && parent is KtParameterList -> parent.getParameters().indexOf(this)
        this is PsiParameter && parent is PsiParameterList -> parent.getParameterIndex(this)
        else -> -1
    }
}

public fun KtModifierListOwner.isPrivate(): Boolean = hasModifier(KtTokens.PRIVATE_KEYWORD)

public fun KtSimpleNameExpression.isImportDirectiveExpression(): Boolean {
    val parent = getParent()
    return parent is KtImportDirective || parent?.getParent() is KtImportDirective
}

public fun KtSimpleNameExpression.isPackageDirectiveExpression(): Boolean {
    val parent = getParent()
    return parent is KtPackageDirective || parent?.getParent() is KtPackageDirective
}

public fun KtExpression.isFunctionLiteralOutsideParentheses(): Boolean {
    val parent = getParent()
    return when (parent) {
        is KtFunctionLiteralArgument -> true
        is KtLabeledExpression -> parent.isFunctionLiteralOutsideParentheses()
        else -> false
    }
}

public fun KtExpression.getAssignmentByLHS(): KtBinaryExpression? {
    val parent = getParent() as? KtBinaryExpression ?: return null
    return if (KtPsiUtil.isAssignment(parent) && parent.getLeft() == this) parent else null
}

public fun KtStringTemplateExpression.getContentRange(): TextRange {
    val start = getNode().getFirstChildNode().getTextLength()
    val lastChild = getNode().getLastChildNode()
    val length = getTextLength()
    return TextRange(start, if (lastChild.getElementType() == KtTokens.CLOSING_QUOTE) length - lastChild.getTextLength() else length)
}

public fun KtStringTemplateExpression.isSingleQuoted(): Boolean
        = getNode().getFirstChildNode().getTextLength() == 1

public fun KtNamedDeclaration.getValueParameters(): List<KtParameter> {
    return getValueParameterList()?.getParameters() ?: Collections.emptyList()
}

public fun KtNamedDeclaration.getValueParameterList(): KtParameterList? {
    return when (this) {
        is KtCallableDeclaration -> getValueParameterList()
        is KtClass -> getPrimaryConstructorParameterList()
        else -> null
    }
}

public fun KtFunctionLiteralArgument.getFunctionLiteralArgumentName(bindingContext: BindingContext): Name? {
    val callExpression = getParent() as KtCallExpression
    val resolvedCall = callExpression.getResolvedCall(bindingContext)
    return (resolvedCall?.getArgumentMapping(this) as? ArgumentMatch)?.valueParameter?.getName()
}

public fun KtExpression.asAssignment(): KtBinaryExpression? =
        if (KtPsiUtil.isAssignment(this)) this as KtBinaryExpression else null

public fun KtDeclaration.visibilityModifier(): PsiElement? {
    val modifierList = modifierList ?: return null
    return KtTokens.VISIBILITY_MODIFIERS.types
                   .asSequence()
                   .map { modifierList.getModifier(it as KtModifierKeywordToken) }
                   .firstOrNull { it != null }
}

public fun KtDeclaration.visibilityModifierType(): KtModifierKeywordToken?
        = visibilityModifier()?.node?.elementType as KtModifierKeywordToken?

public fun KtStringTemplateExpression.isPlain() = entries.all { it is KtLiteralStringTemplateEntry }