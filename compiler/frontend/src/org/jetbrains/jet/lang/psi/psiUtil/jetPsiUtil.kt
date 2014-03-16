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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.lexer.JetTokens
import java.util.Collections
import com.intellij.extapi.psi.StubBasedPsiElementBase
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassOrObjectStub
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import java.util.ArrayList
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.psi.JetPsiFactory
import kotlin.test.assertTrue
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.PsiSearchScopeUtil
import org.jetbrains.jet.lang.psi.JetClassBody
import org.jetbrains.jet.lang.psi.JetParameterList
import org.jetbrains.jet.lang.psi.JetObjectDeclaration
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetCallableDeclaration
import org.jetbrains.jet.lang.psi.JetPropertyAccessor
import org.jetbrains.jet.lang.psi.JetParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiParameter
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.psi.JetUserType
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.psi.JetCallExpression
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiDirectory

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
                getDeclarations() + getPrimaryConstructorParameters().filter { p -> p.getValOrVarNode() != null }
            else ->
                getDeclarations()
        }

public fun JetClass.isAbstract(): Boolean = isTrait() || hasModifier(JetTokens.ABSTRACT_KEYWORD)

[suppress("UNCHECKED_CAST")]
public fun <T: PsiElement> PsiElement.replaced(newElement: T): T = replace(newElement) as T

public fun JetElement.blockExpressionsOrSingle(): Iterator<JetElement> =
        if (this is JetBlockExpression) getStatements().iterator() else SingleIterator(this)

public fun JetElement.outermostLastBlockElement(predicate: (JetElement) -> Boolean = { true }): JetElement? {
    return JetPsiUtil.getOutermostLastBlockElement(this) { e -> e != null && predicate(e) }
}

public fun JetBlockExpression.appendElement(element: JetElement): JetElement =
        addAfter(element, getRBrace()!!.getPrevSibling()!!)!! as JetElement

public fun JetElement.wrapInBlock(): JetBlockExpression {
    val block = JetPsiFactory.createEmptyBody(getProject()) as JetBlockExpression
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
    JetPsiUtil.deleteChildlessElement(parent, this.getClass() as Class<PsiElement>)
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
    val baseExpression = (getParent() as? JetCallExpression) ?: this
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

/**
 * Returns FqName for given declaration (either Java or Kotlin)
 */
public fun PsiElement.getFqName(): FqName? {
    return when (this) {
        is PsiPackage -> FqName(getQualifiedName())
        is PsiClass -> getQualifiedName()?.let { FqName(it) }
        is PsiMember -> getName()?.let { name ->
            val prefix = getContainingClass()?.getQualifiedName()
            FqName(if (prefix != null) "$prefix.$name" else name)
        }
        is JetNamedDeclaration -> JetPsiUtil.getFQName(this)
        else -> null
    }
}

public fun PsiDirectory.getPackage(): PsiPackage? = JavaDirectoryService.getInstance()!!.getPackage(this)