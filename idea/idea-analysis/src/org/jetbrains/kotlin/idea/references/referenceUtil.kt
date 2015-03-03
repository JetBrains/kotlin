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

import com.intellij.psi.PsiReference
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.unwrapped
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.JetPropertyAccessor
import org.jetbrains.kotlin.psi.JetProperty
import java.util.HashSet
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.idea.intentions.OperatorToFunctionIntention
import org.jetbrains.kotlin.psi.JetQualifiedExpression
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetObjectDeclaration
import org.jetbrains.kotlin.psi.JetClass
import com.intellij.psi.PsiPolyVariantReference
import org.jetbrains.kotlin.utils.emptyOrSingletonList
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

// Navigation element of the resolved reference
// For property accessor return enclosing property
// For default object return enclosing class
public val PsiReference.unwrappedTargets: Set<PsiElement>
    get() {
        fun PsiElement.adjust(): PsiElement? {
            val target = unwrapped?.getOriginalElement()
            return when {
                target is JetPropertyAccessor -> target.getNonStrictParentOfType<JetProperty>()
                target is JetObjectDeclaration && target.isDefault() -> target.getNonStrictParentOfType<JetClass>()
                else -> target
            }
        }

        return when (this) {
            is PsiPolyVariantReference -> multiResolve(false).map { it.getElement()?.adjust() }.filterNotNullTo(HashSet<PsiElement>())
            else -> emptyOrSingletonList(resolve()?.adjust()).toSet()
        }
    }

public fun PsiReference.matchesTarget(target: PsiElement): Boolean {
    val unwrapped = target.unwrapped?.getOriginalElement()
    return when {
        unwrapped in unwrappedTargets ->
            true

        this is JetReference
                && unwrappedTargets.any { it is PsiMethod && it.isConstructor() && it.getContainingClass() == unwrapped } ->
            true

        else ->
            false
    }
}

fun AbstractJetReference<out JetExpression>.renameImplicitConventionalCall(newName: String?): JetExpression {
    if (newName == null) return expression

    val expr = OperatorToFunctionIntention.convert(expression) as JetQualifiedExpression
    val newCallee = (expr.getSelectorExpression() as JetCallExpression).getCalleeExpression()!!.getReference()!!.handleElementRename(newName)
    return newCallee.getStrictParentOfType<JetQualifiedExpression>() as JetExpression
}
