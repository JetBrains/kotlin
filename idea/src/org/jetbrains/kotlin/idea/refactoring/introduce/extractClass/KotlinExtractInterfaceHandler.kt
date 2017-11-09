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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractClass

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.kotlin.idea.refactoring.introduce.extractClass.ui.KotlinExtractInterfaceDialog
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

object KotlinExtractInterfaceHandler : KotlinExtractSuperHandlerBase(true) {
    val REFACTORING_NAME = "Extract Interface"

    override fun getErrorMessage(klass: KtClassOrObject): String? {
        if (klass is KtClass && klass.isAnnotation()) return "Interface cannot be extracted from an annotation class"
        return null
    }

    override fun doInvoke(klass: KtClassOrObject, targetParent: PsiElement, project: Project, editor: Editor?) {
        KotlinExtractInterfaceDialog(
                originalClass = klass,
                targetParent = targetParent,
                conflictChecker = { checkConflicts(klass, it) },
                refactoring = { ExtractSuperRefactoring(it).performRefactoring() }
        ).show()
    }
}