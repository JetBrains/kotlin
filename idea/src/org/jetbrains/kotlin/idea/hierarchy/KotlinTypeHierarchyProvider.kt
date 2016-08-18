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

import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.ide.hierarchy.type.JavaTypeHierarchyProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.decompiler.navigation.SourceNavigationHelper
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext

class KotlinTypeHierarchyProvider : JavaTypeHierarchyProvider() {
    override fun getTarget(dataContext: DataContext): PsiElement? {
        val project = PlatformDataKeys.PROJECT.getData(dataContext) ?: return null

        val editor = PlatformDataKeys.EDITOR.getData(dataContext)
        if (editor != null) {
            val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null

            if (!ProjectRootsUtil.isInProjectOrLibSource(file)) return null

            val target = TargetElementUtilBase.findTargetElement(editor, TargetElementUtilBase.getInstance().allAccepted)

            if (target is PsiClass) {
                return target
            }

            if (target is KtClassOrObject) {
                return SourceNavigationHelper.getOriginalPsiClassOrCreateLightClass((target as KtClassOrObject?)!!)
            }
            else if (target is KtNamedFunction) {
                val functionName = target.name
                val functionDescriptor = target.analyze().get<PsiElement, SimpleFunctionDescriptor>(BindingContext.FUNCTION, target)
                if (functionDescriptor != null) {
                    val type = functionDescriptor!!.getReturnType()
                    if (type != null) {
                        val returnTypeText = DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type!!)
                        if (returnTypeText == functionName) {
                            val classOrObjects = KotlinClassShortNameIndex.getInstance().get(functionName, project, GlobalSearchScope.allScope(project))
                            if (classOrObjects.size == 1) {
                                val classOrObject = classOrObjects.iterator().next()
                                return SourceNavigationHelper.getOriginalPsiClassOrCreateLightClass(classOrObject)
                            }
                        }
                    }
                }
            }// Factory methods

            val offset = editor.caretModel.offset
            val element = file.findElementAt(offset) ?: return null

            val classOrObject = PsiTreeUtil.getParentOfType(element, KtClassOrObject::class.java)
            if (classOrObject != null) {
                return SourceNavigationHelper.getOriginalPsiClassOrCreateLightClass(classOrObject)
            }
        }
        else {
            val element = LangDataKeys.PSI_ELEMENT.getData(dataContext)
            if (element is KtClassOrObject) {
                return SourceNavigationHelper.getOriginalPsiClassOrCreateLightClass((element as KtClassOrObject?)!!)
            }
        }

        return null
    }
}

