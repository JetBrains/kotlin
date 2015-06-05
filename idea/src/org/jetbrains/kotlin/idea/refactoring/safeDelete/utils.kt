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

package org.jetbrains.kotlin.idea.refactoring.safeDelete

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.OverridingMethodsSearch
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

public fun PsiElement.canDeleteElement(): Boolean {
    if (this is JetObjectDeclaration && isObjectLiteral()) return false

    if (this is JetParameter) {
        val parameterList = getParent() as? JetParameterList ?: return false
        val declaration = parameterList.getParent() as? JetDeclaration ?: return false
        return declaration !is JetPropertyAccessor
    }

    return this is JetClassOrObject
        || this is JetSecondaryConstructor
        || this is JetNamedFunction
        || this is PsiMethod
        || this is JetProperty
        || this is JetTypeParameter
}

fun PsiElement.removeOverrideModifier() {
    when (this) {
        is JetNamedFunction, is JetProperty -> {
            (this as JetModifierListOwner).getModifierList()?.getModifier(JetTokens.OVERRIDE_KEYWORD)?.delete()
        }
        is PsiMethod -> {
            getModifierList().getAnnotations().firstOrNull {
                annotation -> annotation.getQualifiedName() == "java.lang.Override"
            }?.delete()
        }
    }
}

fun PsiMethod.cleanUpOverrides() {
    val superMethods = findSuperMethods(true)
    for (overridingMethod in OverridingMethodsSearch.search(this, true).findAll()) {
        val currentSuperMethods = overridingMethod.findSuperMethods(true).stream() + superMethods.stream()
        if (currentSuperMethods.all { superMethod -> superMethod.unwrapped == unwrapped }) {
            overridingMethod.unwrapped?.removeOverrideModifier()
        }
    }
}
