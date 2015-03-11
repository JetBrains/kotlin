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
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.idea.search.usagesSearch.dataClassComponentFunctionName
import org.jetbrains.kotlin.psi.JetTypeParameter
import org.jetbrains.kotlin.idea.search.usagesSearch.DefaultSearchHelper
import com.intellij.util.Processor
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.openapi.project.Project
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.FileModificationService
import com.intellij.refactoring.safeDelete.SafeDeleteHandler
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.psi.JetObjectDeclaration
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import javax.swing.JComponent
import java.awt.GridBagConstraints
import java.awt.Insets
import com.intellij.codeInspection.ex.EntryPointsManager
import com.intellij.codeInspection.ex.EntryPointsManagerImpl
import com.intellij.openapi.project.ProjectUtil
import java.awt.GridBagLayout
import javax.swing.JPanel

public class UnusedSymbolInspection : AbstractKotlinInspection() {
    default object {
        private val javaInspection = UnusedDeclarationInspection()

        public fun isEntryPoint(declaration: JetNamedDeclaration): Boolean {
            // TODO Workaround for EA-64030 - IOE: PsiJavaParserFacadeImpl.createAnnotationFromText
            // This should be fixed on IDEA side: ClsAnnotation should not throw exceptions when annotation class has Java keyword
            if (declaration.getAnnotationEntries().any { it.getTypeReference().getText().endsWith("native") }) return false
            if (ProjectStructureUtil.isJsKotlinModule(declaration.getContainingJetFile())) return false

            val lightElement: PsiElement? = when (declaration) {
                is JetClassOrObject -> declaration.toLightClass()
                is JetNamedFunction -> LightClassUtil.getLightClassMethod(declaration)
                else -> return false
            }
            return lightElement != null && javaInspection.isEntryPoint(lightElement)
        }
    }

    override fun runForWholeFile() = true

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : JetVisitorVoid() {
            private fun createQuickFix(declaration: JetNamedDeclaration): LocalQuickFix {
                return object : LocalQuickFix {
                    override fun getName() = QuickFixBundle.message("safe.delete.text", declaration.getName())

                    override fun getFamilyName() = "whatever"

                    override fun applyFix(project: Project, descriptor: ProblemDescriptor?) {
                        if (!FileModificationService.getInstance().prepareFileForWrite(declaration.getContainingFile())) return
                        SafeDeleteHandler.invoke(project, array(declaration), false)
                    }
                }
            }

            override fun visitNamedDeclaration(declaration: JetNamedDeclaration) {
                val messageKey = when (declaration) {
                    is JetClass -> "unused.class"
                    is JetObjectDeclaration -> "unused.object"
                    is JetNamedFunction -> "unused.function"
                    is JetProperty, is JetParameter -> "unused.property"
                    is JetTypeParameter -> "unused.type.parameter"
                    else -> return
                }

                if (!ProjectRootsUtil.isInProjectSource(declaration)) return

                // Simple PSI-based checks
                if (declaration.getNameIdentifier() == null) return
                if (declaration is JetEnumEntry) return
                if (declaration.hasModifier(JetTokens.OVERRIDE_KEYWORD)) return
                if (declaration is JetProperty && declaration.isLocal()) return
                if (declaration is JetParameter && (declaration.getParent()?.getParent() !is JetClass || !declaration.hasValOrVarNode())) return
                if (declaration is JetNamedFunction && isConventionalName(declaration)) return
                //TODO: support this inspection for default objects
                if (declaration is JetObjectDeclaration && declaration.isDefault()) return

                // More expensive, resolve-based checks
                if (isEntryPoint(declaration)) return
                // properties can be referred by component1/component2, which is too expensive to search, don't mark them as unused
                if (declaration is JetParameter && declaration.dataClassComponentFunctionName() != null) return

                // Main checks: finding reference usages && text usages
                if (hasNonTrivialUsages(declaration)) return
                if (declaration is JetClassOrObject && classOrObjectHasTextUsages(declaration)) return

                holder.registerProblem(
                        declaration.getNameIdentifier(),
                        JetBundle.message(messageKey, declaration.getName()),
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        createQuickFix(declaration)
                )
            }
        }
    }

    private fun classOrObjectHasTextUsages(classOrObject: JetClassOrObject): Boolean {
        var hasTextUsages = false

        // Finding text usages
        if (classOrObject.getUseScope() is GlobalSearchScope) {
            val findClassUsagesHandler = KotlinFindClassUsagesHandler(classOrObject, KotlinFindUsagesHandlerFactory(classOrObject.getProject()))
            findClassUsagesHandler.processUsagesInText(
                    classOrObject,
                    { hasTextUsages = true; false },
                    GlobalSearchScope.projectScope(classOrObject.getProject())
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
            is JetProperty, is JetParameter -> PropertyUsagesSearchHelper(skipImports = true)
            else -> DefaultSearchHelper<JetNamedDeclaration>()
        }

        val request = searchHelper.newRequest(UsagesSearchTarget(declaration, useScope))
        val query = UsagesSearch.search(request)

        return !query.forEach(Processor {
            assert(it != null, { "Found reference is null, was looking for: " + JetPsiUtil.getElementTextWithContext(declaration) +
                                 " findAll(): " + query.findAll().map { it?.getElement()?.let{ JetPsiUtil.getElementTextWithContext(it) } } })
            declaration.isAncestor(it.getElement())
        })
    }

    override fun createOptionsPanel(): JComponent? {
        val panel = JPanel(GridBagLayout())
        panel.add(
                EntryPointsManagerImpl.createConfigureAnnotationsButton(),
                GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, Insets(0, 0, 0, 0), 0, 0)
        )
        return panel
    }
}
