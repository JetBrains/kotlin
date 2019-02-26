/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.antlr2fir.fir.modifier

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.impl.FirAbstractAnnotatedElement

class TypeProjectionModifier(
    session: FirSession,
    psi: PsiElement? = null,

    val varianceModifier: VarianceModifier = VarianceModifier.INVARIANT
) : FirAbstractAnnotatedElement(session, psi)
