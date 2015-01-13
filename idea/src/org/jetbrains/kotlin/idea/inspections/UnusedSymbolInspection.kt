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

package org.jetbrains.kotlin.idea.inspections

import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.JetVisitorVoid
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchTarget
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearch
import org.jetbrains.kotlin.idea.JetBundle
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.deadCode.UnusedDeclarationInspection
import org.jetbrains.kotlin.idea.findUsages.handlers.KotlinFindClassUsagesHandler
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchHelper
import org.jetbrains.kotlin.idea.search.usagesSearch.ClassUsagesSearchHelper
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.search.usagesSearch.FunctionUsagesSearchHelper

public class UnusedSymbolInspection : AbstractKotlinInspection() {
    private val javaInspection = UnusedDeclarationInspection()

    override fun runForWholeFile() = true

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : JetVisitorVoid() {
            override fun visitClass(klass: JetClass) {
                if (klass.getName() == null) return

                if (classIsEntryPoint(klass)) return
                if (hasNonTrivialUsages(klass)) return
                if (classHasTextUsages(klass)) return

                holder.registerProblem(
                        klass.getNameIdentifier(),
                        JetBundle.message("unused.class", klass.getName()),
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL
                ) // TODO add quick fix to delete it
            }
        }
    }

    private fun classIsEntryPoint(klass: JetClass): Boolean {
        val lightClass = klass.toLightClass()
        if (lightClass != null && javaInspection.isEntryPoint(lightClass)) return true
        return false
    }

    private fun classHasTextUsages(klass: JetClass): Boolean {
        var hasTextUsages = false

        val classUseScope = klass.getUseScope()
        // Finding text usages
        if (classUseScope is GlobalSearchScope) {
            val findClassUsagesHandler = KotlinFindClassUsagesHandler(klass, KotlinFindUsagesHandlerFactory(klass.getProject()))
            findClassUsagesHandler.processUsagesInText(
                    klass,
                    { hasTextUsages = true; false },
                    classUseScope
            )
        }

        return hasTextUsages
    }

    private fun hasNonTrivialUsages(declaration: JetNamedDeclaration): Boolean {
        val searchHelper: UsagesSearchHelper<out JetNamedDeclaration> = when (declaration) {
            is JetClass -> ClassUsagesSearchHelper(constructorUsages = true, nonConstructorUsages = true, skipImports = true)
            else -> return false
        }

        val useScope = declaration.getUseScope()
        val request = searchHelper.newRequest(UsagesSearchTarget(declaration, useScope))
        val query = UsagesSearch.search(request)

        return query.any { !declaration.isAncestor(it.getElement()) }
    }
}