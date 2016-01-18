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

import com.intellij.ide.projectView.SelectableTreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinIconProvider
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

class KotlinProjectViewProvider(private val myProject: Project) : SelectableTreeStructureProvider, DumbAware {

    override fun modify(parent: AbstractTreeNode<Any>,
                        children: Collection<AbstractTreeNode<Any>>,
                        settings: ViewSettings): Collection<AbstractTreeNode<out Any>> {
        val result = ArrayList<AbstractTreeNode<out Any>>()

        for (child in children) {
            val childValue = child.value

            if (childValue is KtFile) {
                val declarations = childValue.declarations

                val mainClass = KotlinIconProvider.getMainClass(childValue)
                if (mainClass != null && declarations.size == 1) {
                    result.add(KtClassOrObjectTreeNode(childValue.project, mainClass, settings))
                }
                else {
                    result.add(KtFileTreeNode(childValue.project, childValue, settings))
                }
            }
            else {
                result.add(child)
            }

        }

        return result
    }

    override fun getData(selected: Collection<AbstractTreeNode<Any>>, dataName: String): Any? {
        return null
    }

    override fun getTopLevelElement(element: PsiElement): PsiElement? {
        val file = element.containingFile
        if (file == null || file !is KtFile) return null

        val virtualFile = file.virtualFile
        if (!fileInRoots(virtualFile)) return file

        var current: PsiElement? = element
        while (current != null) {
            if (isSelectable(current)) break
            current = current.parent
        }

        if (current is KtFile) {
            val declarations = current.declarations
            val nameWithoutExtension = if (virtualFile != null) virtualFile.nameWithoutExtension else file.name
            if (declarations.size == 1 && declarations[0] is KtClassOrObject &&
                nameWithoutExtension == declarations[0].name) {
                current = declarations[0]
            }
        }

        return if (current != null) current else file
    }

    private fun isSelectable(element: PsiElement): Boolean {
        if (element is KtFile) return true
        if (element is KtDeclaration) {
            var parent = element.getParent()
            if (parent is KtFile) {
                return true
            }
            else if (parent is KtClassBody) {
                parent = parent.getParent()
                if (parent is KtClassOrObject) {
                    return isSelectable(parent)
                }
                else
                    return false
            }
            else
                return false
        }
        else
            return false
    }

    private fun fileInRoots(file: VirtualFile?): Boolean {
        val index = ProjectRootManager.getInstance(myProject).fileIndex
        return file != null && (index.isInSourceContent(file) || index.isInLibraryClasses(file) || index.isInLibrarySource(file))
    }
}
