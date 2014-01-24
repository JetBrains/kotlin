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

package org.jetbrains.jet.plugin.findUsages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.plugin.findUsages.handlers.KotlinFindClassUsagesHandler
import org.jetbrains.jet.plugin.findUsages.handlers.KotlinFindMemberUsagesHandler
import org.jetbrains.jet.plugin.refactoring.JetRefactoringUtil
import org.jetbrains.jet.lang.psi.JetProperty
import com.intellij.find.findUsages.FindUsagesOptions
import org.jetbrains.jet.lang.psi.JetTypeParameter
import org.jetbrains.jet.plugin.findUsages.handlers.KotlinTypeParameterFindUsagesHandler
import org.jetbrains.jet.lang.psi.JetParameter
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import org.jetbrains.jet.lang.psi.JetClassOrObject
import com.intellij.find.findUsages.FindUsagesManager
import com.intellij.find.findUsages.JavaFindUsagesHandlerFactory
import com.intellij.psi.PsiMethod

public class KotlinFindUsagesHandlerFactory(project: Project) : FindUsagesHandlerFactory() {
    val javaHandlerFactory = JavaFindUsagesHandlerFactory(project)

    val findFunctionOptions = KotlinFunctionFindUsagesOptions(project)
    val findPropertyOptions = KotlinPropertyFindUsagesOptions(project)
    val findClassOptions = KotlinClassFindUsagesOptions(project)
    val defaultOptions = FindUsagesOptions(project)

    public override fun canFindUsages(element: PsiElement): Boolean =
            element is JetClassOrObject ||
            element is JetNamedFunction ||
            element is JetProperty ||
            element is JetParameter ||
            element is JetTypeParameter

    public override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
        when(element) {
            is JetClassOrObject ->
                return KotlinFindClassUsagesHandler(element, this)

            is JetNamedFunction, is JetProperty, is JetParameter -> {
                val declaration = element as JetNamedDeclaration

                return if (forHighlightUsages) KotlinFindMemberUsagesHandler.getInstance(declaration, this)
                else JetRefactoringUtil.checkSuperMethods(declaration, null, "super.methods.action.key.find.usages")?.let { callables ->
                    when (callables.size()) {
                        0 -> FindUsagesHandler.NULL_HANDLER
                        1 -> {
                            val callable = callables.get(0)
                            when (callable) {
                                is JetNamedDeclaration ->
                                    KotlinFindMemberUsagesHandler.getInstance(callable, this)
                                is PsiMethod ->
                                    javaHandlerFactory.createFindUsagesHandler(callable, forHighlightUsages)
                                else -> null
                            }
                        }
                        else ->
                            KotlinFindMemberUsagesHandler.getInstance(declaration, callables, this)
                    }
                }
            }

            is JetTypeParameter ->
                return KotlinTypeParameterFindUsagesHandler(element, this)

            else ->
                throw IllegalArgumentException("unexpected element type: " + element)
        }
    }

}
