/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirType

class FirAnnotationCallImpl(
    override val session: FirSession,
    override val psi: PsiElement?,
    override val useSiteTarget: AnnotationUseSiteTarget?,
    override val annotationType: FirType
) : FirAnnotationCall {
    override val arguments = mutableListOf<FirExpression>()
}