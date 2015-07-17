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

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.intentions.OperatorToFunctionIntention
import org.jetbrains.kotlin.idea.kdoc.KDocReference
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.emptyOrSingletonList
import java.util.HashSet

// Navigation element of the resolved reference
// For property accessor return enclosing property
public val PsiReference.unwrappedTargets: Set<PsiElement>
    get() {
        fun PsiElement.adjust(): PsiElement? {
            val target = unwrapped?.getOriginalElement()
            return when {
                target is JetPropertyAccessor -> target.getNonStrictParentOfType<JetProperty>()
                else -> target
            }
        }

        return when (this) {
            is PsiPolyVariantReference -> multiResolve(false).map { it.getElement()?.adjust() }.filterNotNullTo(HashSet<PsiElement>())
            else -> emptyOrSingletonList(resolve()?.adjust()).toSet()
        }
    }

//
public fun PsiReference.canBeReferenceTo(candidateTarget: PsiElement): Boolean {
    // optimization
    return getElement().getContainingFile() == candidateTarget.getContainingFile()
            || ProjectRootsUtil.isInProjectOrLibSource(getElement())
}

public fun PsiReference.matchesTarget(candidateTarget: PsiElement): Boolean {
    if (!canBeReferenceTo(candidateTarget)) return false

    val unwrappedCandidate = candidateTarget.unwrapped?.getOriginalElement() ?: return false

    // Optimizations
    when (this) {
        is JetInvokeFunctionReference -> {
            if (candidateTarget !is JetNamedFunction) return false
        }
        is JetMultiDeclarationReference -> {
            if (candidateTarget !is JetNamedFunction && candidateTarget !is JetParameter) return false
        }
    }

    val targets = unwrappedTargets
    if (unwrappedCandidate in targets) return true
    // TODO: Investigate why PsiCompiledElement identity changes
    if (unwrappedCandidate is PsiCompiledElement && targets.any { it.isEquivalentTo(unwrappedCandidate) }) return true

    if (this is JetReference) {
        return targets.any {
            it.isConstructorOf(unwrappedCandidate)
            || it is JetObjectDeclaration && it.isCompanion() && it.getNonStrictParentOfType<JetClass>() == unwrappedCandidate
        }
    }
    // TODO: Workaround for Kotlin constructor search in Java code. To be removed after refactoring of the search API
    else if (this is PsiJavaCodeReferenceElement && unwrappedCandidate is JetConstructor<*>) {
        var parent = getElement().getParent()
        if (parent is PsiAnonymousClass) {
            parent = parent.getParent()
        }
        if ((parent as? PsiNewExpression)?.resolveConstructor()?.unwrapped == unwrappedCandidate) return true
    }
    if (this is PsiReferenceExpression && candidateTarget is JetObjectDeclaration && unwrappedTargets.size() == 1) {
        val referredClass = unwrappedTargets.first()
        if (referredClass is JetClass && candidateTarget in referredClass.getCompanionObjects()) {
            val parentReference = getParent().getReference()
            if (parentReference != null) {
                return parentReference.unwrappedTargets.any {
                    (it is JetProperty || it is JetNamedFunction) && it.getParent()?.getParent() == candidateTarget
                }
            }
        }
    }
    return false
}

private fun PsiElement.isConstructorOf(unwrappedCandidate: PsiElement) =
    // call to Java constructor
    (this is PsiMethod && isConstructor() && getContainingClass() == unwrappedCandidate) ||
    // call to Kotlin constructor
    (this is JetConstructor<*> && getContainingClassOrObject() == unwrappedCandidate)

fun AbstractJetReference<out JetExpression>.renameImplicitConventionalCall(newName: String?): JetExpression {
    if (newName == null) return expression

    val (newExpression, newNameElement) = OperatorToFunctionIntention.convert(expression)
    newNameElement.mainReference.handleElementRename(newName)
    return newExpression
}

val JetSimpleNameExpression.mainReference: JetSimpleNameReference
    get() = getReferences().firstIsInstance()

val JetReferenceExpression.mainReference: JetReference
    get() = if (this is JetSimpleNameExpression) mainReference else getReferences().firstIsInstance<JetReference>()

val KDocName.mainReference: KDocReference
    get() = getReferences().firstIsInstance()

val JetElement.mainReference: JetReference?
    get() {
        return when {
            this is JetReferenceExpression -> mainReference
            this is KDocName -> mainReference
            else -> getReferences().firstIsInstanceOrNull<JetReference>()
        }
    }
