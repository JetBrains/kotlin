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

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.lang.Language
import com.intellij.lang.refactoring.InlineActionHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInliner.CallableUsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.CodeToInlineBuilder
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.addIfNotNull

class KotlinInlineValHandler : InlineActionHandler() {

    override fun isEnabledForLanguage(l: Language) = l == KotlinLanguage.INSTANCE

    override fun canInlineElement(element: PsiElement): Boolean {
        if (element !is KtProperty) return false
        return element.getter == null && element.receiverTypeReference == null
    }

    override fun inlineElement(project: Project, editor: Editor?, element: PsiElement) {
        val declaration = element as KtProperty
        val file = declaration.containingKtFile
        val name = declaration.name ?: return
        if (file.isCompiled) {
            return showErrorHint(project, editor, "Cannot inline '$name' from a decompiled file")
        }

        val references = ReferencesSearch.search(declaration)
        val referenceExpressions = mutableListOf<KtExpression>()
        val foreignUsages = mutableListOf<PsiElement>()
        for (ref in references) {
            val refElement = ref.element ?: continue
            if (refElement !is KtElement) {
                foreignUsages.add(refElement)
                continue
            }
            referenceExpressions.addIfNotNull((refElement as? KtExpression)?.getQualifiedExpressionForSelectorOrThis())
        }

        if (referenceExpressions.isEmpty()) {
            val kind = if (declaration.isLocal) "Variable" else "Property"
            return showErrorHint(project, editor, "$kind '$name' is never used")
        }

        val assignments = hashSetOf<PsiElement>()
        referenceExpressions.forEach { expression ->
            val parent = expression.parent

            val assignment = expression.getAssignmentByLHS()
            if (assignment != null) {
                assignments.add(parent)
            }

            if (parent is KtUnaryExpression && OperatorConventions.INCREMENT_OPERATIONS.contains(parent.operationToken)) {
                assignments.add(parent)
            }
        }

        val initializerInDeclaration = declaration.initializer
        val initializer = if (initializerInDeclaration != null) {
            if (!assignments.isEmpty()) return reportAmbiguousAssignment(project, editor, name, assignments)
            initializerInDeclaration
        }
        else {
            (assignments.singleOrNull() as? KtBinaryExpression)?.right
            ?: return reportAmbiguousAssignment(project, editor, name, assignments)
        }

        val referencesInOriginalFile = referenceExpressions.filter { it.containingFile == file }
        val isHighlighting = referencesInOriginalFile.isNotEmpty()
        highlightElements(project, editor, referencesInOriginalFile)

        if (referencesInOriginalFile.size != referenceExpressions.size) {
            preProcessInternalUsages(initializer, referenceExpressions)
        }

        val descriptor = element.resolveToDescriptor() as VariableDescriptor
        val expectedType = if (element.typeReference != null)
            descriptor.returnType ?: TypeUtils.NO_EXPECTED_TYPE
        else
            TypeUtils.NO_EXPECTED_TYPE

        val initializerCopy = initializer.copied()
        fun analyzeInitializerCopy(): BindingContext {
            return initializerCopy.analyzeInContext(initializer.getResolutionScope(),
                                                    contextExpression = initializer,
                                                    expectedType = expectedType)
        }

        fun performRefactoring() {
            val reference = editor?.let { TargetElementUtil.findReference(it, it.caretModel.offset) } as? KtSimpleNameReference
            val replacementBuilder = CodeToInlineBuilder(descriptor, element.getResolutionFacade())
            val replacement = replacementBuilder.prepareCodeToInline(initializerCopy, emptyList(), ::analyzeInitializerCopy)
            val replacementStrategy = CallableUsageReplacementStrategy(replacement)

            val dialog = KotlinInlineValDialog(declaration, reference, replacementStrategy, assignments)

            if (!ApplicationManager.getApplication().isUnitTestMode) {
                dialog.show()
                if (!dialog.isOK && isHighlighting) {
                    val statusBar = WindowManager.getInstance().getStatusBar(project)
                    statusBar?.info = RefactoringBundle.message("press.escape.to.remove.the.highlighting")
                }
            }
            else {
                dialog.doAction()
            }
        }

        if (foreignUsages.isNotEmpty()) {
            val conflicts = MultiMap<PsiElement, String>().apply {
                putValue(null, "Property '$name' has non-Kotlin usages. They won't be processed by the Inline refactoring.")
                foreignUsages.forEach { putValue(it, it.text) }
            }
            project.checkConflictsInteractively(conflicts) { performRefactoring() }
        }
        else {
            performRefactoring()
        }
    }

    private fun reportAmbiguousAssignment(project: Project, editor: Editor?, name: String, assignments: Set<PsiElement>) {
        val key = if (assignments.isEmpty()) "variable.has.no.initializer" else "variable.has.no.dominating.definition"
        val message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message(key, name))
        showErrorHint(project, editor, message)
    }

    private fun showErrorHint(project: Project, editor: Editor?, message: String) {
        CommonRefactoringUtil.showErrorHint(project, editor, message, RefactoringBundle.message("inline.variable.title"), HelpID.INLINE_VARIABLE)
    }

}
