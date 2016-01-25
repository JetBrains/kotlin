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
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
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
import kotlin.text.Regex

// NOTE: in this file we collect only Kotlin-specific methods working with PSI and not modifying it

// ----------- Calls and qualified expressions ---------------------------------------------------------------------------------------------

fun KtCallElement.getCallNameExpression(): KtSimpleNameExpression? {
    val calleeExpression = calleeExpression ?: return null

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
fun KtSimpleNameExpression.getQualifiedElement(): KtElement {
    val baseExpression = (parent as? KtCallExpression) ?: this
    val parent = baseExpression.parent
    return when (parent) {
        is KtQualifiedExpression -> if (parent.selectorExpression == baseExpression) parent else baseExpression
        is KtUserType -> if (parent.referenceExpression == baseExpression) parent else baseExpression
        else -> baseExpression
    }
}

fun KtSimpleNameExpression.getTopmostParentQualifiedExpressionForSelector(): KtQualifiedExpression? {
    return sequence<KtExpression>(this) {
        val parentQualified = it.parent as? KtQualifiedExpression
        if (parentQualified?.selectorExpression == it) parentQualified else null
    }.last() as? KtQualifiedExpression
}

/**
 * Returns rightmost selector of the qualified element (null if there is no such selector)
 */
fun KtElement.getQualifiedElementSelector(): KtElement? {
    return when (this) {
        is KtSimpleNameExpression -> this
        is KtCallExpression -> calleeExpression
        is KtQualifiedExpression -> {
            val selector = selectorExpression
            if (selector is KtCallExpression) selector.calleeExpression else selector
        }
        is KtUserType -> getReferenceExpression()
        else -> null
    }
}

fun KtSimpleNameExpression.getReceiverExpression(): KtExpression? {
    val parent = parent
    when {
        parent is KtQualifiedExpression -> {
            val receiverExpression = parent.receiverExpression
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
                val parentsReceiver = grandParent.receiverExpression
                if (parentsReceiver != callExpression) {
                    return parentsReceiver
                }
            }
        }
        parent is KtBinaryExpression && parent.operationReference == this -> {
            return if (parent.getOperationToken() in OperatorConventions.IN_OPERATIONS) parent.right else parent.left
        }
        parent is KtUnaryExpression && parent.operationReference == this -> {
            return parent.baseExpression
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

fun KtElement.getQualifiedExpressionForSelector(): KtQualifiedExpression? {
    val parent = parent
    return if (parent is KtQualifiedExpression && parent.selectorExpression == this) parent else null
}

fun KtExpression.getQualifiedExpressionForSelectorOrThis(): KtExpression {
    return getQualifiedExpressionForSelector() ?: this
}

fun KtExpression.getQualifiedExpressionForReceiver(): KtQualifiedExpression? {
    val parent = parent
    return if (parent is KtQualifiedExpression && parent.receiverExpression == this) parent else null
}

fun KtExpression.getQualifiedExpressionForReceiverOrThis(): KtExpression {
    return getQualifiedExpressionForReceiver() ?: this
}

fun KtExpression.isDotReceiver(): Boolean =
        (parent as? KtDotQualifiedExpression)?.getReceiverExpression() == this

fun KtElement.getCalleeHighlightingRange(): TextRange {
    val annotationEntry: KtAnnotationEntry =
            PsiTreeUtil.getParentOfType<KtAnnotationEntry>(
                    this, KtAnnotationEntry::class.java, /* strict = */false, KtValueArgumentList::class.java
            ) ?: return textRange

    val startOffset = annotationEntry.getAtSymbol()?.getTextRange()?.getStartOffset()
                      ?: annotationEntry.getCalleeExpression()!!.startOffset

    return TextRange(startOffset, annotationEntry.getCalleeExpression()!!.endOffset)
}

// ---------- Block expression -------------------------------------------------------------------------------------------------------------

fun KtElement.blockExpressionsOrSingle(): Sequence<KtElement> =
        if (this is KtBlockExpression) statements.asSequence() else sequenceOf(this)

fun KtExpression.lastBlockStatementOrThis(): KtExpression
        = (this as? KtBlockExpression)?.statements?.lastOrNull() ?: this

fun KtBlockExpression.contentRange(): PsiChildRange {
    val first = (lBrace?.nextSibling ?: firstChild)
            ?.siblings(withItself = false)
            ?.firstOrNull { it !is PsiWhiteSpace }
    val rBrace = rBrace
    if (first == rBrace) return PsiChildRange.EMPTY
    val last = rBrace!!
            .siblings(forward = false, withItself = false)
            .first { it !is PsiWhiteSpace }
    return PsiChildRange(first, last)
}

// ----------- Inheritance -----------------------------------------------------------------------------------------------------------------

fun KtClass.isInheritable(): Boolean {
    return isInterface() || hasModifier(KtTokens.OPEN_KEYWORD) || hasModifier(KtTokens.ABSTRACT_KEYWORD)
}

fun KtDeclaration.isOverridable(): Boolean {
    val parent = parent
    if (!(parent is KtClassBody || parent is KtParameterList)) return false

    val klass = parent.parent as? KtClass ?: return false
    if (!klass.isInheritable() && !klass.isEnum()) return false

    if (hasModifier(KtTokens.FINAL_KEYWORD) || hasModifier(KtTokens.PRIVATE_KEYWORD)) return false

    return klass.isInterface() ||
           hasModifier(KtTokens.ABSTRACT_KEYWORD) || hasModifier(KtTokens.OPEN_KEYWORD) || hasModifier(KtTokens.OVERRIDE_KEYWORD)
}

fun KtClass.isAbstract(): Boolean = isInterface() || hasModifier(KtTokens.ABSTRACT_KEYWORD)

/**
 * Returns the list of unqualified names that are indexed as the superclass names of this class. For the names that might be imported
 * via an aliased import, includes both the original and the aliased name (reference resolution during inheritor search will sort this out).
 *
 * @return the list of possible superclass names
 */
fun StubBasedPsiElementBase<out KotlinClassOrObjectStub<out KtClassOrObject>>.getSuperNames(): List<String> {
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

    require(this is KtClassOrObject) { "it should be ${KtClassOrObject::class} but it is a ${this.javaClass.name}" }

    val stub = getStub()
    if (stub != null) {
        return stub.getSuperNames()
    }

    val specifiers = (this as KtClassOrObject).getSuperTypeListEntries()
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
fun KtExpression.getAnnotationEntries(): List<KtAnnotationEntry> {
    val parent = parent
    return when (parent) {
        is KtAnnotatedExpression -> parent.annotationEntries
        is KtLabeledExpression -> parent.getAnnotationEntries()
        else -> emptyList<KtAnnotationEntry>()
    }
}

fun KtAnnotationsContainer.collectAnnotationEntriesFromStubOrPsi(): List<KtAnnotationEntry> {
    return when (this) {
        is StubBasedPsiElementBase<*> -> getStub()?.collectAnnotationEntriesFromStubElement() ?: collectAnnotationEntriesFromPsi()
        else -> collectAnnotationEntriesFromPsi()
    }
}

private fun StubElement<*>.collectAnnotationEntriesFromStubElement(): List<KtAnnotationEntry> {
    return childrenStubs.flatMap {
        child ->
        when (child.stubType) {
            KtNodeTypes.ANNOTATION_ENTRY -> listOf(child.psi as KtAnnotationEntry)
            KtNodeTypes.ANNOTATION -> (child.psi as KtAnnotation).getEntries()
            else -> emptyList<KtAnnotationEntry>()
        }
    }
}

private fun KtAnnotationsContainer.collectAnnotationEntriesFromPsi(): List<KtAnnotationEntry> {
    return children.flatMap { child ->
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
inline fun <reified T : KtElement> forEachDescendantOfTypeVisitor(noinline block: (T) -> Unit): KtVisitorVoid {
    return object : KtTreeVisitorVoid() {
        override fun visitKtElement(element: KtElement) {
            super.visitKtElement(element)
            if (element is T) {
                block(element)
            }
        }
    }
}

inline fun <reified T : KtElement, R> flatMapDescendantsOfTypeVisitor(accumulator: MutableCollection<R>, noinline map: (T) -> Collection<R>): KtVisitorVoid {
    return forEachDescendantOfTypeVisitor<T> { accumulator.addAll(map(it)) }
}

// ----------- Other -----------------------------------------------------------------------------------------------------------------------

fun KtClassOrObject.effectiveDeclarations(): List<KtDeclaration> {
    return when(this) {
        is KtClass -> getDeclarations() + getPrimaryConstructorParameters().filter { p -> p.hasValOrVar() }
        else -> getDeclarations()
    }
}

fun PsiElement.isExtensionDeclaration(): Boolean {
    val callable: KtCallableDeclaration? = when (this) {
        is KtNamedFunction, is KtProperty -> this as KtCallableDeclaration
        is KtPropertyAccessor -> getNonStrictParentOfType<KtProperty>()
        else -> null
    }

    return callable?.receiverTypeReference != null
}

fun KtClassOrObject.isObjectLiteral(): Boolean = this is KtObjectDeclaration && isObjectLiteral()

//TODO: strange method, and not only Kotlin specific (also Java)
fun PsiElement.parameterIndex(): Int {
    val parent = parent
    return when {
        this is KtParameter && parent is KtParameterList -> parent.getParameters().indexOf(this)
        this is PsiParameter && parent is PsiParameterList -> parent.getParameterIndex(this)
        else -> -1
    }
}

fun KtModifierListOwner.isPrivate(): Boolean = hasModifier(KtTokens.PRIVATE_KEYWORD)

fun KtSimpleNameExpression.isImportDirectiveExpression(): Boolean {
    val parent = parent
    return parent is KtImportDirective || parent?.parent is KtImportDirective
}

fun KtSimpleNameExpression.isPackageDirectiveExpression(): Boolean {
    val parent = parent
    return parent is KtPackageDirective || parent?.parent is KtPackageDirective
}

fun KtExpression.isLambdaOutsideParentheses(): Boolean {
    val parent = parent
    return when (parent) {
        is KtLambdaArgument -> true
        is KtLabeledExpression -> parent.isLambdaOutsideParentheses()
        else -> false
    }
}

fun KtExpression.getAssignmentByLHS(): KtBinaryExpression? {
    val parent = parent as? KtBinaryExpression ?: return null
    return if (KtPsiUtil.isAssignment(parent) && parent.left == this) parent else null
}

fun KtStringTemplateExpression.getContentRange(): TextRange {
    val start = node.firstChildNode.textLength
    val lastChild = node.lastChildNode
    val length = textLength
    return TextRange(start, if (lastChild.elementType == KtTokens.CLOSING_QUOTE) length - lastChild.textLength else length)
}

fun KtStringTemplateExpression.isSingleQuoted(): Boolean
        = node.firstChildNode.textLength == 1

fun KtNamedDeclaration.getValueParameters(): List<KtParameter> {
    return getValueParameterList()?.getParameters() ?: Collections.emptyList()
}

fun KtNamedDeclaration.getValueParameterList(): KtParameterList? {
    return when (this) {
        is KtCallableDeclaration -> valueParameterList
        is KtClass -> getPrimaryConstructorParameterList()
        else -> null
    }
}

fun KtLambdaArgument.getLambdaArgumentName(bindingContext: BindingContext): Name? {
    val callExpression = parent as KtCallExpression
    val resolvedCall = callExpression.getResolvedCall(bindingContext)
    return (resolvedCall?.getArgumentMapping(this) as? ArgumentMatch)?.valueParameter?.name
}

fun KtExpression.asAssignment(): KtBinaryExpression? =
        if (KtPsiUtil.isAssignment(this)) this as KtBinaryExpression else null

fun KtDeclaration.visibilityModifier(): PsiElement? {
    val modifierList = modifierList ?: return null
    return KtTokens.VISIBILITY_MODIFIERS.types
                   .asSequence()
                   .map { modifierList.getModifier(it as KtModifierKeywordToken) }
                   .firstOrNull { it != null }
}

fun KtDeclaration.visibilityModifierType(): KtModifierKeywordToken?
        = visibilityModifier()?.node?.elementType as KtModifierKeywordToken?

fun KtStringTemplateExpression.isPlain() = entries.all { it is KtLiteralStringTemplateEntry }

val KtDeclaration.containingClassOrObject: KtClassOrObject?
        get() = (parent as? KtClassBody)?.parent as? KtClassOrObject

fun KtExpression.getOutermostParenthesizerOrThis(): KtExpression {
    return (parentsWithSelf.zip(parents)).firstOrNull {
        val (element, parent) = it
        when (parent) {
            is KtParenthesizedExpression -> false
            is KtAnnotatedExpression -> parent.baseExpression != element
            is KtLabeledExpression -> parent.baseExpression != element
            else -> true
        }
    }?.first as KtExpression? ?: this
}

fun PsiElement.isFunctionalExpression(): Boolean = this is KtNamedFunction && nameIdentifier == null

private val BAD_NEIGHBOUR_FOR_SIMPLE_TEMPLATE_ENTRY_PATTERN = Regex("[a-zA-Z0-9_].*")

fun canPlaceAfterSimpleNameEntry(element: PsiElement?): Boolean {
    val entryText = element?.text ?: return true
    return !BAD_NEIGHBOUR_FOR_SIMPLE_TEMPLATE_ENTRY_PATTERN.matches(entryText)
}

fun checkReservedPrefixWord(sink: DiagnosticSink, element: PsiElement, word: String, suffixTokens: TokenSet, message: String) {
    KtPsiUtil.getPreviousWord(element, word, suffixTokens)?.let {
        sink.report(Errors.UNSUPPORTED.on(it, message))
    }
}

