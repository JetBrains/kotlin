/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.presentation

import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.NavigatablePsiElement
import org.jetbrains.kotlin.idea.util.module

abstract class KtModuleSpecificListCellRenderer<T : NavigatablePsiElement> : PsiElementListCellRenderer<T>() {
    override fun getIconFlags() = 0

    override fun getComparingObject(element: T?): Comparable<Nothing> {
        val baseText = super.getComparingObject(element)
        val moduleName = runReadAction {
            element?.module?.name
        } ?: return baseText
        return "$baseText [$moduleName]"
    }
}