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

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInspection.*
import com.intellij.codeInspection.deadCode.UnusedDeclarationInspection
import com.intellij.codeInspection.ex.EntryPointsManager
import com.intellij.codeInspection.ex.EntryPointsManagerBase
import com.intellij.codeInspection.ex.EntryPointsManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.PsiSearchHelper.SearchCostResult.FEW_OCCURRENCES
import com.intellij.psi.search.PsiSearchHelper.SearchCostResult.TOO_MANY_OCCURRENCES
import com.intellij.psi.search.PsiSearchHelper.SearchCostResult.ZERO_OCCURRENCES
import com.intellij.refactoring.safeDelete.SafeDeleteHandler
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.findUsages.handlers.KotlinFindClassUsagesHandler
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.search.usagesSearch.*
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JPanel

public class UnusedSymbolInspection : AbstractKotlinInspection() {
    companion object {
        private val javaInspection = UnusedDeclarationInspection()

        public fun isEntryPoint(declaration: JetNamedDeclaration): Boolean {
            val lightElement: PsiElement? = when (declaration) {
                is JetClassOrObject -> declaration.toLightClass()
                is JetNamedFunction, is JetSecondaryConstructor -> LightClassUtil.getLightClassMethod(declaration as JetFunction)
                is JetProperty -> {
                    // can't rely on light element, check annotation ourselves
                    val descriptor = declaration.descriptor ?: return false
                    val entryPointsManager = EntryPointsManager.getInstance(declaration.getProject()) as EntryPointsManagerBase
                    return checkAnnotatedUsingPatterns(
                            descriptor,
                            entryPointsManager.getAdditionalAnnotations() + entryPointsManager.ADDITIONAL_ANNOTATIONS
                    )
                }
                else -> return false
            }
            return lightElement != null && javaInspection.isEntryPoint(lightElement)
        }

        // variation of IDEA's AnnotationUtil.checkAnnotatedUsingPatterns()
        private fun checkAnnotatedUsingPatterns(annotated: Annotated, annotationPatterns: Collection<String>): Boolean {
            val annotationsPresent = annotated.getAnnotations()
                    .map { it.getType() }
                    .filter { !it.isError() }
                    .map { it.getConstructor().getDeclarationDescriptor()?.let { DescriptorUtils.getFqName(it).asString() } }
                    .filterNotNull()

            if (annotationsPresent.isEmpty()) return false

            for (pattern in annotationPatterns) {
                val hasAnnotation = if (pattern.endsWith(".*")) {
                    annotationsPresent.any { it.startsWith(pattern.dropLast(1)) }
                } else {
                    pattern in annotationsPresent
                }
                if (hasAnnotation) return true
            }

            return false
        }

    }

    override fun runForWholeFile() = true

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : JetVisitorVoid() {
            private fun createQuickFix(declaration: JetNamedDeclaration): LocalQuickFix {
                return object : LocalQuickFix {
                    override fun getName() = QuickFixBundle.message("safe.delete.text", declaration.getName())

                    override fun getFamilyName() = "Safe delete"

                    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                        if (!FileModificationService.getInstance().prepareFileForWrite(declaration.getContainingFile())) return
                        SafeDeleteHandler.invoke(project, arrayOf(declaration), false)
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
                val isCompanionObject = declaration is JetObjectDeclaration && declaration.isCompanion()
                if (declaration.getName() == null) return
                if (declaration is JetEnumEntry) return
                if (declaration.hasModifier(JetTokens.OVERRIDE_KEYWORD)) return
                if (declaration is JetProperty && declaration.isLocal()) return
                if (declaration is JetParameter && (declaration.getParent()?.getParent() !is JetPrimaryConstructor || !declaration.hasValOrVar())) return
                if (declaration is JetNamedFunction && isConventionalName(declaration)) return

                // More expensive, resolve-based checks
                if (isEntryPoint(declaration)) return
                // properties can be referred by component1/component2, which is too expensive to search, don't mark them as unused
                if (declaration is JetParameter && declaration.dataClassComponentFunctionName() != null) return

                // Main checks: finding reference usages && text usages
                if (hasNonTrivialUsages(declaration)) return
                if (declaration is JetClassOrObject && classOrObjectHasTextUsages(declaration)) return

                val (inspectionTarget, textRange) = if (isCompanionObject && declaration.getNameIdentifier() == null) {
                    val objectKeyword = (declaration as JetObjectDeclaration).getObjectKeyword()
                    Pair(declaration, TextRange(0, objectKeyword.getStartOffsetInParent() + objectKeyword.getTextLength()))
                } else {
                    Pair(declaration.getNameIdentifier()!!, null)
                }

                val problemDescriptor = holder.getManager().createProblemDescriptor(
                        inspectionTarget,
                        textRange,
                        JetBundle.message(messageKey, declaration.getName()),
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        true,
                        createQuickFix(declaration)
                )

                holder.registerProblem(problemDescriptor)
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
        return name!!.getOperationSymbolsToSearch().isNotEmpty() || name == OperatorConventions.INVOKE
    }

    private fun hasNonTrivialUsages(declaration: JetNamedDeclaration): Boolean {
        val psiSearchHelper = PsiSearchHelper.SERVICE.getInstance(declaration.getProject())

        val useScope = declaration.getUseScope()
        if (useScope is GlobalSearchScope) {
            var zeroOccurrences = true

            for (name in listOf(declaration.getName()) + declaration.getAccessorNames() + declaration.getClassNameForCompanionObject().singletonOrEmptyList()) {
                assert(name != null) { "Name is null for " + declaration.getElementTextWithContext() }
                when (psiSearchHelper.isCheapEnoughToSearch(name!!, useScope, null, null)) {
                    ZERO_OCCURRENCES -> {} // go on, check other names
                    FEW_OCCURRENCES -> zeroOccurrences = false
                    TOO_MANY_OCCURRENCES -> return true // searching usages is too expensive; behave like it is used
                }
            }

            if (zeroOccurrences) {
                if (declaration is JetObjectDeclaration && declaration.isCompanion()) {
                    // go on: companion object can be used only in containing class
                }
                else {
                    return false
                }
            }
        }

        val searchHelper: UsagesSearchHelper<out JetNamedDeclaration> = when (declaration) {
            is JetClassOrObject -> ClassUsagesSearchHelper(constructorUsages = true, nonConstructorUsages = true, skipImports = true)
            is JetNamedFunction -> FunctionUsagesSearchHelper(skipImports = true)
            is JetProperty, is JetParameter -> PropertyUsagesSearchHelper(skipImports = true)
            else -> DefaultSearchHelper<JetNamedDeclaration>()
        }

        val request = searchHelper.newRequest(UsagesSearchTarget(declaration, useScope))
        val query = UsagesSearch.search(request)

        return !query.forEach(Processor {
            assert(it != null, { "Found reference is null, was looking for: " + declaration.getElementTextWithContext() +
                                 " findAll(): " + query.findAll().map { it?.getElement()?.let{ it.getElementTextWithContext() } } })
            declaration.isAncestor(it.getElement()) || it.getElement().getParent() is JetValueArgumentName
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
