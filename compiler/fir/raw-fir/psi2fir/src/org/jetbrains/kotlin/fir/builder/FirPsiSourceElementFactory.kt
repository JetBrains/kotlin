/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElementKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.builder.FirPsiSourceElementFactory
import org.jetbrains.kotlin.toKtPsiSourceElement

abstract class FirPsiSourceElementFactory : FirSessionComponent {
    abstract fun createSourceElement(psi: PsiElement, kind: KtSourceElementKind = KtRealSourceElementKind): KtPsiSourceElement
}

val FirSession.sourceElementFactory: FirPsiSourceElementFactory by FirSession.sessionComponentAccessor()

fun PsiElement.toKtPsiSourceElement(
    session: FirSession,
    kind: KtSourceElementKind = KtRealSourceElementKind
): KtPsiSourceElement {
    return session.sourceElementFactory.createSourceElement(this, kind)
}

object FirPsiSourceElementWithFixedPsiFactory : FirPsiSourceElementFactory() {
    override fun createSourceElement(psi: PsiElement, kind: KtSourceElementKind): KtPsiSourceElement {
        return psi.toKtPsiSourceElement(kind)
    }
}
