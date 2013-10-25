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
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.plugin.findUsages.handlers.KotlinFindClassUsagesHandler
import org.jetbrains.jet.plugin.findUsages.handlers.KotlinFindCallableUsagesHandler
import org.jetbrains.jet.plugin.refactoring.JetRefactoringUtil
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetCallableDeclaration

public class KotlinFindUsagesHandlerFactory(project: Project) : FindUsagesHandlerFactory() {
    val findFunctionOptions = KotlinFunctionFindUsagesOptions(project)
    val findPropertyOptions = KotlinPropertyFindUsagesOptions(project)
    val findClassOptions = KotlinClassFindUsagesOptions(project)

    public override fun canFindUsages(element: PsiElement): Boolean =
            element is JetClass || element is JetNamedFunction || element is JetProperty

    public override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler {
        when(element) {
            is JetClass ->
                return KotlinFindClassUsagesHandler(element, this)

            is JetNamedFunction, is JetProperty -> {
                val declaration = element as JetCallableDeclaration

                return if (forHighlightUsages) KotlinFindCallableUsagesHandler.getInstance(declaration, this)
                else JetRefactoringUtil.checkSuperMethods(declaration, null, "super.methods.action.key.find.usages")?.let { callables ->
                    when (callables.size()) {
                        0 -> null
                        1 ->
                            KotlinFindCallableUsagesHandler.getInstance(callables.get(0) as JetCallableDeclaration, this)
                        else ->
                            KotlinFindCallableUsagesHandler.getInstance(declaration, callables, this)
                    }
                } ?: FindUsagesHandler.NULL_HANDLER
            }

            else ->
                throw IllegalArgumentException("unexpected element type: " + element)
        }
    }

}
