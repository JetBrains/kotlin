/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.FirTypeWithNullability

abstract class FirAbstractAnnotatedType(
    final override val session: FirSession,
    final override val psi: PsiElement?,
    final override val isNullable: Boolean
) : FirTypeWithNullability {
    override val annotations = mutableListOf<FirAnnotationCall>()
}