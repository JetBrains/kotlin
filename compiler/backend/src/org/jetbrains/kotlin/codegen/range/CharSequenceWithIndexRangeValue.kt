/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.range

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.range.forLoop.CharSequenceWithIndexForLoopGenerator
import org.jetbrains.kotlin.codegen.range.forLoop.ForLoopGenerator
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class CharSequenceWithIndexRangeValue(rangeCall: ResolvedCall<out CallableDescriptor>) : AbstractDestructuredPairRangeValue(rangeCall) {

    override fun createDestructuredPairForLoopGenerator(
        codegen: ExpressionCodegen,
        forExpression: KtForExpression,
        loopParameter: KtDestructuringDeclaration,
        rangeCall: ResolvedCall<out CallableDescriptor>
    ): ForLoopGenerator =
        CharSequenceWithIndexForLoopGenerator(
            codegen, forExpression, loopParameter, rangeCall,
            canCacheLength = canCacheLength(rangeCall)
        )

    private fun canCacheLength(rangeCall: ResolvedCall<out CallableDescriptor>): Boolean {
        val receiverType = rangeCall.extensionReceiver?.type ?: return false
        return KotlinBuiltIns.isString(receiverType)
    }

}