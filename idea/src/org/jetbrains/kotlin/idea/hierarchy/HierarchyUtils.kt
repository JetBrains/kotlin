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

package org.jetbrains.kotlin.idea.hierarchy

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

fun isCallHierarchyElement(e: PsiElement): Boolean {
    return e is PsiMethod ||
           e is PsiClass ||
           e is KtFile ||
           e is KtNamedFunction ||
           e is KtSecondaryConstructor ||
           e is KtObjectDeclaration ||
           e is KtClass && !e.isInterface() ||
           e is KtProperty
}

fun getCurrentElement(dataContext: DataContext, project: Project): PsiElement? {
    val editor = CommonDataKeys.EDITOR.getData(dataContext)
    if (editor != null) {
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null
        if (!ProjectRootsUtil.isInProjectOrLibSource(file)) return null
        return TargetElementUtil.findTargetElement(editor, TargetElementUtil.getInstance().allAccepted)
    }

    return CommonDataKeys.PSI_ELEMENT.getData(dataContext)
}

fun getCallHierarchyElement(element: PsiElement) = element.parentsWithSelf.firstOrNull(::isCallHierarchyElement)