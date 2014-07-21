/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi.psiUtil

import org.jetbrains.jet.lang.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lexer.JetTokens
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
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassOrObjectStub
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils

public fun JetCallElement.getCallNameExpression(): JetSimpleNameExpression? {
    val calleeExpression = getCalleeExpression()
    if (calleeExpression == null) return null

    return when (calleeExpression) {
        is JetSimpleNameExpression -> calleeExpression
        is JetConstructorCalleeExpression -> calleeExpression.getConstructorReferenceExpression() as? JetSimpleNameExpression
        else -> null
    }
}

public fun PsiElement.getParentByTypesAndPredicate<T: PsiElement>(
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

public fun PsiElement.getParentByType<T: PsiElement>(parentClass : Class<T>, strict : Boolean = false) : T? {
    return PsiTreeUtil.getParentOfType(this, parentClass, strict)
}

public fun PsiElement?.isAncestor(element: PsiElement, strict: Boolean = false): Boolean {
    return PsiTreeUtil.isAncestor(this, element, strict)
}

public fun <T: PsiElement> T.getIfChildIsInBranch(element: PsiElement, branch: T.() -> PsiElement?): T? {
    return if (branch().isAncestor(element)) this else null
}

public fun PsiElement.getParentByTypeAndBranch<T: PsiElement>(
        parentClass : Class<T>, strict : Boolean = false, branch: T.() -> PsiElement?) : T? {
    return getParentByType(parentClass, strict)?.getIfChildIsInBranch(this, branch)
}

public fun JetClassOrObject.effectiveDeclarations(): List<JetDeclaration> =
        when(this) {
            is JetClass ->
                getDeclarations() + getPrimaryConstructorParameters().filter { p -> p.hasValOrVarNode() }
            else ->
                getDeclarations()
        }

public fun JetClass.isAbstract(): Boolean = isTrait() || hasModifier(JetTokens.ABSTRACT_KEYWORD)

[suppress("UNCHECKED_CAST")]
public fun <T: PsiElement> PsiElement.replaced(newElement: T): T = replace(newElement) as T

[suppress("UNCHECKED_CAST")]
public fun <T: PsiElement> T.copied(): T = copy() as T

public fun JetElement.blockExpressionsOrSingle(): Stream<JetElement> =
        if (this is JetBlockExpression) getStatements().stream() else listOf(this).stream()

public fun JetElement.outermostLastBlockElement(predicate: (JetElement) -> Boolean = { true }): JetElement? {
    return JetPsiUtil.getOutermostLastBlockElement(this) { e -> e != null && predicate(e) }
}

public fun JetBlockExpression.appendElement(element: JetElement): JetElement =
        addAfter(element, getRBrace()!!.getPrevSibling()!!)!! as JetElement

public fun JetBlockExpression.prependElement(element: JetElement): JetElement =
        addBefore(element, getLBrace()!!.getNextSibling()!!)!! as JetElement

public fun JetElement.wrapInBlock(): JetBlockExpression {
    val block = JetPsiFactory(this).createEmptyBody() as JetBlockExpression
    block.appendElement(this)
    return block
}

/**
 * Returns the list of unqualified names that are indexed as the superclass names of this class. For the names that might be imported
 * via an aliased import, includes both the original and the aliased name (reference resolution during inheritor search will sort this out).
 *
 * @return the list of possible superclass names
 */
public fun <T: JetClassOrObject> StubBasedPsiElementBase<out PsiJetClassOrObjectStub<T>>.getSuperNames(): List<String> {
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
    return isTrait() || hasModifier(JetTokens.OPEN_KEYWORD)
}

public fun JetDeclaration.isOverridable(): Boolean {
    val parent = getParent()
    if (!(parent is JetClassBody || parent is JetParameterList)) return false

    val klass = parent.getParent()
    if (!(klass is JetClass && klass.isInheritable())) return false

    if (hasModifier(JetTokens.FINAL_KEYWORD) || hasModifier(JetTokens.PRIVATE_KEYWORD)) return false

    return klass.isTrait() ||
        hasModifier(JetTokens.ABSTRACT_KEYWORD) || hasModifier(JetTokens.OPEN_KEYWORD) || hasModifier(JetTokens.OVERRIDE_KEYWORD)
}

public fun PsiElement.isExtensionDeclaration(): Boolean {
    val callable: JetCallableDeclaration? = when (this) {
        is JetNamedFunction, is JetProperty -> this as JetCallableDeclaration
        is JetPropertyAccessor -> getParentByType(javaClass<JetProperty>())
        else -> null
    }

    return callable?.getReceiverTypeRef() != null
}

public fun PsiElement.isObjectLiteral(): Boolean = this is JetObjectDeclaration && isObjectLiteral()

public fun PsiElement.deleteElementAndCleanParent() {
    val parent = getParent()

    JetPsiUtil.deleteElementWithDelimiters(this)
    [suppress("UNCHECKED_CAST")]
    JetPsiUtil.deleteChildlessElement(parent, this.javaClass as Class<PsiElement>)
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

/**
 * Returns rightmost selector of the qualified element (null if there is no such selector)
 */
public fun JetElement.getQualifiedElementSelector(): JetElement? {
    return when (this) {
        is JetSimpleNameExpression -> this
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

public fun JetElement.getCalleeExpressionIfAny(): JetExpression? {
    val element = if (this is JetExpression) JetPsiUtil.safeDeparenthesize(this, false) else this
    return when (element) {
        is JetSimpleNameExpression -> element
        is JetCallElement -> element.getCalleeExpression()
        is JetQualifiedExpression -> element.getSelectorExpression()?.getCalleeExpressionIfAny()
        is JetOperationExpression -> element.getOperationReference()
        else -> null
    }
}

public fun JetElement.getTextWithLocation(): String = "'${this.getText()}' at ${DiagnosticUtils.atLocation(this)}"
