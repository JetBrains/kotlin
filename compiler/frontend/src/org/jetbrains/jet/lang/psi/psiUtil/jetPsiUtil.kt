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

fun PsiElement.getParentByTypesAndPredicate<T: PsiElement>(
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

fun PsiElement.getParentByType<T: PsiElement>(parentClass : Class<T>, strict : Boolean = false) : T? {
    return PsiTreeUtil.getParentOfType(this, parentClass, strict)
}

fun PsiElement?.isAncestor(element: PsiElement, strict: Boolean = false): Boolean {
    return PsiTreeUtil.isAncestor(this, element, strict)
}

fun <T: PsiElement> T.getIfChildIsInBranch(element: PsiElement, branch: T.() -> PsiElement?): T? {
    return if (branch().isAncestor(element)) this else null
}

fun PsiElement.getParentByTypeAndBranch<T: PsiElement>(
        parentClass : Class<T>, strict : Boolean = false, branch: T.() -> PsiElement?) : T? {
    return getParentByType(parentClass, strict)?.getIfChildIsInBranch(this, branch)
}

fun JetClassOrObject.effectiveDeclarations(): List<JetDeclaration> =
        when(this) {
            is JetClass ->
                getDeclarations() + getPrimaryConstructorParameters().filter { p -> p.getValOrVarNode() != null }
            else ->
                getDeclarations()
        }

fun JetClass.isAbstract() = isTrait() || hasModifier(JetTokens.ABSTRACT_KEYWORD)

[suppress("UNCHECKED_CAST")]
fun <T: PsiElement> PsiElement.replaced(newElement: T): T = replace(newElement)!! as T

fun JetElement.blockExpressionsOrSingle(): Iterator<JetElement> =
        if (this is JetBlockExpression) getStatements().iterator() else SingleIterator(this)

fun JetElement.outermostLastBlockElement(predicate: (JetElement) -> Boolean = { true }): JetElement? {
    return JetPsiUtil.getOutermostLastBlockElement(this) { e -> e != null && predicate(e) }
}

fun JetBlockExpression.appendElement(element: JetElement): JetElement =
        addAfter(element, getRBrace()!!.getPrevSibling()!!)!! as JetElement

fun JetElement.wrapInBlock(): JetBlockExpression {
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
fun <T: JetClassOrObject> StubBasedPsiElementBase<out PsiJetClassOrObjectStub<T>>.getSuperNames(): List<String> {
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

fun SearchScope.contains(element: PsiElement): Boolean = PsiSearchScopeUtil.isInScope(this, element)

fun JetClass.isInheritable(): Boolean {
    return isTrait() || hasModifier(JetTokens.OPEN_KEYWORD)
}