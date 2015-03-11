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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
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

public fun PsiReference.matchesTarget(candidateTarget: PsiElement): Boolean {
    val unwrappedCandidate = candidateTarget.unwrapped?.getOriginalElement() ?: return false
    val targets = unwrappedTargets
    if (unwrappedCandidate in targets) return true

    if (this is JetReference) {
        return targets.any {
            it is PsiMethod && it.isConstructor() && it.getContainingClass() == unwrappedCandidate
            || it is JetObjectDeclaration && it.isDefault() && it.getNonStrictParentOfType<JetClass>() == unwrappedCandidate
        }
    }
    if (this is PsiReferenceExpression && candidateTarget is JetObjectDeclaration && unwrappedTargets.size() == 1) {
        val referredClass = unwrappedTargets.first()
        if (referredClass is JetClass && candidateTarget in referredClass.getDefaultObjects()) {
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

fun AbstractJetReference<out JetExpression>.renameImplicitConventionalCall(newName: String?): JetExpression {
    if (newName == null) return expression

    val expr = OperatorToFunctionIntention.convert(expression) as JetQualifiedExpression
    val newCallee = (expr.getSelectorExpression() as JetCallExpression).getCalleeExpression()!!.getReference()!!.handleElementRename(newName)
    return newCallee.getStrictParentOfType<JetQualifiedExpression>() as JetExpression
}
