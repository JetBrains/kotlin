/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef

class FirImplicitTypeRefImpl(
    override val session: FirSession,
    override val psi: PsiElement?
) : FirImplicitTypeRef {
    override val annotations: List<FirAnnotationCall>
        get() = emptyList()
}

object FirComputingImplicitTypeRef : FirImplicitTypeRef {
    override val psi: PsiElement?
        get() = null
    override val session: FirSession
        get() = error("Session independent")
    override val annotations: List<FirAnnotationCall>
        get() = emptyList()

}