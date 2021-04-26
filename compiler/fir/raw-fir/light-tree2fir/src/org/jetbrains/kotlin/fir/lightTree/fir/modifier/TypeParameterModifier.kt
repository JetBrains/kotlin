/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir.modifier

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.REIFICATION_MODIFIER
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.VARIANCE_MODIFIER
import org.jetbrains.kotlin.types.Variance

class TypeParameterModifier(
    private val varianceModifiers: MutableList<VarianceModifier> = mutableListOf(),
    private var reificationModifier: ReificationModifier? = null
) {
    val annotations: MutableList<FirAnnotationCall> = mutableListOf()

    fun addModifier(modifier: LighterASTNode) {
        val tokenType = modifier.tokenType
        when {
            VARIANCE_MODIFIER.contains(tokenType) -> this.varianceModifiers += VarianceModifier.valueOf(modifier.toString().uppercase())
            REIFICATION_MODIFIER.contains(tokenType) -> this.reificationModifier =
                ReificationModifier.valueOf(modifier.toString().uppercase())
        }
    }

    fun getVariance(): Variance {
        return when {
            varianceModifiers.contains(VarianceModifier.OUT) -> Variance.OUT_VARIANCE
            varianceModifiers.contains(VarianceModifier.IN) -> Variance.IN_VARIANCE
            else -> Variance.INVARIANT
        }
    }

    fun hasReified(): Boolean {
        return reificationModifier == ReificationModifier.REIFIED
    }
}
