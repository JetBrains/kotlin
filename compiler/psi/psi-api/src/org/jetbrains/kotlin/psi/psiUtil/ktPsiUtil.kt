/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.psiUtil

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.MODALITY_MODIFIERS
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.psi.utils.OperatorTokens
import java.util.*

// NOTE: in this file we collect only Kotlin-specific methods working with PSI and not modifying it

// ----------- Calls and qualified expressions ---------------------------------------------------------------------------------------------

/** Returns the simple-name reference of this call's callee (its function or constructor name), or `null`. */
fun KtCallElement.getCallNameExpression(): KtSimpleNameExpression? {
    val calleeExpression = calleeExpression ?: return null

    return when (calleeExpression) {
        is KtSimpleNameExpression -> calleeExpression
        is KtConstructorCalleeExpression -> calleeExpression.constructorReferenceExpression
        else -> null
    }
}

/**
 * Returns the enclosing qualified element for this name: a [KtQualifiedExpression], a [KtUserType], or this expression
 * itself if there is no such qualifier.
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

/**
 * Like [getQualifiedElement], but also returns the enclosing [KtCallableReferenceExpression] when this name is its
 * callable reference.
 */
fun KtSimpleNameExpression.getQualifiedElementOrCallableRef(): KtElement {
    val parent = parent
    if (parent is KtCallableReferenceExpression && parent.callableReference == this) return parent

    return getQualifiedElement()
}

/** Returns the outermost qualified expression in which this name is (transitively) the selector, or `null`. */
fun KtSimpleNameExpression.getTopmostParentQualifiedExpressionForSelector(): KtQualifiedExpression? {
    return generateSequence<KtExpression>(this) {
        val parentQualified = it.parent as? KtQualifiedExpression
        if (parentQualified?.selectorExpression == it) parentQualified else null
    }.last() as? KtQualifiedExpression
}

/**
 * Returns the rightmost selector of this qualified element, or `null` if there is no such selector.
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

/**
 * Returns the receiver this name is applied to, considering qualified expressions, binary/unary operators, and
 * qualified user types, or `null` if this name has no receiver.
 */
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
            val grandParent = parent.parent
            if (grandParent is KtQualifiedExpression) {
                val parentsReceiver = grandParent.receiverExpression
                if (parentsReceiver != parent) {
                    return parentsReceiver
                }
            }
        }
        parent is KtBinaryExpression && parent.operationReference == this -> {
            return if (parent.operationToken in OperatorTokens.IN_OPERATIONS) parent.right else parent.left
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

/** Returns the qualified expression in which this element is the selector, or `null` if it is not a selector. */
fun KtElement.getQualifiedExpressionForSelector(): KtQualifiedExpression? {
    val parent = parent
    return if (parent is KtQualifiedExpression && parent.selectorExpression == this) parent else null
}

/** Returns the qualified expression in which this expression is the selector, or this expression itself otherwise. */
fun KtExpression.getQualifiedExpressionForSelectorOrThis(): KtExpression {
    return getQualifiedExpressionForSelector() ?: this
}

/** Returns the qualified expression in which this expression is the receiver, or `null` if it is not a receiver. */
fun KtExpression.getQualifiedExpressionForReceiver(): KtQualifiedExpression? {
    val parent = parent
    return if (parent is KtQualifiedExpression && parent.receiverExpression == this) parent else null
}

/** Returns the qualified expression in which this expression is the receiver, or this expression itself otherwise. */
fun KtExpression.getQualifiedExpressionForReceiverOrThis(): KtExpression {
    return getQualifiedExpressionForReceiver() ?: this
}

/** Returns `true` if this expression is the receiver of an enclosing dot-qualified expression. */
fun KtExpression.isDotReceiver(): Boolean =
    (parent as? KtDotQualifiedExpression)?.receiverExpression == this

/** Returns `true` if this expression is the selector of an enclosing dot-qualified expression. */
fun KtExpression.isDotSelector(): Boolean =
    (parent as? KtDotQualifiedExpression)?.selectorExpression == this

/**
 * Returns the call expression this expression represents: the selector's call for a qualified expression, or this
 * expression if it is itself a call. Returns `null` if there is no call.
 */
fun KtExpression.getPossiblyQualifiedCallExpression(): KtCallExpression? =
    ((this as? KtQualifiedExpression)?.selectorExpression ?: this) as? KtCallExpression

// ---------- Block expression -------------------------------------------------------------------------------------------------------------

/** Returns the statements of this element if it is a block, otherwise a single-element sequence of this element. */
fun KtElement.blockExpressionsOrSingle(): Sequence<KtElement> =
    if (this is KtBlockExpression) statements.asSequence() else sequenceOf(this)

/** Returns the last statement if this expression is a block, otherwise this expression itself. */
fun KtExpression.lastBlockStatementOrThis(): KtExpression = (this as? KtBlockExpression)?.statements?.lastOrNull() ?: this

/** Returns the range of statements between the braces of this block, excluding the braces and outer whitespace. */
fun KtBlockExpression.contentRange(): PsiChildRange {
    val lBrace = this.lBrace ?: return PsiChildRange.EMPTY
    val rBrace = this.rBrace ?: return PsiChildRange.EMPTY

    val first = lBrace.siblings(withItself = false).firstOrNull { it !is PsiWhiteSpace }
    if (first == rBrace) return PsiChildRange.EMPTY

    val last = rBrace.siblings(forward = false, withItself = false).first { it !is PsiWhiteSpace }
    if (last == lBrace) return PsiChildRange.EMPTY

    return PsiChildRange(first, last)
}

// ----------- Inheritance -----------------------------------------------------------------------------------------------------------------

/** Returns `true` if this class is abstract, that is, it is an interface or has the `abstract` modifier. */
fun KtClass.isAbstract(): Boolean = isInterface() || hasModifier(KtTokens.ABSTRACT_KEYWORD)

/**
 * Returns the unqualified names indexed as this class's superclass names. For names that might be imported through an
 * alias, this includes both the original and aliased names; reference resolution during inheritor search disambiguates
 * them.
 *
 * @return the list of possible superclass names
 */
fun StubBasedPsiElementBase<out KotlinClassOrObjectStub<out KtClassOrObject>>.getSuperNames(): List<String> {
    fun addSuperName(result: MutableList<String>, referencedName: String) {
        result.add(referencedName)

        val file = containingFile
        if (file is KtFile) {
            getImportedSimpleNameByImportAlias(file, referencedName)?.let(result::add)
        }
    }

    require(this is KtClassOrObject) { "it should be ${KtClassOrObject::class} but it is a ${this::class.java.name}" }

    val stub = greenStub
    if (stub != null) {
        return stub.superNames
    }

    val specifiers = this.superTypeListEntries
    if (specifiers.isEmpty()) return Collections.emptyList()

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

/**
 * Returns the annotation entries applied to this expression through an enclosing annotated (or labeled) expression, or
 * an empty list if there are none.
 *
 * Annotations on labeled expressions lie on their base expressions.
 */
fun KtExpression.getAnnotationEntries(): List<KtAnnotationEntry> {
    val parent = parent
    return when (parent) {
        is KtAnnotatedExpression -> parent.annotationEntries
        is KtLabeledExpression -> parent.getAnnotationEntries()
        else -> emptyList()
    }
}

/** Collects the annotation entries of this container, reading from the stub when available and falling back to the PSI. */
fun KtAnnotationsContainer.collectAnnotationEntriesFromStubOrPsi(): List<KtAnnotationEntry> {
    return when (this) {
        is StubBasedPsiElementBase<*> -> stub?.collectAnnotationEntriesFromStubElement() ?: collectAnnotationEntriesFromPsi()
        else -> collectAnnotationEntriesFromPsi()
    }
}

private fun StubElement<*>.collectAnnotationEntriesFromStubElement(): List<KtAnnotationEntry> {
    return childrenStubs.flatMap { child ->
        @Suppress("DEPRECATION") // KT-78356
        val stubType = child.stubType
        when (stubType) {
            KtNodeTypes.ANNOTATION_ENTRY -> listOf(child.psi as KtAnnotationEntry)
            KtNodeTypes.ANNOTATION -> (child.psi as KtAnnotation).entries
            else -> emptyList()
        }
    }
}

private fun KtAnnotationsContainer.collectAnnotationEntriesFromPsi(): List<KtAnnotationEntry> {
    return children.flatMap { child ->
        when (child) {
            is KtAnnotationEntry -> listOf(child)
            is KtAnnotation -> child.entries
            else -> emptyList()
        }
    }
}

// -------- Recursive tree visiting --------------------------------------------------------------------------------------------------------

/**
 * Returns a recursive visitor that calls [block] on each descendant of type [T]. Calls happen in DFS-exit order, so
 * deeper nodes are visited before their ancestors.
 */
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

/**
 * Returns a recursive visitor that maps each descendant of type [T] via [map] and adds all results to [accumulator].
 */
inline fun <reified T : KtElement, R> flatMapDescendantsOfTypeVisitor(
    accumulator: MutableCollection<R>,
    noinline map: (T) -> Collection<R>,
): KtVisitorVoid {
    return forEachDescendantOfTypeVisitor<T> { accumulator.addAll(map(it)) }
}

// ----------- Contracts -------------------------------------------------------------------------------------------------------------------

/**
 * Whether the declaration may have a legacy contract (a contract defined inside the body).
 *
 * In other words, **false** guarantees that the declaration cannot have a contract,
 * but **true** does not guarantee that the declaration has a contract.
 */
@KtImplementationDetail
fun KtDeclarationWithBody.isLegacyContractPresentPsiCheck(): Boolean {
    val statement = bodyBlockExpression?.firstStatement ?: return false
    val unwrappedExpression = statement.unwrapParenthesesLabelsAndAnnotations() as? KtExpression ?: return false
    return unwrappedExpression.isContractDescriptionCallPsiCheck()
}

/**
 * Best-effort PSI check for whether this function's body begins with a `contract { ... }` call, taking into account
 * whether contracts are allowed on member functions via [isAllowedOnMembers]. `false` is definitive; `true` is not a
 * guarantee.
 */
fun KtNamedFunction.isContractPresentPsiCheck(isAllowedOnMembers: Boolean): Boolean {
    val contractAllowedHere =
        (isAllowedOnMembers || isTopLevel) &&
                hasBlockBody() &&
                !hasModifier(KtTokens.OPERATOR_KEYWORD)
    if (!contractAllowedHere) return false

    val firstExpression = (this as? KtFunction)?.bodyBlockExpression?.statements?.firstOrNull() ?: return false

    return firstExpression.isContractDescriptionCallPsiCheck()
}

/** Best-effort PSI check for whether this expression is a `contract { ... }` (or `kotlin.contracts.contract { ... }`) call. */
fun KtExpression.isContractDescriptionCallPsiCheck(): Boolean =
    (this is KtCallExpression && calleeExpression?.text == "contract") || (this is KtQualifiedExpression && isContractDescriptionCallPsiCheck())

/** Best-effort PSI check for whether this qualified expression is a `kotlin.contracts.contract { ... }` call. */
@OptIn(KtPsiInconsistencyHandling::class)
fun KtQualifiedExpression.isContractDescriptionCallPsiCheck(): Boolean {
    val expression = selectorExpression ?: return false
    return receiverExpressionOrNull?.text == "kotlin.contracts" && expression.isContractDescriptionCallPsiCheck()
}

/** Returns `true` if this element is the first statement of its enclosing block (also seeing through a dot qualifier). */
fun KtElement.isFirstStatement(): Boolean {
    var parent = parent
    var element = this
    if (parent is KtDotQualifiedExpression) {
        element = parent
        parent = parent.parent
    }
    return parent is KtBlockExpression && parent.firstStatement == element
}


// ----------- Other -----------------------------------------------------------------------------------------------------------------------

/**
 * Returns the declarations of this class or object, including primary-constructor `val`/`var` property parameters for
 * a class.
 */
fun KtClassOrObject.effectiveDeclarations(): List<KtDeclaration> {
    return when (this) {
        is KtClass -> getDeclarations() + getPrimaryConstructorParameters().filter { p -> p.hasValOrVar() }
        else -> declarations
    }
}

/** Returns `true` if this element is an extension function or property (that is, it declares a receiver). */
fun PsiElement.isExtensionDeclaration(): Boolean {
    val callable: KtCallableDeclaration? = when (this) {
        is KtNamedFunction, is KtProperty -> this as KtCallableDeclaration
        is KtPropertyAccessor -> getNonStrictParentOfType<KtProperty>()
        else -> null
    }

    return callable?.receiverTypeReference != null
}

/**
 * Returns `true` if this declaration is `expect`, either directly (via the modifier) or by being contained in an
 * `expect` declaration.
 */
fun KtDeclaration.isExpectDeclaration(): Boolean = when {
    hasExpectModifier() -> true
    this is KtParameter -> ownerDeclaration?.isExpectDeclaration() == true
    else -> containingClassOrObject?.isExpectDeclaration() == true
}

/**
 * Returns `true` if this declaration is `actual`: either it has an explicit `actual` modifier, or it is a constructor
 * of an `actual` annotation, value, or inline class.
 */
fun KtDeclaration.isActualDeclaration(): Boolean = hasActualModifier() || isImplicitlyActualDeclaration()

internal fun KtDeclaration.isImplicitlyActualDeclaration(): Boolean = when (this) {
    is KtConstructor<*> -> (containingClassOrObject as? KtClass)?.let { klass ->
        klass.hasActualModifier() && klass.allowsImplicitlyActualConstructor()
    } == true
    else -> false
}

internal fun KtClass.allowsImplicitlyActualConstructor() = isAnnotation() || isValue() || isInline()

/** Returns `true` if this declaration declares context receivers (or context parameters). */
fun KtElement.isContextualDeclaration(): Boolean {
    val contextReceivers = when (this) {
        is KtCallableDeclaration -> contextReceivers
        is KtClassOrObject -> contextReceivers
        else -> emptyList()
    }
    return contextReceivers.isNotEmpty()
}

/** Returns `true` if this is the body of an object literal (`object : Foo { ... }`). */
fun KtClassOrObject.isObjectLiteral(): Boolean = this is KtObjectDeclaration && isObjectLiteral()

/** Returns the index of this parameter within its parameter list (Kotlin or Java), or `-1` if it is not a parameter. */
//TODO: strange method, and not only Kotlin specific (also Java)
fun PsiElement.parameterIndex(): Int {
    val parent = parent
    return when (this) {
        is KtParameter if parent is KtParameterList -> parent.parameters.indexOf(this)
        is KtParameter if parent is KtContextParameterList -> parent.contextParameters.indexOf(this)
        is PsiParameter if parent is PsiParameterList -> parent.getParameterIndex(this)
        else -> -1
    }
}

/** The index of this argument within its enclosing value argument list. */
val KtValueArgument.argumentIndex: Int get() = (parent as KtValueArgumentList).arguments.indexOf(this)

/** Returns `true` if this declaration has the `private` modifier. */
fun KtModifierListOwner.isPrivate(): Boolean = hasModifier(KtTokens.PRIVATE_KEYWORD)

/** Returns `true` if this declaration has the `protected` modifier. */
fun KtModifierListOwner.isProtected(): Boolean = hasModifier(KtTokens.PROTECTED_KEYWORD)

/** Returns `true` if this name is part of an `import` directive. */
fun KtSimpleNameExpression.isImportDirectiveExpression(): Boolean {
    val parent = parent
    return parent is KtImportDirective || parent.parent is KtImportDirective
}

/** Returns `true` if this name is part of a `package` directive. */
fun KtSimpleNameExpression.isPackageDirectiveExpression(): Boolean {
    val parent = parent
    return parent is KtPackageDirective || parent.parent is KtPackageDirective
}

/** Returns `true` if this expression appears inside an `import` directive. */
fun KtExpression.isInImportDirective(): Boolean {
    return parents.takeWhile { it !is KtDeclaration && it !is KtBlockExpression }.any { it is KtImportDirective }
}

/** Returns `true` if this expression is a trailing lambda argument written outside the call parentheses. */
fun KtExpression.isLambdaOutsideParentheses(): Boolean {
    val parent = parent
    return when (parent) {
        is KtLambdaArgument -> true
        is KtLabeledExpression -> parent.isLambdaOutsideParentheses()
        else -> false
    }
}

/** Returns the assignment in which this expression is the left-hand side, or `null` if it is not assigned to. */
fun KtExpression.getAssignmentByLHS(): KtBinaryExpression? {
    val parent = parent as? KtBinaryExpression ?: return null
    return if (KtPsiUtil.isAssignment(parent) && parent.left == this) parent else null
}

/** Walks up from [element] through qualified/simple-name expressions to find a plain `=` assignment whose LHS it is. */
tailrec fun findAssignment(element: PsiElement?): KtBinaryExpression? =
    when (val parent = element?.parent) {
        is KtBinaryExpression -> if (parent.left == element && parent.operationToken == KtTokens.EQ) parent else null
        is KtQualifiedExpression -> findAssignment(element.parent)
        is KtSimpleNameExpression -> findAssignment(element.parent)
        else -> null
    }

/** Returns the text range of the string content, excluding the interpolation prefix and the opening/closing quotes. */
fun KtStringTemplateExpression.getContentRange(): TextRange {
    val interpolationPrefixOrOpenQuote = node.firstChildNode ?: return TextRange.EMPTY_RANGE
    val openQuoteAfterPrefixOrNull = interpolationPrefixOrOpenQuote.treeNext?.takeIf { secondNode ->
        interpolationPrefixOrOpenQuote.elementType == KtNodeTypes.STRING_INTERPOLATION_PREFIX && secondNode.elementType == KtTokens.OPEN_QUOTE
    }
    val start = interpolationPrefixOrOpenQuote.textLength + (openQuoteAfterPrefixOrNull?.textLength ?: 0)
    val lastChild = node.lastChildNode
    val length = textLength
    return TextRange(start, if (lastChild.elementType == KtTokens.CLOSING_QUOTE) length - lastChild.textLength else length)
}

/**
 * Returns `true` if this expression can be the callee of a call with the same name.
 *
 * `this` in `this(args)` is not considered a callee, and `name` in `name++` is not considered a callee either.
 */
fun KtSimpleNameExpression.isCallee(): Boolean {
    val parent = parent
    return when (parent) {
        is KtCallElement -> parent.calleeExpression == this
        is KtBinaryExpression -> parent.operationReference == this
        else -> {
            val callElement =
                getStrictParentOfType<KtUserType>()
                    ?.getStrictParentOfType<KtTypeReference>()
                    ?.getStrictParentOfType<KtConstructorCalleeExpression>()
                    ?.getStrictParentOfType<KtCallElement>()

            if (callElement != null) {
                val ktConstructorCalleeExpression = callElement.calleeExpression as? KtConstructorCalleeExpression
                (ktConstructorCalleeExpression?.typeReference?.typeElement as? KtUserType)?.referenceExpression == this
            } else {
                false
            }
        }
    }
}

/** The literal text content of the string, excluding the quotes and interpolation prefix. */
val KtStringTemplateExpression.plainContent: String
    get() = getContentRange().substring(text)

/** Returns `true` if this is a single-quoted string (`"..."`), as opposed to a triple-quoted raw string. */
fun KtStringTemplateExpression.isSingleQuoted(): Boolean =
    node.findChildByType(KtTokens.OPEN_QUOTE)?.textLength == 1

/** `true` if this is a private nested (non-top-level) class or object. */
val KtNamedDeclaration.isPrivateNestedClassOrObject: Boolean get() = this is KtClassOrObject && isPrivate() && !isTopLevel()

/** Returns the value parameters of this declaration (constructor value parameters for a class), or an empty list. */
fun KtNamedDeclaration.getValueParameters(): List<KtParameter> {
    return getValueParameterList()?.parameters ?: Collections.emptyList()
}

/** Returns the value parameter list of this declaration (the primary constructor's for a class), or `null`. */
fun KtNamedDeclaration.getValueParameterList(): KtParameterList? {
    return when (this) {
        is KtCallableDeclaration -> valueParameterList
        is KtClass -> getPrimaryConstructorParameterList()
        else -> null
    }
}

/** Returns this expression as a [KtBinaryExpression] if it is an assignment, or `null` otherwise. */
fun KtExpression.asAssignment(): KtBinaryExpression? =
    if (KtPsiUtil.isAssignment(this)) this as KtBinaryExpression else null

private fun KtModifierList.modifierFromTokenSet(set: TokenSet): PsiElement? {
    return set.types
        .asSequence()
        .map { getModifier(it as KtModifierKeywordToken) }
        .firstOrNull { it != null }

}

private fun KtModifierListOwner.modifierFromTokenSet(set: TokenSet) = modifierList?.modifierFromTokenSet(set)

/** Returns the visibility modifier token (`public`/`private`/`protected`/`internal`), or `null` if none is present. */
fun KtModifierList.visibilityModifier() = modifierFromTokenSet(KtTokens.VISIBILITY_MODIFIERS)

/** Returns the type of the visibility modifier, or `null` if none is present. */
fun KtModifierList.visibilityModifierType(): KtModifierKeywordToken? = visibilityModifier()?.node?.elementType as KtModifierKeywordToken?

/** Returns the visibility modifier token of this declaration, or `null` if none is present. */
fun KtModifierListOwner.visibilityModifier() = modifierList?.modifierFromTokenSet(KtTokens.VISIBILITY_MODIFIERS)

/** `true` if this declaration is effectively public (no visibility modifier or an explicit `public`), and not local. */
val KtModifierListOwner.isPublic: Boolean
    get() {
        if (this is KtDeclaration && KtPsiUtil.isLocal(this)) return false
        val visibilityModifier = visibilityModifierType()
        return visibilityModifier == null || visibilityModifier == KtTokens.PUBLIC_KEYWORD
    }

/** Returns the type of this declaration's visibility modifier, or `null` if none is present. */
fun KtModifierListOwner.visibilityModifierType(): KtModifierKeywordToken? =
    visibilityModifier()?.node?.elementType as KtModifierKeywordToken?

/** Returns the type of this declaration's visibility modifier, or the default visibility (`public`) if none is present. */
fun KtModifierListOwner.visibilityModifierTypeOrDefault(): KtModifierKeywordToken =
    visibilityModifierType() ?: KtTokens.DEFAULT_VISIBILITY_KEYWORD

/** Returns the modality modifier token (`abstract`/`open`/`final`/`sealed`), or `null` if none is present. */
fun KtDeclaration.modalityModifier() = modifierFromTokenSet(MODALITY_MODIFIERS)

/** Returns the type of this declaration's modality modifier, or `null` if none is present. */
fun KtDeclaration.modalityModifierType(): KtModifierKeywordToken? = modalityModifier()?.node?.elementType as KtModifierKeywordToken?

/** Returns `true` if this string consists only of literal text, with no interpolation or escape sequences. */
fun KtStringTemplateExpression.isPlain() = entries.all { it is KtLiteralStringTemplateEntry }

/** Returns `true` if this string consists only of literal text and escape sequences, with no interpolation. */
fun KtStringTemplateExpression.isPlainWithEscapes() =
    entries.all { it is KtLiteralStringTemplateEntry || it is KtEscapeStringTemplateEntry }

/**
 * The class or object that declares this declaration as a member (including constructors and nested classes), or `null`
 * if it is not a class member — for example, a member function's parameter or local, or a property accessor.
 */
val KtDeclaration.containingClassOrObject: KtClassOrObject?
    get() = when (val parent = parent) {
        is KtClassBody -> parent.containingClassOrObject
        is KtClassOrObject -> parent
        is KtParameterList -> (parent.parent as? KtPrimaryConstructor)?.getContainingClassOrObject()
        is KtDestructuringDeclaration if this is KtDestructuringDeclarationEntry -> parent.containingClassOrObject
        else -> null
    }

/**
 * Whether this declaration is declared inside a companion object block.
 *
 * ### Example:
 *
 * ```kotlin
 * class Foo {
 *   companion {
 *     fun static1() {} // true
 *   }
 *
 *   fun regular() {} // false
 * }
 * ```
 */
@KtExperimentalApi
val KtDeclaration.isFromCompanionBlock: Boolean
    get() = (parent as? KtClassBody)?.parent is KtCompanionBlock

/**
 * Whether the callable is a
 * [companion extension](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0449-companions-block-extension.md#companion-extensions)
 * or comes from a
 * [companion block](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0449-companions-block-extension.md#companion-blocks).
 *
 * **Note:** according to the KEEP, [KtEnumEntry]s are also considered implicitly declared in a companion block.
 */
@KtExperimentalApi
val KtDeclarationWithReturnType.isCompanion: Boolean
    get() = this is KtEnumEntry || hasModifier(KtTokens.COMPANION_KEYWORD) || isFromCompanionBlock

/**
 * The containing script for top-level declarations.
 *
 * @see containingClassOrObject
 */
@KtExperimentalApi
val KtDeclaration.containingScript: KtScript?
    get() = when (val parent = parent) {
        is KtBlockExpression -> parent.containingScript
        is KtDestructuringDeclaration if this is KtDestructuringDeclarationEntry -> parent.containingScript
        else -> null
    }

/**
 * The containing class or script for a declaration.
 *
 * @see containingClassOrObject
 * @see containingScript
 */
@KtExperimentalApi
val KtDeclaration.containingClassOrScript: KtNamedDeclaration?
    get() = containingClassOrObject ?: containingScript

/**
 * The containing script for the block expression.
 *
 * @see KtDeclaration.containingScript
 */
@KtExperimentalApi
val KtBlockExpression.containingScript: KtScript?
    get() = parent as? KtScript

/**
 * The containing [ClassId] for this declaration. REPL [KtScript]s are supported.
 *
 * @see containingScript
 * @see containingClassOrObject
 */
@KtExperimentalApi
val KtDeclaration.containingClassId: ClassId?
    get() {
        containingClassOrObject?.let {
            return it.getClassId()
        }

        val script = containingScript?.takeIf(KtScript::isReplSnippet) ?: return null
        return ClassId.topLevel(script.fqName)
    }


/**
 * The containing class for the body.
 *
 * **Note**: it bypasses [KtCompanionBlock].
 */
@OptIn(KtExperimentalApi::class)
val KtClassBody.containingClassOrObject: KtClassOrObject?
    get() = when (val parent = parent) {
        is KtClassOrObject -> parent
        is KtCompanionBlock -> parent.containingClassOrObject
        else -> null
    }

/**
 * The containing class for the companion block.
 */
@KtExperimentalApi
val KtCompanionBlock.containingClassOrObject: KtClassOrObject?
    get() = (parent as? KtClassBody)?.containingClassOrObject

/**
 * Returns the outermost expression that wraps this one only through parentheses, labels, or annotations, or this
 * expression itself.
 */
fun KtExpression.getOutermostParenthesizerOrThis(): KtExpression {
    return (parentsWithSelf.zip(parents)).firstOrNull {
        val [element, parent] = it
        when (parent) {
            is KtParenthesizedExpression -> false
            is KtAnnotatedExpression -> parent.baseExpression != element
            is KtLabeledExpression -> parent.baseExpression != element
            else -> true
        }
    }?.first as KtExpression? ?: this
}

/** Returns `true` if this element is an anonymous function (a `fun` without a name). */
fun PsiElement.isFunctionalExpression(): Boolean = this is KtNamedFunction && nameIdentifier == null

private val BAD_NEIGHBOUR_FOR_SIMPLE_TEMPLATE_ENTRY_PATTERN = Regex("([a-zA-Z0-9_]|[^\\p{ASCII}]).*")

/**
 * Returns `true` if placing text after a `$name` simple-name template entry would not accidentally extend the name,
 * that is, [element] does not begin with an identifier character.
 */
fun canPlaceAfterSimpleNameEntry(element: PsiElement?): Boolean {
    val entryText = element?.text ?: return true
    return !BAD_NEIGHBOUR_FOR_SIMPLE_TEMPLATE_ENTRY_PATTERN.matches(entryText)
}

/** Returns the enclosing classes that this element can access an outer instance of, from innermost to outermost. */
fun KtElement.nonStaticOuterClasses(): Sequence<KtClass> {
    return generateSequence(containingClass()) { if (it.isInner()) it.containingClass() else null }
}

/** Returns the nearest enclosing [KtClass], or `null` if there is none. */
fun KtElement.containingClass(): KtClass? = getStrictParentOfType()

/** Returns the member property (or primary-constructor property parameter) with the given [name], or `null`. */
fun KtClassOrObject.findPropertyByName(name: String): KtNamedDeclaration? {
    return declarations.firstOrNull { it is KtProperty && it.name == name } as KtNamedDeclaration?
        ?: primaryConstructorParameters.firstOrNull { it.hasValOrVar() && it.name == name }
}

/** Returns the member function with the given [name], or `null` if there is none. */
fun KtClassOrObject.findFunctionByName(name: String): KtNamedDeclaration? {
    return declarations.firstOrNull { it is KtNamedFunction && it.name == name } as KtNamedDeclaration?
}

/** Returns `true` if [e] is the reference expression naming the classifier in a user type. */
fun isTypeConstructorReference(e: PsiElement): Boolean {
    val parent = e.parent
    return parent is KtUserType && parent.referenceExpression == e
}

/** Returns `true` if this parameter is a primary-constructor `val`/`var` property parameter. */
fun KtParameter.isPropertyParameter() = ownerFunction is KtPrimaryConstructor && hasValOrVar()

/** Returns `true` if [expression] is the receiver of an enclosing `::` (callable reference or class literal) expression. */
fun isDoubleColonReceiver(expression: KtExpression) =
    expression.getParentOfTypeAndBranch<KtDoubleColonExpression> { this.receiverExpression } != null

@Deprecated(
    "Use getOrCreateFunctionLiteralParameterList() instead",
    ReplaceWith(
        "this.getOrCreateFunctionLiteralParameterList()",
        "org.jetbrains.kotlin.idea.base.psi.getOrCreateFunctionLiteralParameterList",
    ),
)
@OptIn(KtNonPublicApi::class)
fun KtFunctionLiteral.getOrCreateParameterList(): KtParameterList =
    KtPsiMutationService.getInstance().getOrCreateFunctionLiteralParameterList(this)

/**
 * Returns the label (explicit or the enclosing call's name) and the call that this lambda is passed to, as a pair.
 * Either component may be `null` if it cannot be determined.
 */
fun KtFunctionLiteral.findLabelAndCall(): Pair<Name?, KtCallExpression?> {
    val literalParent = (this.parent as KtLambdaExpression).parent

    fun KtValueArgument.callExpression(): KtCallExpression? {
        val parent = parent
        return (if (parent is KtValueArgumentList) parent else this).parent as? KtCallExpression
    }

    when (literalParent) {
        is KtLabeledExpression -> {
            val callExpression = (literalParent.parent as? KtValueArgument)?.callExpression()
            return Pair(literalParent.getLabelNameAsName(), callExpression)
        }

        is KtValueArgument -> {
            val callExpression = literalParent.callExpression()
            val label = (callExpression?.calleeExpression as? KtSimpleNameExpression)?.getReferencedNameAsName()
            return Pair(label, callExpression)
        }

        else -> {
            return Pair(null, null)
        }
    }
}

@Deprecated(
    "Use getOrCreateCallValueArgumentList() instead",
    ReplaceWith(
        "this.getOrCreateCallValueArgumentList()",
        "org.jetbrains.kotlin.idea.base.psi.getOrCreateCallValueArgumentList",
    ),
)
@OptIn(KtNonPublicApi::class)
fun KtCallExpression.getOrCreateValueArgumentList(): KtValueArgumentList =
    KtPsiMutationService.getInstance().getOrCreateCallValueArgumentList(this)

@Deprecated(
    "Use appendTypeArgument(typeArgument) instead",
    ReplaceWith(
        "this.appendTypeArgument(typeArgument)",
        "org.jetbrains.kotlin.idea.base.psi.appendTypeArgument",
    ),
)
@OptIn(KtNonPublicApi::class)
fun KtCallExpression.addTypeArgument(typeArgument: KtTypeProjection) {
    KtPsiMutationService.getInstance().appendTypeArgument(this, typeArgument)
}

/** Returns `true` if this declaration has a body (a function or property that defines one). */
fun KtDeclaration.hasBody() = when (this) {
    is KtFunction -> hasBody()
    is KtProperty -> hasBody()
    else -> false
}


/** Returns the reference expression of this expression (the callee for a call), or `null` if it is not a reference. */
fun KtExpression.referenceExpression(): KtReferenceExpression? =
    (if (this is KtCallExpression) calleeExpression else this) as? KtReferenceExpression

/** Returns the nearest enclosing labeled expression with the given [labelName], or `null` if there is none. */
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

@Deprecated(
    "Use astReplace(newElement) instead",
    ReplaceWith("this.astReplace(newElement)", "org.jetbrains.kotlin.idea.base.psi.astReplace"),
)
@OptIn(KtNonPublicApi::class)
fun PsiElement.astReplace(newElement: PsiElement) {
    KtPsiMutationService.getInstance().astReplace(this, newElement)
}

@Deprecated("The API is deprecated and is preserved only for compatibility with K1")
var KtElement.parentSubstitute: PsiElement? by UserDataProperty(Key.create("PARENT_SUBSTITUTE"))

private val HARD_KEYWORDS: Set<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    KtTokens.KEYWORDS.types.mapTo(HashSet()) { (it as KtKeywordToken).value }
}

/**
 * Checks if this string is a valid Kotlin identifier.
 *
 * A regular identifier (without backticks) must:
 * - Start with a letter (including Unicode letters) or underscore;
 * - Contain only letters, digits, or underscores;
 * - Not be a hard keyword.
 *
 * Escaped identifiers (strings starting with a backtick) are also supported: the function returns `true` for strings
 * such as `` `class` `` or `` `with spaces` ``.
 *
 * The function performs only basic, platform-agnostic validation. Individual build targets may impose additional
 * restrictions; for example, the JVM target also applies Java bytecode and Dalvik restrictions.
 * See `org.jetbrains.kotlin.resolve.jvm.checkers.DalvikIdentifierUtils.isValidDalvikCharacter`.
 *
 * @see quoteIfNeeded
 */
fun String?.isIdentifier(): Boolean {
    if (this == null || isEmpty()) return false

    if (startsWith("`")) {
        // Mirrors escaped identifier rules in the lexer (see Kotlin.flex)
        val unescaped = removeSurrounding("`")
        return unescaped.isNotEmpty() && unescaped.all { it != '`' && it != '\n' }
    }

    if (this in HARD_KEYWORDS) {
        return false
    }

    val length = this.length
    var index = 0

    while (index < length) {
        val codePoint = codePointAt(index)

        val isValid = (codePoint == '_'.code)
                || Character.isLetter(codePoint)
                || (index > 0 && Character.isDigit(codePoint))

        if (!isValid) {
            return false
        }

        index += Character.charCount(codePoint)
    }

    return true
}

/** Returns this string as-is if it is a valid identifier, otherwise wrapped in backticks. */
fun String.quoteIfNeeded(): String = if (this.isIdentifier()) this else "`$this`"

/** Returns `true` if this element is a top-level member of a Kotlin file or a top-level Java class. */
fun PsiElement.isTopLevelKtOrJavaMember(): Boolean {
    return when (this) {
        is KtDeclaration -> isKtFile(parent)
        is PsiClass -> containingClass == null && this.qualifiedName != null
        else -> false
    }
}

/** Returns this declaration's name, substituting the "no name provided" special name for a missing or special name. */
fun KtNamedDeclaration.safeNameForLazyResolve(): Name {
    return nameAsName.safeNameForLazyResolve()
}

/** Returns this name, or the "no name provided" special name if it is `null` or special. */
fun Name?.safeNameForLazyResolve(): Name = this?.takeUnless(Name::isSpecial) ?: SpecialNames.NO_NAME_PROVIDED

/** Returns this declaration's fully qualified name using the safe name for lazy resolution, or `null` if unavailable. */
fun KtNamedDeclaration.safeFqNameForLazyResolve(): FqName? {
    //NOTE: should only create special names for package level declarations, so we can safely rely on real fq name for parent
    val parentFqName = KtNamedDeclarationUtil.getParentFqName(this)
    return parentFqName?.child(safeNameForLazyResolve())
}

/** Returns `true` if [element] is a top-level element of a file or a top-level statement of a script. */
fun isTopLevelInFileOrScript(element: PsiElement): Boolean {
    val parent = element.parent
    return when (parent) {
        is KtFile -> true
        is KtBlockExpression -> parent.parent is KtScript
        else -> false
    }
}

/** Returns the top-level declarations of this file, taking them from the script if the file is a script. */
fun KtFile.getFileOrScriptDeclarations() = if (isScript()) script!!.declarations else declarations

/**
 * Returns the enclosing `as`/`as?` (binary-with-type) expression that this expression is the operand of through a
 * possibly-qualified call, or `null`.
 */
fun KtExpression.getBinaryWithTypeParent(): KtBinaryExpressionWithTypeRHS? {
    val callExpression = parent as? KtCallExpression ?: return null
    val possibleQualifiedExpression = callExpression.parent

    val targetExpression = if (possibleQualifiedExpression is KtQualifiedExpression) {
        if (possibleQualifiedExpression.selectorExpression != callExpression) return null
        possibleQualifiedExpression
    } else {
        callExpression
    }

    return targetExpression.topParenthesizedParentOrMe().parent as? KtBinaryExpressionWithTypeRHS
}

/**
 * Returns the outermost enclosing expression that this expression is wrapped in only through parentheses, or this
 * expression itself.
 */
fun KtExpression.topParenthesizedParentOrMe(): KtExpression {
    var result: KtExpression = this
    while (KtPsiUtil.deparenthesizeOnce(result.parent as? KtExpression) == result) {
        result = result.parent as? KtExpression ?: break
    }
    return result
}

/** Returns the trailing comma immediately before [closingElement] (a closing bracket/parenthesis), or `null`. */
fun getTrailingCommaByClosingElement(closingElement: PsiElement?): PsiElement? {
    val elementBeforeClosingElement =
        closingElement?.getPrevSiblingIgnoringWhitespaceAndComments() ?: return null

    return elementBeforeClosingElement.run { if (node.elementType == KtTokens.COMMA) this else null }
}

/** Returns the trailing comma at the end of [elementList] (skipping a trailing comment), or `null` if there is none. */
fun getTrailingCommaByElementsList(elementList: PsiElement?): PsiElement? {
    val lastChild = elementList?.lastChild?.let { if (it !is PsiComment) it else it.getPrevSiblingIgnoringWhitespaceAndComments() }
    return lastChild?.takeIf { it.node.elementType == KtTokens.COMMA }
}

/** `true` if this reference is the backtick-escaped underscore identifier `` `_` `` (distinct from the plain `_` placeholder). */
val KtNameReferenceExpression.isUnderscoreInBackticks
    get() = getReferencedName() == "`_`"

/** Returns the innermost non-nullable type element by stripping any nesting of nullable types, or `null`. */
tailrec fun KtTypeElement.unwrapNullability(): KtTypeElement? {
    return when (this) {
        is KtNullableType -> this.innerType?.unwrapNullability()
        else -> this
    }
}

internal fun isKtFile(parent: PsiElement?): Boolean {
    // Avoid loading KtFile, which depends on Java PSI and is not available in some setups.
    // For example, remote dev: https://youtrack.jetbrains.com/issue/GTW-7554.
    return parent is PsiFile && parent.language == KotlinLanguage.INSTANCE
}

/** Returns the original short name imported under the alias [aliasName] in [file], or `null` if there is no such alias. */
fun getImportedSimpleNameByImportAlias(file: KtFile, aliasName: String): String? {
    val directive = file.findImportByAlias(aliasName) ?: return null

    var reference = directive.importedReference
    while (reference is KtDotQualifiedExpression) {
        reference = reference.selectorExpression
    }
    if (reference is KtSimpleNameExpression) {
        return reference.getReferencedName()
    }

    return null
}

/**
 * A best-effort way to get the [ClassId] of this expression's type without semantic resolution.
 */
fun KtConstantExpression.inferClassIdByPsi(): ClassId? =
    ClassIdCalculator.inferConstantExpressionClassIdByPsi(this)
