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

package org.jetbrains.kotlin.idea.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import java.util.*

fun getClassOrObjectChildren(
        classOrObject: KtClassOrObject?, project: Project?,
        settings: ViewSettings): Collection<AbstractTreeNode<*>> {
    if (classOrObject != null && settings.isShowMembers) {
        val result = ArrayList<AbstractTreeNode<*>>()
        val declarations = classOrObject.declarations
        for (declaration in declarations) {
            if (declaration is KtClassOrObject) {
                result.add(KtClassOrObjectTreeNode(project, declaration, settings))
            }
            else {
                result.add(KtDeclarationTreeNode(project, declaration, settings))
            }
        }

        return result
    }
    else {
        return emptyList()
    }
}

fun canRepresentPsiElement(value: PsiElement?, element: Any?, settings: ViewSettings): Boolean {
    if (value == null || !value.isValid) {
        return false
    }

    val file = value.containingFile
    if (file != null && (file === element || file.virtualFile === element)) {
        return true
    }

    if (value === element) {
        return true
    }

    if (!settings.isShowMembers) {
        if (element is PsiElement && element.containingFile != null) {
            val elementFile = element.containingFile
            if (elementFile != null && file != null) {
                return elementFile == file
            }
        }
    }

    return false
}
