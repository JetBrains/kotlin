/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi

class NonLocalFirBuilderContext(val originalDeclaration: FirDeclaration) : Context<PsiElement>() {
    override fun addCapturedTypeParameters(
        status: Boolean,
        declarationSource: KtSourceElement?,
        currentFirTypeParameters: List<FirTypeParameterRef>
    ) {
        if (originalDeclaration is FirTypeParameterRefsOwner && declarationSource?.psi == originalDeclaration.psi) {
            super.addCapturedTypeParameters(status, declarationSource, originalDeclaration.typeParameters)
        } else {
            super.addCapturedTypeParameters(status, declarationSource, currentFirTypeParameters)
        }
    }
}
