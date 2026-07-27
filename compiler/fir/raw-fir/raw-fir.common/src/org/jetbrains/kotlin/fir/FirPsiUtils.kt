/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtRealPsiSourceElement

val FirElement.psi: PsiElement? get() = (source as? KtPsiSourceElement)?.psi
val FirElement.realPsi: PsiElement? get() = (source as? KtRealPsiSourceElement)?.psi
