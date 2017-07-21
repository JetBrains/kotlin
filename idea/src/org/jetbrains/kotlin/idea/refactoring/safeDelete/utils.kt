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

import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.search.searches.OverridingMethodsSearch
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.formatJavaOrLightMethod
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parameterIndex
import java.util.*

fun PsiElement.canDeleteElement(): Boolean {
    if (this is KtObjectDeclaration && isObjectLiteral()) return false

    if (this is KtParameter) {
        val parameterList = parent as? KtParameterList ?: return false
        val declaration = parameterList.parent as? KtDeclaration ?: return false
        return declaration !is KtPropertyAccessor
    }

    return this is KtClassOrObject
        || this is KtSecondaryConstructor
        || this is KtNamedFunction
        || this is PsiMethod
        || this is PsiClass
        || this is KtProperty
        || this is KtTypeParameter
        || this is KtTypeAlias
}

fun PsiElement.removeOverrideModifier() {
    when (this) {
        is KtNamedFunction, is KtProperty -> {
            (this as KtModifierListOwner).modifierList?.getModifier(KtTokens.OVERRIDE_KEYWORD)?.delete()
        }
        is PsiMethod -> {
            modifierList.annotations.firstOrNull {
                annotation -> annotation.qualifiedName == "java.lang.Override"
            }?.delete()
        }
    }
}

fun PsiMethod.cleanUpOverrides() {
    val superMethods = findSuperMethods(true)
    for (overridingMethod in OverridingMethodsSearch.search(this, true).findAll()) {
        val currentSuperMethods = overridingMethod.findSuperMethods(true).asSequence() + superMethods.asSequence()
        if (currentSuperMethods.all { superMethod -> superMethod.unwrapped == unwrapped }) {
            overridingMethod.unwrapped?.removeOverrideModifier()
        }
    }
}

fun checkParametersInMethodHierarchy(parameter: PsiParameter): Collection<PsiElement>? {
    val method = parameter.declarationScope as PsiMethod

    val parametersToDelete = collectParametersHierarchy(method, parameter)
    if (parametersToDelete.size <= 1 || ApplicationManager.getApplication().isUnitTestMode) return parametersToDelete

    val message = KotlinBundle.message("delete.param.in.method.hierarchy", formatJavaOrLightMethod(method))
    val exitCode = Messages.showOkCancelDialog(parameter.project, message, IdeBundle.message("title.warning"), Messages.getQuestionIcon())
    return if (exitCode == Messages.OK) parametersToDelete else null
}

// TODO: generalize breadth-first search
private fun collectParametersHierarchy(method: PsiMethod, parameter: PsiParameter): Set<PsiElement> {
    val queue = ArrayDeque<PsiMethod>()
    val visited = HashSet<PsiMethod>()
    val parametersToDelete = HashSet<PsiElement>()

    queue.add(method)
    while (!queue.isEmpty()) {
        val currentMethod = queue.poll()

        visited += currentMethod
        addParameter(currentMethod, parametersToDelete, parameter)

        currentMethod.findSuperMethods(true)
                .filter { it !in visited }
                .forEach { queue.offer(it) }
        OverridingMethodsSearch.search(currentMethod)
                .filter { it !in visited }
                .forEach { queue.offer(it) }
    }
    return parametersToDelete
}

private fun addParameter(method: PsiMethod, result: MutableSet<PsiElement>, parameter: PsiParameter) {
    val parameterIndex = parameter.unwrapped!!.parameterIndex()

    if (method is KtLightMethod) {
        val declaration = method.kotlinOrigin
        if (declaration is KtFunction) {
            result.add(declaration.valueParameters[parameterIndex])
        }
    }
    else {
        result.add(method.parameterList.parameters[parameterIndex])
    }
}