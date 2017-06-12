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

package org.jetbrains.kotlin.idea.slicer

import com.intellij.psi.PsiElement
import com.intellij.slicer.SliceAnalysisParams
import com.intellij.slicer.SliceUsage
import com.intellij.util.Processor
import org.jetbrains.kotlin.psi.KtExpression

open class KotlinSliceUsage : SliceUsage {
    val lambdaLevel: Int
    val forcedExpressionMode: Boolean

    constructor(element: PsiElement, parent: SliceUsage, lambdaLevel: Int, forcedExpressionMode: Boolean) : super(element, parent) {
        this.lambdaLevel = lambdaLevel
        this.forcedExpressionMode = forcedExpressionMode
    }

    constructor(element: PsiElement, params: SliceAnalysisParams) : super(element, params) {
        this.lambdaLevel = 0
        this.forcedExpressionMode = false
    }

    override fun copy(): KotlinSliceUsage {
        val element = usageInfo.element!!
        if (parent == null) return KotlinSliceUsage(element, params)
        return KotlinSliceUsage(element, parent, lambdaLevel, forcedExpressionMode)
    }

    public override fun processUsagesFlownDownTo(element: PsiElement, uniqueProcessor: Processor<SliceUsage>) {
        InflowSlicer(element as? KtExpression ?: return, uniqueProcessor, this).processChildren()
    }

    public override fun processUsagesFlownFromThe(element: PsiElement, uniqueProcessor: Processor<SliceUsage>) {
        OutflowSlicer(element as? KtExpression ?: return, uniqueProcessor, this).processChildren()
    }
}