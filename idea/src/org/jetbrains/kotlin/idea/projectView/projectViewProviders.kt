/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.projectView

import com.intellij.ide.projectView.SelectableTreeStructureProvider
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.idea.KotlinIconProviderBase
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import java.util.*

class KotlinExpandNodeProjectViewProvider : TreeStructureProvider, DumbAware {

    // should be called after ClassesTreeStructureProvider
    override fun modify(
        parent: AbstractTreeNodeAny,
        children: Collection<AbstractTreeNodeAny>,
        settings: ViewSettings
    ): Collection<AbstractTreeNode<out Any>> {
        val result = ArrayList<AbstractTreeNode<out Any>>()

        for (child in children) {
            val childValue = child.value?.asKtFile()

            if (childValue != null) {
                val mainClass = KotlinIconProviderBase.getSingleClass(childValue)
                if (mainClass != null) {
                    result.add(KtClassOrObjectTreeNode(childValue.project, mainClass, settings))
                } else {
                    result.add(KtFileTreeNode(childValue.project, childValue, settings))
                }
            } else {
                result.add(child)
            }

        }

        return result
    }

    private fun Any.asKtFile(): KtFile? = when (this) {
        is KtFile -> this
        is KtLightClassForFacade -> files.singleOrNull()
        is KtLightClass -> kotlinOrigin?.containingFile as? KtFile
        else -> null
    }

    override fun getData(selected: Collection<AbstractTreeNodeAny>, dataName: String): Any? = null
}


class KotlinSelectInProjectViewProvider(private val project: Project) : SelectableTreeStructureProvider, DumbAware {
    override fun getData(selected: Collection<AbstractTreeNodeAny>, dataName: String): Any? = null

    override fun modify(
        parent: AbstractTreeNodeAny,
        children: Collection<AbstractTreeNodeAny>,
        settings: ViewSettings
    ): Collection<AbstractTreeNode<out Any>> {
        return ArrayList(children)
    }


    // should be called before ClassesTreeStructureProvider
    override fun getTopLevelElement(element: PsiElement): PsiElement? {
        if (!element.isValid) return null
        val file = element.containingFile as? KtFile ?: return null

        val virtualFile = file.virtualFile
        if (!fileInRoots(virtualFile)) return file

        var current = element.parentsWithSelf.firstOrNull() { it.isSelectable() }

        if (current is KtFile) {
            val declaration = current.declarations.singleOrNull()
            val nameWithoutExtension = virtualFile?.nameWithoutExtension ?: file.name
            if (declaration is KtClassOrObject && nameWithoutExtension == declaration.name) {
                current = declaration
            }
        }

        return current ?: file
    }

    private fun PsiElement.isSelectable(): Boolean = when (this) {
        is KtFile -> true
        is KtDeclaration -> parent is KtFile || ((parent as? KtClassBody)?.parent as? KtClassOrObject)?.isSelectable() ?: false
        else -> false
    }

    private fun fileInRoots(file: VirtualFile?): Boolean {
        val index = ProjectRootManager.getInstance(project).fileIndex
        return file != null && (index.isInSourceContent(file) || index.isInLibraryClasses(file) || index.isInLibrarySource(file))
    }
}
