/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.psi.PsiElement
import com.intellij.slicer.SliceUsage
import com.intellij.usages.UsagePresentation
import com.intellij.util.Processor

class KotlinSliceDereferenceUsage(
    element: PsiElement,
    parent: KotlinSliceUsage,
    lambdaLevel: Int
) : KotlinSliceUsage(element, parent, lambdaLevel, false) {
    override fun processChildren(processor: Processor<in SliceUsage>) {
        // no children
    }

    override fun getPresentation() = object : UsagePresentation by super.getPresentation() {
        override fun getTooltipText() = "Variable dereferenced"
    }
}
