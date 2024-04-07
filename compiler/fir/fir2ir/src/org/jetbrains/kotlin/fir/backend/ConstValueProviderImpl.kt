/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.serialization.constant.ConstValueProvider
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.name.Name

class ConstValueProviderImpl(
    components: Fir2IrComponents,
) : ConstValueProvider() {
    override val session: FirSession = components.session
    override val evaluatedConstTracker: EvaluatedConstTracker = components.configuration.evaluatedConstTracker

    override fun findConstantValueFor(firExpression: FirExpression?): ConstantValue<*>? {
        val firFile = processingFirFile
        if (firExpression == null || firFile == null) return null

        // We can't evaluate vararg expression, only its arguments. We can accidentally find a const result for vararg
        // when there is only one argument, they both are going to have the same offset.
        if (firExpression is FirVarargArgumentsExpression) return null

        val fileName = firFile.packageFqName.child(Name.identifier(firFile.name)).asString()
        val (start, end) = firExpression.getCorrespondingIrOffset() ?: return null
        return evaluatedConstTracker.load(start, end, fileName)
    }

    companion object {
        fun FirExpression.getCorrespondingIrOffset(): Pair<Int, Int>? {
            return if (this is FirQualifiedAccessExpression && this.shouldUseCalleeReferenceAsItsSourceInIr()) {
                val calleeReference = this.calleeReference
                val start = calleeReference.source?.startOffsetSkippingComments() ?: calleeReference.source?.startOffset ?: UNDEFINED_OFFSET
                val end = this.source?.endOffset ?: return null
                start to end
            } else {
                val start = this.source?.startOffset ?: return null
                val end = this.source?.endOffset ?: return null
                start to end
            }
        }
    }
}
