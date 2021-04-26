/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir.modifier

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.ModifierSets.VARIANCE_MODIFIER
import org.jetbrains.kotlin.types.Variance

class TypeProjectionModifier(
    val source: FirSourceElement? = null,
    private val varianceModifiers: MutableList<VarianceModifier> = mutableListOf()
) {
    val annotations: MutableList<FirAnnotationCall> = mutableListOf()

    fun addModifier(modifier: LighterASTNode) {
        val tokenType = modifier.tokenType
        when {
            VARIANCE_MODIFIER.contains(tokenType) -> this.varianceModifiers += VarianceModifier.valueOf(modifier.toString().uppercase())
        }
    }

    fun getVariance(): Variance {
        return when {
            varianceModifiers.contains(VarianceModifier.IN) -> Variance.IN_VARIANCE
            varianceModifiers.contains(VarianceModifier.OUT) -> Variance.OUT_VARIANCE
            else -> Variance.INVARIANT
        }
    }
}
