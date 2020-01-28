/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
package org.jetbrains.kotlin.idea.hierarchy.calls

import com.intellij.ide.hierarchy.CallHierarchyBrowserBase
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.ide.hierarchy.JavaHierarchyUtil
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.ui.PopupHandler
import org.jetbrains.kotlin.psi.KtElement
import java.util.*
import javax.swing.JTree

class KotlinCallHierarchyBrowser(element: PsiElement) :
    CallHierarchyBrowserBase(element.project, element) {
    override fun createTrees(type2TreeMap: MutableMap<String, JTree>) {
        val group =
            ActionManager.getInstance().getAction(IdeActions.GROUP_CALL_HIERARCHY_POPUP) as ActionGroup
        val tree1 = createTree(false)
        PopupHandler.installPopupHandler(
            tree1,
            group,
            ActionPlaces.CALL_HIERARCHY_VIEW_POPUP,
            ActionManager.getInstance()
        )
        val baseOnThisMethodAction =
            BaseOnThisMethodAction()
        baseOnThisMethodAction.registerCustomShortcutSet(
            ActionManager.getInstance().getAction(IdeActions.ACTION_CALL_HIERARCHY).shortcutSet,
            tree1
        )
        type2TreeMap[CALLEE_TYPE] = tree1
        val tree2 = createTree(false)
        PopupHandler.installPopupHandler(
            tree2,
            group,
            ActionPlaces.CALL_HIERARCHY_VIEW_POPUP,
            ActionManager.getInstance()
        )
        baseOnThisMethodAction.registerCustomShortcutSet(
            ActionManager.getInstance().getAction(IdeActions.ACTION_CALL_HIERARCHY).shortcutSet,
            tree2
        )
        type2TreeMap[CALLER_TYPE] = tree2
    }

    override fun getElementFromDescriptor(descriptor: HierarchyNodeDescriptor): PsiElement? {
        return getTargetElement(descriptor)
    }

    override fun isApplicableElement(element: PsiElement): Boolean {
        return if (element is PsiClass) false else isCallHierarchyElement(element) // PsiClass is not allowed at the hierarchy root
    }

    override fun createHierarchyTreeStructure(
        typeName: String,
        psiElement: PsiElement
    ): HierarchyTreeStructure? {
        if (psiElement !is KtElement) return null
        if (typeName == CALLER_TYPE) {
            return KotlinCallerTreeStructure(psiElement, currentScopeType)
        }
        return if (typeName == CALLEE_TYPE) {
            KotlinCalleeTreeStructure(psiElement, currentScopeType)
        } else null
    }

    override fun getComparator(): Comparator<NodeDescriptor<*>> {
        return JavaHierarchyUtil.getComparator(myProject)
    }

    companion object {
        private fun getTargetElement(descriptor: HierarchyNodeDescriptor): PsiElement? {
            return (descriptor as? KotlinCallHierarchyNodeDescriptor)?.psiElement
        }
    }
}