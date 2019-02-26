/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.antlr2fir.fir.modifier

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.types.Variance

class TypeParameterModifier(
    session: FirSession,
    psi: PsiElement? = null,

    val varianceModifier: VarianceModifier = VarianceModifier.INVARIANT,
    val reificationModifier: ReificationModifier? = null
) : FirAbstractAnnotatedElement(session, psi)

enum class VarianceModifier{
    IN {
        override fun toVariance(): Variance {
            return Variance.IN_VARIANCE
        }
    },
    OUT {
        override fun toVariance(): Variance {
            return Variance.OUT_VARIANCE
        }
    },
    INVARIANT {
        override fun toVariance(): Variance {
            return Variance.INVARIANT
        }
    };

    abstract fun toVariance(): Variance
}

enum class ReificationModifier{
    REIFIED
}