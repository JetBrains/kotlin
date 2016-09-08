/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class RemoveFinalUpperBoundFix(val ktTypeReference: KtTypeReference) : KotlinQuickFixAction<KtTypeReference>(ktTypeReference) {
    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = ktTypeReference.getStrictParentOfType<KtFunction>() ?: ktTypeReference.getStrictParentOfType<KtClass>() ?: return
        val parameter = ktTypeReference.parent as? KtTypeParameter ?: return
        val parameterList = parameter.getStrictParentOfType<KtTypeParameterList>() ?: return
        val userTypes = element.collectDescendantsOfType<KtUserType>()
        val bound = parameter.extendsBound ?: return
        userTypes.filter {
            it.text == parameter.nameIdentifier?.text
        }.map {
            it.replaced(bound)
        }

        if (parameterList.parameters.size == 1) {
            parameterList.delete()
        }
        else {
            parameterList.removeParameter(parameter)
        }
    }

    override fun getText() = "Remove final upper bound"

    override fun getFamilyName() = "Remove upper bound"

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            return RemoveFinalUpperBoundFix(Errors.FINAL_UPPER_BOUND.cast(diagnostic).psiElement)
        }
    }
}