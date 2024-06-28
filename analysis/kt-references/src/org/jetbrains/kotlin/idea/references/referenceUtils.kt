/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

val KtSimpleNameExpression.mainReference: KtSimpleNameReference
    get() = references.firstIsInstance()

val KtReferenceExpression.mainReference: KtReference
    get() = if (this is KtSimpleNameExpression) mainReference else references.firstIsInstance()

val KDocName.mainReference: KDocReference
    get() = references.firstIsInstance()

val KtElement.mainReference: KtReference?
    get() = when (this) {
        is KtReferenceExpression -> mainReference
        is KDocName -> mainReference
        else -> references.firstIsInstanceOrNull()
    }

// Navigation element of the resolved reference
// For property accessor return enclosing property
val PsiReference.unwrappedTargets: Set<PsiElement>
    get() {
        fun PsiElement.adjust(): PsiElement? = when (val target = unwrapped?.originalElement) {
            is KtPropertyAccessor -> target.getNonStrictParentOfType<KtProperty>()
            else -> target
        }

        return when (this) {
            is PsiPolyVariantReference -> multiResolve(false).mapNotNullTo(HashSet()) { it.element?.adjust() }
            else -> listOfNotNull(resolve()?.adjust()).toSet()
        }
    }

fun KtFunction.getCalleeByLambdaArgument(): KtSimpleNameExpression? {
    val argument = getParentOfTypeAndBranch<KtValueArgument> { getArgumentExpression() } ?: return null
    val callExpression = when (argument) {
        is KtLambdaArgument -> argument.parent as? KtCallExpression
        else -> (argument.parent as? KtValueArgumentList)?.parent as? KtCallExpression
    } ?: return null
    return callExpression.calleeExpression as? KtSimpleNameExpression
}

fun PsiElement.isConstructorOf(unwrappedCandidate: PsiElement): Boolean =
    when {
        // call to Java constructor
        this is PsiMethod && isConstructor && containingClass == unwrappedCandidate -> true
        // call to Kotlin constructor
        this is KtConstructor<*> && getContainingClassOrObject().isEquivalentTo(unwrappedCandidate) -> true
        else -> false
    }