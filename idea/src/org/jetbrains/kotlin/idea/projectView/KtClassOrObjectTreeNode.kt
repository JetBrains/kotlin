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

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinIconProvider
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtPsiUtil

class KtClassOrObjectTreeNode(project: Project?, ktClassOrObject: KtClassOrObject, viewSettings: ViewSettings)
    : AbstractPsiBasedNode<KtClassOrObject>(project, ktClassOrObject, viewSettings) {

    override fun extractPsiFromValue(): PsiElement? {
        return value
    }

    override fun getChildrenImpl(): Collection<AbstractTreeNode<*>>? {
        return getClassOrObjectChildren(value, project, settings)
    }

    private fun update(node: AbstractTreeNode<*>) {
        val project = project
        if (project != null) {
            ProjectView.getInstance(project).currentProjectViewPane.treeBuilder.addSubtreeToUpdateByElement(node)
        }
    }

    override fun updateImpl(data: PresentationData) {
        val classOrObject = value
        if (classOrObject != null) {
            data.setPresentableText(classOrObject.name)

            val parent = parent
            if (KotlinIconProvider.getMainClass(classOrObject.getContainingKtFile()) != null) {
                if (parent is KtFileTreeNode) {
                    update(parent.getParent())
                }
            }
            else {
                if (parent !is KtClassOrObjectTreeNode && parent !is KtFileTreeNode) {
                    update(parent)
                }
            }
        }
    }

    override fun isDeprecated(): Boolean {
        return KtPsiUtil.isDeprecated(value)
    }

    override fun canRepresent(element: Any?): Boolean {
        if (!isValid) {
            return false
        }

        return super.canRepresent(element) || canRepresentPsiElement(value, element, settings)
    }

    override fun getWeight(): Int {
        return 20
    }
}
