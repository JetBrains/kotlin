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
import com.intellij.ide.hierarchy.type.JavaTypeHierarchyProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.decompiler.navigation.SourceNavigationHelper
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext

class KotlinTypeHierarchyProvider : JavaTypeHierarchyProvider() {
    private fun getTargetByReference(project: Project, editor: Editor): PsiElement? {
        val target = TargetElementUtil.findTargetElement(editor, TargetElementUtil.getInstance().allAccepted)

        return when (target) {
            is PsiClass -> target
            is KtClassOrObject -> SourceNavigationHelper.getOriginalPsiClassOrCreateLightClass(target)
            is KtNamedFunction -> { // Factory methods
                val functionName = target.name
                val functionDescriptor = target.analyze()[BindingContext.FUNCTION, target] ?: return null
                val type = functionDescriptor.returnType ?: return null
                val returnTypeText = DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type)
                if (returnTypeText != functionName) return null
                val classOrObject = KotlinClassShortNameIndex.getInstance()[functionName, project, project.allScope()].singleOrNull()
                                    ?: return null
                SourceNavigationHelper.getOriginalPsiClassOrCreateLightClass(classOrObject)
            }
            else -> null
        }
    }

    private fun getTargetByContainingElement(editor: Editor, file: PsiFile): PsiElement? {
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset) ?: return null
        val classOrObject = element.getNonStrictParentOfType<KtClassOrObject>() ?: return null
        return SourceNavigationHelper.getOriginalPsiClassOrCreateLightClass(classOrObject)
    }

    override fun getTarget(dataContext: DataContext): PsiElement? {
        val project = PlatformDataKeys.PROJECT.getData(dataContext) ?: return null

        val editor = PlatformDataKeys.EDITOR.getData(dataContext)
        if (editor != null) {
            val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null
            if (!ProjectRootsUtil.isInProjectOrLibSource(file)) return null
            return getTargetByReference(project, editor) ?: getTargetByContainingElement(editor, file)
        }

        val element = LangDataKeys.PSI_ELEMENT.getData(dataContext)
        if (element is KtClassOrObject) return SourceNavigationHelper.getOriginalPsiClassOrCreateLightClass((element as KtClassOrObject?)!!)

        return null
    }
}

