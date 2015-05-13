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

import org.jetbrains.kotlin.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.JetTokens
import java.util.Collections
import com.intellij.extapi.psi.StubBasedPsiElementBase
import java.util.ArrayList
import kotlin.test.assertTrue
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.PsiSearchScopeUtil
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPackage
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiDirectory
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.PsiComment
import org.jetbrains.kotlin.resolve.calls.CallTransformer.CallForImplicitInvoke
import com.intellij.openapi.util.TextRange
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

public fun JetCallElement.getCallNameExpression(): JetSimpleNameExpression? {
    val calleeExpression = getCalleeExpression() ?: return null

    return when (calleeExpression) {
        is JetSimpleNameExpression -> calleeExpression
        is JetConstructorCalleeExpression -> calleeExpression.getConstructorReferenceExpression()
        else -> null
    }
}

public fun PsiElement.getParentOfTypesAndPredicate<T: PsiElement>(
        strict : Boolean = false, vararg parentClasses : Class<T>, predicate: (T) -> Boolean
) : T? {
    var element = if (strict) getParent() else this
    while (element != null) {
        [suppress("UNCHECKED_CAST")]
        when {
            (parentClasses.isEmpty() || parentClasses.any {parentClass -> parentClass.isInstance(element)}) && predicate(element!! as T) ->
                return element as T
            element is PsiFile ->
                return null
            else ->
                element = element!!.getParent()
        }
    }

    return null
}

public fun PsiElement.getNonStrictParentOfType<T: PsiElement>(parentClass : Class<T>) : T? {
    return PsiTreeUtil.getParentOfType(this, parentClass, false)
}

inline public fun PsiElement.getParentOfType<reified T: PsiElement>(strict: Boolean): T? {
    return PsiTreeUtil.getParentOfType(this, javaClass<T>(), strict)
}

inline public fun PsiElement.getStrictParentOfType<reified T: PsiElement>(): T? {
    return PsiTreeUtil.getParentOfType(this, javaClass<T>(), true)
}

inline public fun PsiElement.getNonStrictParentOfType<reified T: PsiElement>(): T? {
    return PsiTreeUtil.getParentOfType(this, javaClass<T>(), false)
}

inline public fun PsiElement.getChildOfType<reified T: PsiElement>(): T? {
    return PsiTreeUtil.getChildOfType(this, javaClass<T>())
}

inline public fun PsiElement.getChildrenOfType<reified T: PsiElement>(): Array<T> {
    return PsiTreeUtil.getChildrenOfType(this, javaClass<T>()) ?: array()
}

public fun PsiElement?.isAncestor(element: PsiElement, strict: Boolean = false): Boolean {
    return PsiTreeUtil.isAncestor(this, element, strict)
}

public fun <T: PsiElement> T.getIfChildIsInBranch(element: PsiElement, branch: T.() -> PsiElement?): T? {
    return if (branch().isAncestor(element)) this else null
}

inline public fun PsiElement.getParentOfTypeAndBranch<reified T: PsiElement>(strict: Boolean = false, noinline branch: T.() -> PsiElement?) : T? {
    return getParentOfType<T>(strict)?.getIfChildIsInBranch(this, branch)
}

public fun JetClassOrObject.effectiveDeclarations(): List<JetDeclaration> =
        when(this) {
            is JetClass ->
                getDeclarations() + getPrimaryConstructorParameters().filter { p -> p.hasValOrVarNode() }
            else ->
                getDeclarations()
        }

public fun JetClass.isAbstract(): Boolean = isInterface() || hasModifier(JetTokens.ABSTRACT_KEYWORD)

[suppress("UNCHECKED_CAST")]
public inline fun <reified T: PsiElement> PsiElement.replaced(newElement: T): T {
    val result = replace(newElement)
    return if (result is T)
        result
    else
        (result as JetParenthesizedExpression).getExpression() as T
}

[suppress("UNCHECKED_CAST")]
public fun <T: PsiElement> T.copied(): T = copy() as T

public fun JetElement.blockExpressionsOrSingle(): Sequence<JetElement> =
        if (this is JetBlockExpression) getStatements().asSequence() else sequenceOf(this)

public fun JetExpression.lastBlockStatementOrThis(): JetExpression
        = (this as? JetBlockExpression)?.getStatements()?.lastIsInstanceOrNull<JetExpression>() ?: this

public fun JetBlockExpression.appendElement(element: JetElement): JetElement {
    val rBrace = getRBrace()
    val anchor = if (rBrace == null) {
        val lastChild = getLastChild()
        if (lastChild !is PsiWhiteSpace) addAfter(JetPsiFactory(this).createNewLine(), lastChild)!! else lastChild
    }
    else {
        rBrace.getPrevSibling()!!
    }
    return addAfter(element, anchor)!! as JetElement
}

public fun JetElement.wrapInBlock(): JetBlockExpression {
    val block = JetPsiFactory(this).createEmptyBody()
    block.appendElement(this)
    return block
}

/**
 * Returns the list of unqualified names that are indexed as the superclass names of this class. For the names that might be imported
 * via an aliased import, includes both the original and the aliased name (reference resolution during inheritor search will sort this out).
 *
 * @return the list of possible superclass names
 */
public fun <T: JetClassOrObject> StubBasedPsiElementBase<out KotlinClassOrObjectStub<T>>.getSuperNames(): List<String> {
    fun addSuperName(result: MutableList<String>, referencedName: String): Unit {
        result.add(referencedName)

        val file = getContainingFile()
        if (file is JetFile) {
            val directive = file.findImportByAlias(referencedName)
            if (directive != null) {
                var reference = directive.getImportedReference()
                while (reference is JetDotQualifiedExpression) {
                    reference = (reference as JetDotQualifiedExpression).getSelectorExpression()
                }
                if (reference is JetSimpleNameExpression) {
                    result.add((reference as JetSimpleNameExpression).getReferencedName())
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

public fun SearchScope.contains(element: PsiElement): Boolean = PsiSearchScopeUtil.isInScope(this, element)

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

public fun PsiElement.isExtensionDeclaration(): Boolean {
    val callable: JetCallableDeclaration? = when (this) {
        is JetNamedFunction, is JetProperty -> this as JetCallableDeclaration
        is JetPropertyAccessor -> getNonStrictParentOfType<JetProperty>()
        else -> null
    }

    return callable?.getReceiverTypeReference() != null
}

public fun PsiElement.isObjectLiteral(): Boolean = this is JetObjectDeclaration && isObjectLiteral()

public fun PsiElement.deleteElementAndCleanParent() {
    val parent = getParent()

    JetPsiUtil.deleteElementWithDelimiters(this)
    [suppress("UNCHECKED_CAST")]
    JetPsiUtil.deleteChildlessElement(parent, this.javaClass)
}

public fun PsiElement.parameterIndex(): Int {
    val parent = getParent()
    return when {
        this is JetParameter && parent is JetParameterList -> parent.getParameters().indexOf(this)
        this is PsiParameter && parent is PsiParameterList -> parent.getParameterIndex(this)
        else -> -1
    }
}

/**
 * Returns enclosing qualifying element for given [[JetSimpleNameExpression]]
 * ([[JetQualifiedExpression]] or [[JetUserType]] or original expression)
 */
public fun JetSimpleNameExpression.getQualifiedElement(): JetElement {
    val baseExpression: JetElement = (getParent() as? JetCallExpression) ?: this
    val parent = baseExpression.getParent()
    return when (parent) {
        is JetQualifiedExpression -> if (parent.getSelectorExpression().isAncestor(baseExpression)) parent else baseExpression
        is JetUserType -> if (parent.getReferenceExpression().isAncestor(baseExpression)) parent else baseExpression
        else -> baseExpression
    }
}

public fun JetSimpleNameExpression.getTopmostParentQualifiedExpressionForSelector(): JetQualifiedExpression? {
    return stream<JetExpression>(this) {
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

/**
 * Returns outermost qualified element ([[JetQualifiedExpression]] or [[JetUserType]]) in the non-interleaving chain
 * of qualified elements which enclose given expression
 * If there is no such elements original expression is returned
 */
public fun JetSimpleNameExpression.getOutermostNonInterleavingQualifiedElement(): JetElement {
    var element = ((getParent() as? JetCallExpression) ?: this).getParent()
    if (element !is JetQualifiedExpression && element !is JetUserType) return this

    while (true) {
        val parent = element!!.getParent()
        if (parent !is JetQualifiedExpression && parent !is JetUserType) return element as JetElement
        element = parent
    }
}

public fun PsiDirectory.getPackage(): PsiPackage? = JavaDirectoryService.getInstance()!!.getPackage(this)

public fun JetModifierListOwner.isPrivate(): Boolean = hasModifier(JetTokens.PRIVATE_KEYWORD)

public fun PsiElement.isInsideOf(elements: Iterable<PsiElement>): Boolean = elements.any { it.isAncestor(this) }

public tailRecursive fun PsiElement.getOutermostParentContainedIn(container: PsiElement): PsiElement? {
    val parent = getParent()
    return if (parent == container) this else parent?.getOutermostParentContainedIn(container)
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
            val callExpression = (parent as JetCallExpression)
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

public fun JetSimpleNameExpression.isImportDirectiveExpression(): Boolean {
    val parent = getParent()
    if (parent == null) {
        return false
    }
    else {
        return parent is JetImportDirective || parent.getParent() is JetImportDirective
    }
}

public fun JetElement.getTextWithLocation(): String = "'${this.getText()}' at ${DiagnosticUtils.atLocation(this)}"

public fun JetExpression.isFunctionLiteralOutsideParentheses(): Boolean {
    val parent = getParent()
    return when (parent) {
        is JetFunctionLiteralArgument -> true
        is JetLabeledExpression -> parent.isFunctionLiteralOutsideParentheses()
        else -> false
    }
}

public fun PsiElement.siblings(forward: Boolean = true, withItself: Boolean = true): Sequence<PsiElement> {
    val stepFun = if (forward) { e: PsiElement -> e.getNextSibling() } else { e: PsiElement -> e.getPrevSibling() }
    val sequence = sequence(this, stepFun)
    return if (withItself) sequence else sequence.drop(1)
}

public fun ASTNode.siblings(forward: Boolean = true, withItself: Boolean = true): Sequence<ASTNode> {
    val stepFun = if (forward) { node: ASTNode -> node.getTreeNext() } else { e: ASTNode -> e.getTreeNext() }
    val sequence = sequence(this, stepFun)
    return if (withItself) sequence else sequence.drop(1)
}

public fun PsiElement.parents(withItself: Boolean = true): Sequence<PsiElement> {
    val sequence = sequence(this) { if (it is PsiFile) null else it.getParent() }
    return if (withItself) sequence else sequence.drop(1)
}

public fun ASTNode.parents(withItself: Boolean = true): Sequence<ASTNode> {
    val sequence = sequence(this) { it.getTreeParent() }
    return if (withItself) sequence else sequence.drop(1)
}

public fun JetExpression.getAssignmentByLHS(): JetBinaryExpression? {
    val parent = getParent() as? JetBinaryExpression ?: return null
    return if (JetPsiUtil.isAssignment(parent) && parent.getLeft() == this) parent else null
}

public fun PsiElement.prevLeaf(skipEmptyElements: Boolean = false): PsiElement?
        = PsiTreeUtil.prevLeaf(this, skipEmptyElements)

public fun PsiElement.nextLeaf(skipEmptyElements: Boolean = false): PsiElement?
        = PsiTreeUtil.nextLeaf(this, skipEmptyElements)

public fun PsiElement.prevLeafSkipWhitespacesAndComments(): PsiElement? {
    var leaf = prevLeaf()
    while (leaf is PsiWhiteSpace || leaf is PsiComment) {
        leaf = leaf!!.prevLeaf()
    }
    return leaf
}

public fun PsiElement.prevLeafSkipWhitespaces(): PsiElement? {
    var leaf = prevLeaf()
    while (leaf is PsiWhiteSpace) {
        leaf = leaf!!.prevLeaf()
    }
    return leaf
}

public fun PsiElement.nextLeafSkipWhitespacesAndComments(): PsiElement? {
    var leaf = nextLeaf()
    while (leaf is PsiWhiteSpace || leaf is PsiComment) {
        leaf = leaf!!.nextLeaf()
    }
    return leaf
}

public fun JetExpression.isDotReceiver(): Boolean =
        (getParent() as? JetDotQualifiedExpression)?.getReceiverExpression() == this

public fun Call.isSafeCall(): Boolean {
    if (this is CallForImplicitInvoke) {
        //implicit safe 'invoke'
        if (getOuterCall().isExplicitSafeCall()) {
            return true
        }
    }
    return isExplicitSafeCall()
}

public fun Call.isExplicitSafeCall(): Boolean = getCallOperationNode()?.getElementType() == JetTokens.SAFE_ACCESS

public fun JetStringTemplateExpression.getContentRange(): TextRange {
    val start = getNode().getFirstChildNode().getTextLength()
    val lastChild = getNode().getLastChildNode()
    val length = getTextLength()
    return TextRange(start, if (lastChild.getElementType() == JetTokens.CLOSING_QUOTE) length - lastChild.getTextLength() else length)
}

public fun JetStringTemplateExpression.isSingleQuoted(): Boolean
        = getNode().getFirstChildNode().getTextLength() == 1

public fun PsiFile.elementsInRange(range: TextRange): List<PsiElement> {
    var offset = range.getStartOffset()
    val result = ArrayList<PsiElement>()
    while (offset < range.getEndOffset()) {
        val currentRange = TextRange(offset, range.getEndOffset())
        val leaf = findFirstLeafWhollyInRange(this, currentRange) ?: break

        val element = leaf
                .parents(withItself = true)
                .first {
                    val parent = it.getParent()
                    it is PsiFile || parent.getTextRange() !in currentRange
                }
        result.add(element)

        offset = element.endOffset
    }
    return result
}

private fun findFirstLeafWhollyInRange(file: PsiFile, range: TextRange): PsiElement? {
    var element = file.findElementAt(range.getStartOffset()) ?: return null
    var elementRange = element.getTextRange()
    if (elementRange.getStartOffset() < range.getStartOffset()) {
        element = element.nextLeaf(skipEmptyElements = true) ?: return null
        elementRange = element.getTextRange()
    }
    assert(elementRange.getStartOffset() >= range.getStartOffset())
    return if (elementRange.getEndOffset() <= range.getEndOffset()) element else null
}

fun JetNamedDeclaration.getValueParameters(): List<JetParameter> {
    return getValueParameterList()?.getParameters() ?: Collections.emptyList()
}

fun JetNamedDeclaration.getValueParameterList(): JetParameterList? {
    return when (this) {
        is JetCallableDeclaration -> getValueParameterList()
        is JetClass -> getPrimaryConstructorParameterList()
        else -> null
    }
}

public fun PsiElement.getElementTextWithContext(): String {
    if (this is PsiFile) {
        return getContainingFile().getText()
    }

    // Find parent for element among file children
    val topLevelElement = PsiTreeUtil.findFirstParent(this, { it.getParent() is PsiFile }) ?:
        throw AssertionError("For non-file element we should always be able to find parent in file children")

    val startContextOffset = topLevelElement.startOffset
    val elementContextOffset = getTextRange().getStartOffset()

    val inFileParentOffset = elementContextOffset - startContextOffset

    return StringBuilder(topLevelElement.getText())
            .insert(inFileParentOffset, "<caret>")
            .insert(0, "File name: ${getContainingFile().getName()}\n")
            .toString()
}

// Calls `block` on each descendant of T type
// Note, that calls happen in order of DFS-exit, so deeper nodes are applied earlier
inline fun <reified T : JetElement> forEachDescendantOfTypeVisitor(
    noinline block: (T) -> Unit
): JetVisitorVoid =
        object : JetTreeVisitorVoid() {
            override fun visitJetElement(element: JetElement) {
                super.visitJetElement(element)
                if (element is T) {
                    block(element)
                }
            }
        }

inline fun <reified T : JetElement, R> flatMapDescendantsOfTypeVisitor(
    accumulator: MutableCollection<R>,
    noinline map: (T) -> Collection<R>
): JetVisitorVoid = forEachDescendantOfTypeVisitor<T> { accumulator.addAll(map(it)) }

inline fun <reified T : JetElement> PsiElement.forEachDescendantsOfType(noinline block: (T) -> Unit) =
        accept(forEachDescendantOfTypeVisitor(block))

public fun PsiFile.getFqNameByDirectory(): FqName {
    val qualifiedNameByDirectory = getParent()?.getPackage()?.getQualifiedName()
    return qualifiedNameByDirectory?.let { FqName(it) } ?: FqName.ROOT
}

public fun JetFile.packageMatchesDirectory(): Boolean = getPackageFqName() == getFqNameByDirectory()

public fun JetAnnotationsContainer.collectAnnotationEntriesFromStubOrPsi(): List<JetAnnotationEntry> =
    when (this) {
        is StubBasedPsiElementBase<*> -> getStub()?.collectAnnotationEntriesFromStubElement() ?: collectAnnotationEntriesFromPsi()
        else -> collectAnnotationEntriesFromPsi()
    }

private fun StubElement<*>.collectAnnotationEntriesFromStubElement() =
    getChildrenStubs().flatMap {
        child ->
        when (child.getStubType()) {
            JetNodeTypes.ANNOTATION_ENTRY -> listOf(child.getPsi() as JetAnnotationEntry)
            JetNodeTypes.ANNOTATION -> (child.getPsi() as JetAnnotation).getEntries()
            else -> emptyList<JetAnnotationEntry>()
        }
    }

private fun JetAnnotationsContainer.collectAnnotationEntriesFromPsi() =
    getChildren().flatMap {
        child ->
        when (child) {
            is JetAnnotationEntry -> listOf(child)
            is JetAnnotation -> child.getEntries()
            else -> emptyList<JetAnnotationEntry>()
        }
    }

public fun JetElement.getCalleeHighlightingRange(): TextRange {
    val annotationEntry: JetAnnotationEntry =
            PsiTreeUtil.getParentOfType<JetAnnotationEntry>(
                    this, javaClass<JetAnnotationEntry>(), /* strict = */false, javaClass<JetValueArgumentList>()
            ) ?: return getTextRange()

    val startOffset = annotationEntry.getAtSymbol()?.getTextRange()?.getStartOffset()
                      ?: annotationEntry.getCalleeExpression().startOffset

    return TextRange(startOffset, annotationEntry.getCalleeExpression().endOffset)
}

public val PsiElement.startOffset: Int
    get() = getTextRange().getStartOffset()

public val PsiElement.endOffset: Int
    get() = getTextRange().getEndOffset()
