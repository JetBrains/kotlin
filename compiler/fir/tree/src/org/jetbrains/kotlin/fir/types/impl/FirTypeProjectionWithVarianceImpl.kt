/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.types.Variance

class FirTypeProjectionWithVarianceImpl(
    override val session: FirSession,
    override val psi: PsiElement?,
    override val variance: Variance,
    override val type: FirType
) : FirTypeProjectionWithVariance