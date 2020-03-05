/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement

abstract class FirSourceElement

class FirPsiSourceElement(val psi: PsiElement) : FirSourceElement()
class FirLightSourceElement(val element: LighterASTNode) : FirSourceElement()

val FirSourceElement?.psi: PsiElement? get() = (this as? FirPsiSourceElement)?.psi

val FirElement.psi: PsiElement? get() = (source as? FirPsiSourceElement)?.psi

@Suppress("NOTHING_TO_INLINE")
inline fun PsiElement.toFirSourceElement(): FirPsiSourceElement = FirPsiSourceElement(this)

@Suppress("NOTHING_TO_INLINE")
inline fun LighterASTNode.toFirSourceElement(): FirLightSourceElement = FirLightSourceElement(this)

val FirSourceElement?.lightNode: LighterASTNode? get() = (this as? FirLightSourceElement)?.element