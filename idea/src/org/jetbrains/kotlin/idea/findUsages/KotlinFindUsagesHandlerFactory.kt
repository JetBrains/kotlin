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

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.idea.findUsages.handlers.KotlinFindClassUsagesHandler
import org.jetbrains.kotlin.idea.findUsages.handlers.KotlinFindMemberUsagesHandler
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringUtil
import org.jetbrains.kotlin.psi.JetProperty
import com.intellij.find.findUsages.FindUsagesOptions
import org.jetbrains.kotlin.psi.JetTypeParameter
import org.jetbrains.kotlin.idea.findUsages.handlers.KotlinTypeParameterFindUsagesHandler
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.psi.JetClassOrObject
import com.intellij.find.findUsages.JavaFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.findUsages.handlers.DelegatingFindMemberUsagesHandler
import org.jetbrains.kotlin.plugin.findUsages.handlers.KotlinFindUsagesHandlerDecorator
import com.intellij.openapi.extensions.Extensions

public class KotlinFindUsagesHandlerFactory(project: Project) : FindUsagesHandlerFactory() {
    val javaHandlerFactory = JavaFindUsagesHandlerFactory(project)

    public val findFunctionOptions: KotlinFunctionFindUsagesOptions = KotlinFunctionFindUsagesOptions(project)
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
        val handler = when (element) {
            is JetClassOrObject ->
                KotlinFindClassUsagesHandler(element, this)

            is JetNamedFunction, is JetProperty, is JetParameter -> {
                val declaration = element as JetNamedDeclaration

                if (forHighlightUsages) return KotlinFindMemberUsagesHandler.getInstance(declaration, this)
                JetRefactoringUtil.checkSuperMethods(declaration, null, "super.methods.action.key.find.usages")?.let { callables ->
                    if (callables.empty) FindUsagesHandler.NULL_HANDLER else DelegatingFindMemberUsagesHandler(declaration, callables, this)
                }
            }

            is JetTypeParameter ->
                KotlinTypeParameterFindUsagesHandler(element, this)

            else ->
                throw IllegalArgumentException("unexpected element type: " + element)
        }

        for (decorator in Extensions.getArea(element.getProject()).getExtensionPoint(KotlinFindUsagesHandlerDecorator.EP_NAME).getExtensions()) {
            val decorated = decorator.decorateHandler(element, forHighlightUsages, handler!!)
            if (decorated != handler) return decorated
        }
        return handler
    }

}
