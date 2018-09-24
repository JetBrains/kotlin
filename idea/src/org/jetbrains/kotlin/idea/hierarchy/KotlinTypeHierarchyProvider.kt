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
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.idea.caches.lightClasses.KtFakeLightClass
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.platform.impl.isJvm
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KotlinTypeHierarchyProvider : JavaTypeHierarchyProvider() {
    private fun getOriginalPsiClassOrCreateLightClass(classOrObject: KtClassOrObject, module: Module?): PsiClass? {
        val fqName = classOrObject.fqName
        if (fqName != null && module?.platform.isJvm) {
            val javaClassId = JavaToKotlinClassMap.mapKotlinToJava(fqName.toUnsafe())
            if (javaClassId != null) {
                return JavaPsiFacade.getInstance(classOrObject.project).findClass(
                        javaClassId.asSingleFqName().asString(),
                        GlobalSearchScope.allScope(classOrObject.project)
                )
            }
        }
        return classOrObject.toLightClass() ?: KtFakeLightClass(classOrObject)
    }

    private fun getTargetByReference(
        project: Project,
        editor: Editor,
        module: Module?
    ): PsiElement? {
        val target = TargetElementUtil.findTargetElement(editor, TargetElementUtil.getInstance().allAccepted)

        return when (target) {
            is PsiClass -> target
            is KtConstructor<*> -> getOriginalPsiClassOrCreateLightClass(target.getContainingClassOrObject(), module)
            is KtClassOrObject -> getOriginalPsiClassOrCreateLightClass(target, module)
            is KtNamedFunction -> { // Factory methods
                val functionName = target.name
                val functionDescriptor = target.resolveToDescriptorIfAny(BodyResolveMode.FULL) ?: return null
                val type = functionDescriptor.returnType ?: return null
                val returnTypeText = DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type)
                if (returnTypeText != functionName) return null
                val classOrObject = KotlinClassShortNameIndex.getInstance()[functionName, project, project.allScope()].singleOrNull()
                                    ?: return null
                getOriginalPsiClassOrCreateLightClass(classOrObject, module)
            }
            else -> null
        }
    }

    private fun getTargetByContainingElement(editor: Editor, file: PsiFile): PsiElement? {
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset) ?: return null
        val classOrObject = element.getNonStrictParentOfType<KtClassOrObject>() ?: return null
        return getOriginalPsiClassOrCreateLightClass(classOrObject, file.module)
    }

    override fun getTarget(dataContext: DataContext): PsiElement? {
        val project = PlatformDataKeys.PROJECT.getData(dataContext) ?: return null

        val editor = PlatformDataKeys.EDITOR.getData(dataContext)
        if (editor != null) {
            val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null
            if (!ProjectRootsUtil.isInProjectOrLibSource(file)) return null
            val psiElement = getTargetByReference(project, editor, file.module) ?: getTargetByContainingElement(editor, file)
            if (psiElement is PsiNamedElement && psiElement.name == null) {
                return null
            }
            return psiElement
        }

        val element = LangDataKeys.PSI_ELEMENT.getData(dataContext)
        if (element is KtClassOrObject) return getOriginalPsiClassOrCreateLightClass(element, element.module)

        return null
    }
}

