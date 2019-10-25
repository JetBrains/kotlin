/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.PsiElement

abstract class FirSourceElement

class FirPsiSourceElement(val psi: PsiElement) : FirSourceElement()

val FirSourceElement?.psi: PsiElement? get() = (this as? FirPsiSourceElement)?.psi

val FirElement.psi: PsiElement? get() = (source as? FirPsiSourceElement)?.psi

@Suppress("NOTHING_TO_INLINE")
inline fun PsiElement.toFirSourceElement(): FirPsiSourceElement = FirPsiSourceElement(this)