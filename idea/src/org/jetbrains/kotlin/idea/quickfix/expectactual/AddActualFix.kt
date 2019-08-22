/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix.expectactual

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.quickfix.TypeAccessibilityChecker
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class AddActualFix(
    actualClassOrObject: KtClassOrObject,
    missedDeclarations: List<KtDeclaration>
) : KotlinQuickFixAction<KtClassOrObject>(actualClassOrObject) {
    private val missedDeclarationPointers = missedDeclarations.map { it.createSmartPointer() }

    override fun getFamilyName() = text

    override fun getText() = "Add missing actual members"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val factory = KtPsiFactory(element)

        val codeStyleManager = CodeStyleManager.getInstance(project)

        fun PsiElement.clean() {
            ShortenReferences.DEFAULT.process(codeStyleManager.reformat(this) as KtElement)
        }

        val module = element.module ?: return
        val checker = TypeAccessibilityChecker.create(project, module)
        for (missedDeclaration in missedDeclarationPointers.mapNotNull { it.element }) {
            val actualDeclaration = when (missedDeclaration) {
                is KtClassOrObject -> factory.generateClassOrObject(project, false, missedDeclaration, checker)
                is KtFunction, is KtProperty -> missedDeclaration.toDescriptor()?.safeAs<CallableMemberDescriptor>()?.let {
                    generateCallable(project, false, missedDeclaration, it, element, checker = checker)
                }
                else -> null
            } ?: continue

            if (actualDeclaration is KtPrimaryConstructor) {
                if (element.primaryConstructor == null)
                    element.addAfter(actualDeclaration, element.nameIdentifier).clean()
            } else {
                element.addDeclaration(actualDeclaration).clean()
            }
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val missedDeclarations = DiagnosticFactory.cast(diagnostic, Errors.NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS).b.mapNotNull {
                DescriptorToSourceUtils.descriptorToDeclaration(it.first) as? KtDeclaration
            }.ifEmpty { return null }

            return (diagnostic.psiElement as? KtClassOrObject)?.let {
                AddActualFix(it, missedDeclarations)
            }
        }
    }
}