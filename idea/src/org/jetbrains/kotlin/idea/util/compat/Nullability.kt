/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MissingRecentApi", "IncompatibleAPI")

package org.jetbrains.kotlin.idea.util.compat

import com.intellij.codeInsight.Nullability as IntellijNullability
import com.intellij.codeInspection.dataFlow.DfaUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import com.intellij.slicer.SliceLanguageSupportProvider
import com.intellij.slicer.SliceLeafEquality
import com.intellij.slicer.SliceNullnessAnalyzerBase

// BUNCH: 181
typealias Nullability = IntellijNullability

// BUNCH: 181
fun dfaCheckNullability(variable: PsiVariable?, context: PsiElement?): Nullability =
    DfaUtil.checkNullability(variable, context)

// BUNCH: 181
fun dfaInferMethodNullability(method: PsiMethod): Nullability =
    DfaUtil.inferMethodNullability(method)

// BUNCH: 181
abstract class SliceNullnessAnalyzerBaseEx(
    leafEquality: SliceLeafEquality,
    provider: SliceLanguageSupportProvider
) : SliceNullnessAnalyzerBase(leafEquality, provider) {
    override fun checkNullability(element: PsiElement?): Nullability {
        return checkNullabilityEx(element)
    }

    abstract fun checkNullabilityEx(element: PsiElement?): Nullability
}