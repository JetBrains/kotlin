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

package org.jetbrains.kotlin.idea.quickfix.migration

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.CleanupFix
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class MigrateTypeParameterListFix(typeParameterList: KtTypeParameterList)
    : KotlinQuickFixAction<KtTypeParameterList>(typeParameterList), CleanupFix {

    override fun getFamilyName(): String = "Migrate type parameter list syntax"
    override fun getText(): String  = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val function = element.getStrictParentOfType<KtNamedFunction>() ?: return
        function.addBefore(element, function.receiverTypeReference ?: function.nameIdentifier)
        element.delete()
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val typeParameterList = diagnostic.psiElement as? KtTypeParameterList ?: return null
            return MigrateTypeParameterListFix(typeParameterList)
        }
    }
}
