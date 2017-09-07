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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.ImportAllMembersIntention
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class AddWhenRemainingBranchesFixWithImport(expression: KtWhenExpression) : AddWhenRemainingBranchesFix(expression) {

    override fun getFamilyName() = "Add remaining branches with import"

    override fun getText() = "Add remaining branches with import"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (!super.isAvailable(project, editor, file)) return false
        if (!isEnumSubject(element)) return false
        return true
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        super.invoke(project, editor, file)
        importMember(element)
    }

    private fun importMember(element: KtWhenExpression?) {
        if (!isEnumSubject(element)) return
        element?.entries
                ?.map { it.conditions.toList() }
                ?.flatten()
                ?.firstNotNullResult {
                    (it as? KtWhenConditionWithExpression)?.expression as? KtDotQualifiedExpression
                }
                ?.also {
                    ImportAllMembersIntention.importMember(it)
                }
    }

    private fun isEnumSubject(element: KtWhenExpression?): Boolean {
        val subject = element?.subjectExpression?.referenceExpression() ?: return false
        val descriptor = subject.analyze().getType(subject)?.constructor?.declarationDescriptor ?: return false
        val declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? KtClass ?: return false
        return declaration.isEnum()
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<PsiElement>? {
            val whenExpression = diagnostic.psiElement.getNonStrictParentOfType<KtWhenExpression>() ?: return null
            return AddWhenRemainingBranchesFixWithImport(whenExpression)
        }
    }

}