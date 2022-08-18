/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisOnEdt

@OptIn(KtAllowAnalysisOnEdt::class)
internal inline fun <E> allowLightClassesOnEdt(action: () -> E): E = allowAnalysisOnEdt(action)

internal fun PsiElement.nonExistentType(): PsiType =
    JavaPsiFacade.getElementFactory(project).createTypeFromText("error.NonExistentClass", this)
