/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.slicer.SliceUsage
import com.intellij.util.Processor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtElement

object LambdaResultInflowBehaviour : KotlinSliceAnalysisMode.Behaviour {
    override fun processUsages(
        element: KtElement,
        parent: KotlinSliceUsage,
        uniqueProcessor: Processor<in SliceUsage>
    ) {
        InflowSlicer(element, uniqueProcessor, parent).processChildren(parent.forcedExpressionMode)
    }

    override val slicePresentationPrefix: String
        get() = KotlinBundle.message("slicer.text.tracking.enclosing.lambda")

    override val testPresentationPrefix: String
        get() = "[LAMBDA IN] "

    override fun equals(other: Any?) = other === this
    override fun hashCode() = 0
}