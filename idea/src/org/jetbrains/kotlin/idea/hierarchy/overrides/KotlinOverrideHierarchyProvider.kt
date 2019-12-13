/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.hierarchy.overrides

import com.intellij.ide.hierarchy.HierarchyBrowser
import com.intellij.ide.hierarchy.HierarchyBrowserBaseEx
import com.intellij.ide.hierarchy.HierarchyProvider
import com.intellij.ide.hierarchy.MethodHierarchyBrowserBase
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.hierarchy.getCurrentElement
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate

class KotlinOverrideHierarchyProvider : HierarchyProvider {
    override fun getTarget(dataContext: DataContext): PsiElement? {
        return CommonDataKeys.PROJECT.getData(dataContext)?.let { project ->
            getOverrideHierarchyElement(getCurrentElement(dataContext, project))
        }
    }

    override fun createHierarchyBrowser(target: PsiElement): HierarchyBrowser =
        KotlinOverrideHierarchyBrowser(target.project, target)

    override fun browserActivated(hierarchyBrowser: HierarchyBrowser) {
        (hierarchyBrowser as HierarchyBrowserBaseEx).changeView(MethodHierarchyBrowserBase.METHOD_TYPE)
    }

    private fun getOverrideHierarchyElement(element: PsiElement?): PsiElement? =
        element?.getParentOfTypesAndPredicate { it.isOverrideHierarchyElement() }
}

fun PsiElement.isOverrideHierarchyElement() = this is KtCallableDeclaration && containingClassOrObject != null
