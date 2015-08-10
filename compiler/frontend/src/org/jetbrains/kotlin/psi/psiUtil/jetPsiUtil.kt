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
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import java.util.ArrayList
import java.util.Collections
import kotlin.test.assertTrue

// NOTE: in this file we collect only Kotlin-specific methods working with PSI and not modifying it

// ----------- Calls and qualified expressions ---------------------------------------------------------------------------------------------

public fun JetCallElement.getCallNameExpression(): JetSimpleNameExpression? {
    val calleeExpression = getCalleeExpression() ?: return null

    return when (calleeExpression) {
        is JetSimpleNameExpression -> calleeExpression
        is JetConstructorCalleeExpression -> calleeExpression.getConstructorReferenceExpression()
        else -> null
    }
}

/**
 * Returns enclosing qualifying element for given [[JetSimpleNameExpression]]
 * ([[JetQualifiedExpression]] or [[JetUserType]] or original expression)
 */
public fun JetSimpleNameExpression.getQualifiedElement(): JetElement {
    val baseExpression = (parent as? JetCallExpression) ?: this
    val parent = baseExpression.parent
    return when (parent) {
        is JetQualifiedExpression -> if (parent.selectorExpression == baseExpression) parent else baseExpression
        is JetUserType -> if (parent.referenceExpression == baseExpression) parent else baseExpression
        else -> baseExpression
    }
}

public fun JetSimpleNameExpression.getTopmostParentQualifiedExpressionForSelector(): JetQualifiedExpression? {
    return sequence<JetExpression>(this) {
        val parentQualified = it.getParent() as? JetQualifiedExpression
        if (parentQualified?.getSelectorExpression() == it) parentQualified else null
    }.last() as? JetQualifiedExpression
}

/**
 * Returns rightmost selector of the qualified element (null if there is no such selector)
 */
public fun JetElement.getQualifiedElementSelector(): JetElement? {
    return when (this) {
        is JetSimpleNameExpression -> this
        is JetCallExpression -> getCalleeExpression()
        is JetQualifiedExpression -> {
            val selector = getSelectorExpression()
            if (selector is JetCallExpression) selector.getCalleeExpression() else selector
        }
        is JetUserType -> getReferenceExpression()
        else -> null
    }
}

public fun JetSimpleNameExpression.getReceiverExpression(): JetExpression? {
    val parent = getParent()
    when {
        parent is JetQualifiedExpression && !isImportDirectiveExpression() -> {
            val receiverExpression = parent.getReceiverExpression()
            // Name expression can't be receiver for itself
            if (receiverExpression != this) {
                return receiverExpression
            }
        }
        parent is JetCallExpression -> {
            //This is in case `a().b()`
            val callExpression = parent
            val grandParent = callExpression.getParent()
            if (grandParent is JetQualifiedExpression) {
                val parentsReceiver = grandParent.getReceiverExpression()
                if (parentsReceiver != callExpression) {
                    return parentsReceiver
                }
            }
        }
        parent is JetBinaryExpression && parent.getOperationReference() == this -> {
            return if (parent.getOperationToken() in OperatorConventions.IN_OPERATIONS) parent.getRight() else parent.getLeft()
        }
        parent is JetUnaryExpression && parent.getOperationReference() == this -> {
            return parent.getBaseExpression()!!
        }
        parent is JetUserType -> {
            val qualifier = parent.getQualifier()
            if (qualifier != null) {
                return qualifier.getReferenceExpression()!!
            }
        }
    }
    return null
}

public fun JetElement.getQualifiedExpressionForSelector(): JetQualifiedExpression? {
    val parent = getParent()
    return if (parent is JetQualifiedExpression && parent.getSelectorExpression() == this) parent else null
}

public fun JetExpression.getQualifiedExpressionForSelectorOrThis(): JetExpression {
    return getQualifiedExpressionForSelector() ?: this
}

public fun JetExpression.getQualifiedExpressionForReceiver(): JetQualifiedExpression? {
    val parent = getParent()
    return if (parent is JetQualifiedExpression && parent.getReceiverExpression() == this) parent else null
}

public fun JetExpression.getQualifiedExpressionForReceiverOrThis(): JetExpression {
    return getQualifiedExpressionForReceiver() ?: this
}

public fun JetExpression.isDotReceiver(): Boolean =
        (getParent() as? JetDotQualifiedExpression)?.getReceiverExpression() == this

public fun JetElement.getCalleeHighlightingRange(): TextRange {
    val annotationEntry: JetAnnotationEntry =
            PsiTreeUtil.getParentOfType<JetAnnotationEntry>(
                    this, javaClass<JetAnnotationEntry>(), /* strict = */false, javaClass<JetValueArgumentList>()
            ) ?: return getTextRange()

    val startOffset = annotationEntry.getAtSymbol()?.getTextRange()?.getStartOffset()
                      ?: annotationEntry.getCalleeExpression()!!.startOffset

    return TextRange(startOffset, annotationEntry.getCalleeExpression()!!.endOffset)
}

// ---------- Block expression -------------------------------------------------------------------------------------------------------------

public fun JetElement.blockExpressionsOrSingle(): Sequence<JetElement> =
        if (this is JetBlockExpression) getStatements().asSequence() else sequenceOf(this)

public fun JetExpression.lastBlockStatementOrThis(): JetExpression
        = (this as? JetBlockExpression)?.getStatements()?.lastOrNull() ?: this

public fun JetBlockExpression.contentRange(): PsiChildRange {
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

public fun JetClass.isInheritable(): Boolean {
    return isInterface() || hasModifier(JetTokens.OPEN_KEYWORD) || hasModifier(JetTokens.ABSTRACT_KEYWORD)
}

public fun JetDeclaration.isOverridable(): Boolean {
    val parent = getParent()
    if (!(parent is JetClassBody || parent is JetParameterList)) return false

    val klass = parent.getParent()
    if (!(klass is JetClass && klass.isInheritable())) return false

    if (hasModifier(JetTokens.FINAL_KEYWORD) || hasModifier(JetTokens.PRIVATE_KEYWORD)) return false

    return klass.isInterface() ||
           hasModifier(JetTokens.ABSTRACT_KEYWORD) || hasModifier(JetTokens.OPEN_KEYWORD) || hasModifier(JetTokens.OVERRIDE_KEYWORD)
}

public fun JetClass.isAbstract(): Boolean = isInterface() || hasModifier(JetTokens.ABSTRACT_KEYWORD)

/**
 * Returns the list of unqualified names that are indexed as the superclass names of this class. For the names that might be imported
 * via an aliased import, includes both the original and the aliased name (reference resolution during inheritor search will sort this out).
 *
 * @return the list of possible superclass names
 */
public fun StubBasedPsiElementBase<out KotlinClassOrObjectStub<out JetClassOrObject>>.getSuperNames(): List<String> {
    fun addSuperName(result: MutableList<String>, referencedName: String): Unit {
        result.add(referencedName)

        val file = getContainingFile()
        if (file is JetFile) {
            val directive = file.findImportByAlias(referencedName)
            if (directive != null) {
                var reference = directive.getImportedReference()
                while (reference is JetDotQualifiedExpression) {
                    reference = reference.getSelectorExpression()
                }
                if (reference is JetSimpleNameExpression) {
                    result.add(reference.getReferencedName())
                }
            }
        }
    }

    assertTrue(this is JetClassOrObject)

    val stub = getStub()
    if (stub != null) {
        return stub.getSuperNames()
    }

    val specifiers = (this as JetClassOrObject).getDelegationSpecifiers()
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
public fun JetExpression.getAnnotationEntries(): List<JetAnnotationEntry> {
    val parent = getParent()
    return when (parent) {
        is JetAnnotatedExpression -> parent.getAnnotationEntries()
        is JetLabeledExpression -> parent.getAnnotationEntries()
        else -> emptyList<JetAnnotationEntry>()
    }
}

public fun JetAnnotationsContainer.collectAnnotationEntriesFromStubOrPsi(): List<JetAnnotationEntry> {
    return when (this) {
        is StubBasedPsiElementBase<*> -> getStub()?.collectAnnotationEntriesFromStubElement() ?: collectAnnotationEntriesFromPsi()
        else -> collectAnnotationEntriesFromPsi()
    }
}

private fun StubElement<*>.collectAnnotationEntriesFromStubElement(): List<JetAnnotationEntry> {
    return getChildrenStubs().flatMap {
        child ->
        when (child.getStubType()) {
            JetNodeTypes.ANNOTATION_ENTRY -> listOf(child.getPsi() as JetAnnotationEntry)
            JetNodeTypes.ANNOTATION -> (child.getPsi() as JetAnnotation).getEntries()
            else -> emptyList<JetAnnotationEntry>()
        }
    }
}

private fun JetAnnotationsContainer.collectAnnotationEntriesFromPsi(): List<JetAnnotationEntry> {
    return getChildren().flatMap { child ->
        when (child) {
            is JetAnnotationEntry -> listOf(child)
            is JetAnnotation -> child.getEntries()
            else -> emptyList<JetAnnotationEntry>()
        }
    }
}

// -------- Recursive tree visiting --------------------------------------------------------------------------------------------------------

// Calls `block` on each descendant of T type
// Note, that calls happen in order of DFS-exit, so deeper nodes are applied earlier
public inline fun <reified T : JetElement> forEachDescendantOfTypeVisitor(noinline block: (T) -> Unit): JetVisitorVoid {
    return object : JetTreeVisitorVoid() {
        override fun visitJetElement(element: JetElement) {
            super.visitJetElement(element)
            if (element is T) {
                block(element)
            }
        }
    }
}

public inline fun <reified T : JetElement, R> flatMapDescendantsOfTypeVisitor(accumulator: MutableCollection<R>, noinline map: (T) -> Collection<R>): JetVisitorVoid {
    return forEachDescendantOfTypeVisitor<T> { accumulator.addAll(map(it)) }
}

// ----------- Other -----------------------------------------------------------------------------------------------------------------------

public fun JetClassOrObject.effectiveDeclarations(): List<JetDeclaration> {
    return when(this) {
        is JetClass -> getDeclarations() + getPrimaryConstructorParameters().filter { p -> p.hasValOrVar() }
        else -> getDeclarations()
    }
}

public fun JetDeclaration.isExtensionDeclaration(): Boolean {
    val callable: JetCallableDeclaration? = when (this) {
        is JetNamedFunction, is JetProperty -> this as JetCallableDeclaration
        is JetPropertyAccessor -> getNonStrictParentOfType<JetProperty>()
        else -> null
    }

    return callable?.getReceiverTypeReference() != null
}

public fun JetClassOrObject.isObjectLiteral(): Boolean = this is JetObjectDeclaration && isObjectLiteral()

//TODO: strange method, and not only Kotlin specific (also Java)
public fun PsiElement.parameterIndex(): Int {
    val parent = getParent()
    return when {
        this is JetParameter && parent is JetParameterList -> parent.getParameters().indexOf(this)
        this is PsiParameter && parent is PsiParameterList -> parent.getParameterIndex(this)
        else -> -1
    }
}

public fun JetModifierListOwner.isPrivate(): Boolean = hasModifier(JetTokens.PRIVATE_KEYWORD)

public fun JetSimpleNameExpression.isImportDirectiveExpression(): Boolean {
    val parent = getParent()
    if (parent == null) {
        return false
    }
    else {
        return parent is JetImportDirective || parent.getParent() is JetImportDirective
    }
}

public fun JetExpression.isFunctionLiteralOutsideParentheses(): Boolean {
    val parent = getParent()
    return when (parent) {
        is JetFunctionLiteralArgument -> true
        is JetLabeledExpression -> parent.isFunctionLiteralOutsideParentheses()
        else -> false
    }
}

public fun JetExpression.getAssignmentByLHS(): JetBinaryExpression? {
    val parent = getParent() as? JetBinaryExpression ?: return null
    return if (JetPsiUtil.isAssignment(parent) && parent.getLeft() == this) parent else null
}

public fun JetStringTemplateExpression.getContentRange(): TextRange {
    val start = getNode().getFirstChildNode().getTextLength()
    val lastChild = getNode().getLastChildNode()
    val length = getTextLength()
    return TextRange(start, if (lastChild.getElementType() == JetTokens.CLOSING_QUOTE) length - lastChild.getTextLength() else length)
}

public fun JetStringTemplateExpression.isSingleQuoted(): Boolean
        = getNode().getFirstChildNode().getTextLength() == 1

public fun JetNamedDeclaration.getValueParameters(): List<JetParameter> {
    return getValueParameterList()?.getParameters() ?: Collections.emptyList()
}

public fun JetNamedDeclaration.getValueParameterList(): JetParameterList? {
    return when (this) {
        is JetCallableDeclaration -> getValueParameterList()
        is JetClass -> getPrimaryConstructorParameterList()
        else -> null
    }
}

public fun JetFunctionLiteralArgument.getFunctionLiteralArgumentName(bindingContext: BindingContext): Name? {
    val callExpression = getParent() as JetCallExpression
    val resolvedCall = callExpression.getResolvedCall(bindingContext)
    return (resolvedCall?.getArgumentMapping(this) as? ArgumentMatch)?.valueParameter?.getName()
}

public fun JetExpression.asAssignment(): JetBinaryExpression? =
        if (JetPsiUtil.isAssignment(this)) this as JetBinaryExpression else null

