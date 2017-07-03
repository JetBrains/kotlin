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

package org.jetbrains.kotlin.idea.hierarchy.overrides

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.ide.hierarchy.JavaHierarchyUtil
import com.intellij.ide.hierarchy.MethodHierarchyBrowserBase
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.project.Project
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElement
import com.intellij.ui.PopupHandler
import com.intellij.usageView.UsageViewLongNameLocation
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import javax.swing.JPanel
import javax.swing.JTree

class KotlinOverrideHierarchyBrowser(
        project: Project, baseElement: PsiElement
) : MethodHierarchyBrowserBase(project, baseElement) {
    override fun createTrees(trees: MutableMap<String, JTree>) {
        val actionManager = ActionManager.getInstance()

        val tree = createTree(false)

        val group = actionManager.getAction(IdeActions.GROUP_METHOD_HIERARCHY_POPUP) as ActionGroup
        PopupHandler.installPopupHandler(tree, group, ActionPlaces.METHOD_HIERARCHY_VIEW_POPUP, actionManager)

        BaseOnThisMethodAction().registerCustomShortcutSet(actionManager.getAction(IdeActions.ACTION_METHOD_HIERARCHY).shortcutSet, tree)

        trees.put(MethodHierarchyBrowserBase.METHOD_TYPE, tree)
    }

    override fun createLegendPanel(): JPanel? =
            MethodHierarchyBrowserBase.createStandardLegendPanel(
                    KotlinBundle.message("hierarchy.legend.member.is.defined.in.class"),
                    KotlinBundle.message("hierarchy.legend.member.defined.in.superclass"),
                    KotlinBundle.message("hierarchy.legend.member.should.be.defined")
            )

    override fun getElementFromDescriptor(descriptor: HierarchyNodeDescriptor) = descriptor.psiElement

    override fun isApplicableElement(psiElement: PsiElement): Boolean =
            psiElement.isOverrideHierarchyElement()

    override fun createHierarchyTreeStructure(typeName: String, psiElement: PsiElement): HierarchyTreeStructure? =
            if (typeName == MethodHierarchyBrowserBase.METHOD_TYPE) KotlinOverrideTreeStructure(myProject, psiElement as KtCallableDeclaration) else null

    override fun getComparator() = JavaHierarchyUtil.getComparator(myProject)!!

    override fun getContentDisplayName(typeName: String, element: PsiElement): String? {
        val targetElement = element.unwrapped
        if (targetElement is KtDeclaration) {
            return ElementDescriptionUtil.getElementDescription(targetElement, UsageViewLongNameLocation.INSTANCE)
        }
        return super.getContentDisplayName(typeName, element)
    }
}
