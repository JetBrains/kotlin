/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.asJava.classes.KtFakeLightClass
import org.jetbrains.kotlin.asJava.toFakeLightClass
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KotlinTypeHierarchyProvider : JavaTypeHierarchyProvider() {
    private fun getOriginalPsiClassOrCreateLightClass(classOrObject: KtClassOrObject, module: Module?): PsiClass? {
        val fqName = classOrObject.fqName
        if (fqName != null && module?.platform.isJvm()) {
            val javaClassId = JavaToKotlinClassMap.mapKotlinToJava(fqName.toUnsafe())
            if (javaClassId != null) {
                return JavaPsiFacade.getInstance(classOrObject.project).findClass(
                    javaClassId.asSingleFqName().asString(),
                    GlobalSearchScope.allScope(classOrObject.project)
                )
            }
        }
        return classOrObject.toLightClass() ?: classOrObject.toFakeLightClass()
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

