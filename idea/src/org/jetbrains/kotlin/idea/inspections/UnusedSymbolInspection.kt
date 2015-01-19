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
import org.jetbrains.kotlin.asJava.LightClassUtil
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
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.PsiSearchHelper.SearchCostResult.*
import org.jetbrains.kotlin.idea.search.usagesSearch.getOperationSymbolsToSearch
import org.jetbrains.kotlin.idea.search.usagesSearch.INVOKE_OPERATION_NAME
import org.jetbrains.kotlin.psi.JetEnumEntry
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.idea.search.usagesSearch.PropertyUsagesSearchHelper
import org.jetbrains.kotlin.idea.search.usagesSearch.getAccessorNames

public class UnusedSymbolInspection : AbstractKotlinInspection() {
    private val javaInspection = UnusedDeclarationInspection()

    override fun runForWholeFile() = true

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : JetVisitorVoid() {
            override fun visitClass(klass: JetClass) {
                if (klass.getName() == null) return

                if (klass is JetEnumEntry) return

                if (isEntryPoint(klass)) return
                if (hasNonTrivialUsages(klass)) return
                if (classHasTextUsages(klass)) return

                holder.registerProblem(
                        klass.getNameIdentifier(),
                        JetBundle.message("unused.class", klass.getName()),
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL
                ) // TODO add quick fix to delete it
            }

            override fun visitNamedFunction(function: JetNamedFunction) {
                if (function.getName() == null) return

                if (function.hasModifier(JetTokens.OVERRIDE_KEYWORD)) return
                if (isEntryPoint(function)) return
                if (isConventionalName(function)) return
                if (hasNonTrivialUsages(function)) return

                holder.registerProblem(
                        function.getNameIdentifier(),
                        JetBundle.message("unused.function", function.getName()),
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL
                ) // TODO add quick fix to delete it
            }

            override fun visitProperty(property: JetProperty) {
                if (property.getName() == null) return

                if (property.isLocal()) return

                if (property.hasModifier(JetTokens.OVERRIDE_KEYWORD)) return
                if (hasNonTrivialUsages(property)) return

                holder.registerProblem(
                        property.getNameIdentifier(),
                        JetBundle.message("unused.property", property.getName()),
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL
                ) // TODO add quick fix to delete it
            }
        }
    }

    private fun isEntryPoint(declaration: JetNamedDeclaration): Boolean {
        val lightElement: PsiElement? = when (declaration) {
            is JetClass ->  declaration.toLightClass()
            is JetNamedFunction -> LightClassUtil.getLightClassMethod(declaration)
            else -> null
        }
        return lightElement != null && javaInspection.isEntryPoint(lightElement)
    }

    private fun classHasTextUsages(klass: JetClass): Boolean {
        var hasTextUsages = false

        // Finding text usages
        if (klass.getUseScope() is GlobalSearchScope) {
            val findClassUsagesHandler = KotlinFindClassUsagesHandler(klass, KotlinFindUsagesHandlerFactory(klass.getProject()))
            findClassUsagesHandler.processUsagesInText(
                    klass,
                    { hasTextUsages = true; false },
                    GlobalSearchScope.projectScope(klass.getProject())
            )
        }

        return hasTextUsages
    }

    private fun isConventionalName(namedDeclaration: JetNamedDeclaration): Boolean {
        val name = namedDeclaration.getNameAsName()
        return name.getOperationSymbolsToSearch().isNotEmpty() || name == INVOKE_OPERATION_NAME
    }

    private fun hasNonTrivialUsages(declaration: JetNamedDeclaration): Boolean {
        val psiSearchHelper = PsiSearchHelper.SERVICE.getInstance(declaration.getProject())

        val useScope = declaration.getUseScope()
        if (useScope is GlobalSearchScope) {
            var zeroOccurrences = true

            for (name in listOf(declaration.getName()) + declaration.getAccessorNames()) {
                when (psiSearchHelper.isCheapEnoughToSearch(name, useScope, null, null)) {
                    ZERO_OCCURRENCES -> {} // go on, check other names
                    FEW_OCCURRENCES -> zeroOccurrences = false
                    TOO_MANY_OCCURRENCES -> return true // searching usages is too expensive; behave like it is used
                }
            }

            if (zeroOccurrences) {
                return false
            }
        }

        val searchHelper: UsagesSearchHelper<out JetNamedDeclaration> = when (declaration) {
            is JetClass -> ClassUsagesSearchHelper(constructorUsages = true, nonConstructorUsages = true, skipImports = true)
            is JetNamedFunction -> FunctionUsagesSearchHelper(skipImports = true)
            is JetProperty -> PropertyUsagesSearchHelper(skipImports = true)
            else -> return false
        }

        val request = searchHelper.newRequest(UsagesSearchTarget(declaration, useScope))
        val query = UsagesSearch.search(request)

        return query.any { !declaration.isAncestor(it.getElement()) }
    }
}