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

package org.jetbrains.kotlin.psi.psiUtil

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.TokenSet
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

// NOTE: in this file we collect only Kotlin-specific methods working with PSI and not modifying it

// ----------- Calls and qualified expressions ---------------------------------------------------------------------------------------------

fun KtCallElement.getCallNameExpression(): KtSimpleNameExpression? {
    val calleeExpression = calleeExpression ?: return null

    return when (calleeExpression) {
        is KtSimpleNameExpression -> calleeExpression
        is KtConstructorCalleeExpression -> calleeExpression.constructorReferenceExpression
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

fun KtSimpleNameExpression.getQualifiedElementOrCallableRef(): KtElement {
    val parent = parent
    if (parent is KtCallableReferenceExpression && parent.callableReference == this) return parent

    return getQualifiedElement()
}

fun KtSimpleNameExpression.getTopmostParentQualifiedExpressionForSelector(): KtQualifiedExpression? {
    return generateSequence<KtExpression>(this) {
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
            (selector as? KtCallExpression)?.calleeExpression ?: selector
        }
        is KtUserType -> referenceExpression
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
            val grandParent = callExpression.parent
            if (grandParent is KtQualifiedExpression) {
                val parentsReceiver = grandParent.receiverExpression
                if (parentsReceiver != callExpression) {
                    return parentsReceiver
                }
            }
        }
        parent is KtBinaryExpression && parent.operationReference == this -> {
            return if (parent.operationToken in OperatorConventions.IN_OPERATIONS) parent.right else parent.left
        }
        parent is KtUnaryExpression && parent.operationReference == this -> {
            return parent.baseExpression
        }
        parent is KtUserType -> {
            val qualifier = parent.qualifier
            if (qualifier != null) {
                return qualifier.referenceExpression!!
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
        (parent as? KtDotQualifiedExpression)?.receiverExpression == this

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
    if (last == lBrace) return PsiChildRange.EMPTY
    return PsiChildRange(first, last)
}

// ----------- Inheritance -----------------------------------------------------------------------------------------------------------------

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

        val file = containingFile
        if (file is KtFile) {
            val directive = file.findImportByAlias(referencedName)
            if (directive != null) {
                var reference = directive.importedReference
                while (reference is KtDotQualifiedExpression) {
                    reference = reference.selectorExpression
                }
                if (reference is KtSimpleNameExpression) {
                    result.add(reference.getReferencedName())
                }
            }
        }
    }

    require(this is KtClassOrObject) { "it should be ${KtClassOrObject::class} but it is a ${this::class.java.name}" }

    val stub = stub
    if (stub != null) {
        return stub.getSuperNames()
    }

    val specifiers = (this as KtClassOrObject).superTypeListEntries
    if (specifiers.isEmpty()) return Collections.emptyList<String>()

    val result = ArrayList<String>()
    for (specifier in specifiers) {
        val superType = specifier.typeAsUserType
        if (superType != null) {
            val referencedName = superType.referencedName
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
            KtNodeTypes.ANNOTATION -> (child.psi as KtAnnotation).entries
            else -> emptyList<KtAnnotationEntry>()
        }
    }
}

private fun KtAnnotationsContainer.collectAnnotationEntriesFromPsi(): List<KtAnnotationEntry> {
    return children.flatMap { child ->
        when (child) {
            is KtAnnotationEntry -> listOf(child)
            is KtAnnotation -> child.entries
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
        else -> declarations
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
        this is KtParameter && parent is KtParameterList -> parent.parameters.indexOf(this)
        this is PsiParameter && parent is PsiParameterList -> parent.getParameterIndex(this)
        else -> -1
    }
}

fun KtModifierListOwner.isPrivate(): Boolean = hasModifier(KtTokens.PRIVATE_KEYWORD)

fun KtModifierListOwner.isProtected(): Boolean = hasModifier(KtTokens.PROTECTED_KEYWORD)

fun KtSimpleNameExpression.isImportDirectiveExpression(): Boolean {
    val parent = parent
    return parent is KtImportDirective || parent.parent is KtImportDirective
}

fun KtSimpleNameExpression.isPackageDirectiveExpression(): Boolean {
    val parent = parent
    return parent is KtPackageDirective || parent.parent is KtPackageDirective
}

fun KtExpression.isInImportDirective(): Boolean {
    return parents.takeWhile { it !is KtDeclaration && it !is KtBlockExpression }.any { it is KtImportDirective }
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

val KtStringTemplateExpression.plainContent: String
    get() = getContentRange().substring(text)

fun KtStringTemplateExpression.isSingleQuoted(): Boolean
        = node.firstChildNode.textLength == 1

fun KtNamedDeclaration.getValueParameters(): List<KtParameter> {
    return getValueParameterList()?.parameters ?: Collections.emptyList()
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

private fun KtDeclaration.modifierFromTokenSet(set: TokenSet): PsiElement? {
    val modifierList = modifierList ?: return null
    return set.types
            .asSequence()
            .map { modifierList.getModifier(it as KtModifierKeywordToken) }
            .firstOrNull { it != null }

}

fun KtDeclaration.visibilityModifier() = modifierFromTokenSet(KtTokens.VISIBILITY_MODIFIERS)

fun KtDeclaration.visibilityModifierType(): KtModifierKeywordToken?
        = visibilityModifier()?.node?.elementType as KtModifierKeywordToken?

private val MODALITY_MODIFIERS = TokenSet.create(
        KtTokens.ABSTRACT_KEYWORD, KtTokens.FINAL_KEYWORD, KtTokens.SEALED_KEYWORD, KtTokens.OPEN_KEYWORD
)

fun KtDeclaration.modalityModifier() = modifierFromTokenSet(MODALITY_MODIFIERS)

fun KtStringTemplateExpression.isPlain() = entries.all { it is KtLiteralStringTemplateEntry }
fun KtStringTemplateExpression.isPlainWithEscapes() = entries.all { it is KtLiteralStringTemplateEntry || it is KtEscapeStringTemplateEntry }

// Correct for class members only (including constructors and nested classes)
// Returns null e.g. for member function parameters, member function locals, property accessors
val KtDeclaration.containingClassOrObject: KtClassOrObject?
        get() = parent.let {
            when (it) {
                is KtClassBody -> it.parent as? KtClassOrObject
                is KtClassOrObject -> it
                is KtParameterList -> (it.parent as? KtPrimaryConstructor)?.getContainingClassOrObject()
                else -> null
            }
        }

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

fun checkReservedPrefixWord(sink: DiagnosticSink, element: PsiElement, word: String, message: String) {
    KtPsiUtil.getPreviousWord(element, word)?.let {
        sink.report(Errors.UNSUPPORTED.on(it, message))
    }
}

fun checkReservedYield(expression: KtSimpleNameExpression?, sink: DiagnosticSink) {
    // do not force identifier calculation for elements from stubs.
    if (expression?.getReferencedName() != "yield") return

    val identifier = expression.getIdentifier() ?: return

    if (identifier.node.elementType == KtTokens.IDENTIFIER && "yield" == identifier.text) {
        sink.report(Errors.YIELD_IS_RESERVED.on(identifier, "Identifier 'yield' is reserved. Use backticks to call it: `yield`"))
    }
}

val MESSAGE_FOR_YIELD_BEFORE_LAMBDA = "Reserved yield block/lambda. Use 'yield() { ... }' or 'yield(fun...)'"

fun checkReservedYieldBeforeLambda(element: PsiElement, sink: DiagnosticSink) {
    KtPsiUtil.getPreviousWord(element, "yield")?.let {
        sink.report(Errors.YIELD_IS_RESERVED.on(it, MESSAGE_FOR_YIELD_BEFORE_LAMBDA))
    }
}

fun KtElement.nonStaticOuterClasses(): Sequence<KtClass> {
    return generateSequence(containingClass()) { if (it.isInner()) it.containingClass() else null }
}

fun KtElement.containingClass(): KtClass? = getStrictParentOfType()

fun KtClassOrObject.findPropertyByName(name: String): KtNamedDeclaration? {
    return declarations.firstOrNull { it is KtProperty && it.name == name } as KtNamedDeclaration?
           ?: primaryConstructorParameters.firstOrNull { it.hasValOrVar() && it.name == name }
}

fun isTypeConstructorReference(e: PsiElement): Boolean {
    val parent = e.parent
    return parent is KtUserType && parent.referenceExpression == e
}

fun KtParameter.isPropertyParameter() = ownerFunction is KtPrimaryConstructor && hasValOrVar()

fun isDoubleColonReceiver(expression: KtExpression) = expression.getParentOfTypeAndBranch<KtDoubleColonExpression> { this.receiverExpression } != null

fun KtFunctionLiteral.getOrCreateParameterList(): KtParameterList {
    valueParameterList?.let { return it }

    val psiFactory = KtPsiFactory(this)

    val anchor = lBrace
    val newParameterList = addAfter(psiFactory.createLambdaParameterList("x"), anchor) as KtParameterList
    newParameterList.removeParameter(0)
    if (arrow == null) {
        val whitespaceAndArrow = psiFactory.createWhitespaceAndArrow()
        addRangeAfter(whitespaceAndArrow.first, whitespaceAndArrow.second, newParameterList)
    }
    return newParameterList
}

fun KtCallExpression.getOrCreateValueArgumentList(): KtValueArgumentList {
    valueArgumentList?.let { return it }
    return addAfter(KtPsiFactory(this).createCallArguments("()"),
                    typeArgumentList ?: calleeExpression) as KtValueArgumentList
}

fun KtDeclaration.hasBody() = when (this) {
    is KtFunction -> hasBody()
    is KtProperty -> hasBody()
    else -> false
}


fun KtExpression.referenceExpression(): KtReferenceExpression? =
        (if (this is KtCallExpression) calleeExpression else this) as? KtReferenceExpression

fun KtExpression.getLabeledParent(labelName: String): KtLabeledExpression? {
    parents.forEach {
        when (it) {
            is KtLabeledExpression -> if (it.getLabelName() == labelName) return it
            is KtParenthesizedExpression, is KtAnnotatedExpression, is KtLambdaExpression -> return@forEach
            else -> return null
        }
    }
    return null
}