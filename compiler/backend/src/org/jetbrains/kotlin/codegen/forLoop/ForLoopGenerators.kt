/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.forLoop

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.RangeCodegenUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.org.objectweb.asm.Type

fun ExpressionCodegen.getForLoopGenerator(forExpression: KtForExpression) : AbstractForLoopGenerator {
    getLoopRangeResolvedCall(forExpression, bindingContext)?.let { loopRangeCall ->
        createOptimizedForLoopGeneratorOrNull(forExpression, loopRangeCall)?.let {
            return it
        }
    }

    val loopRange = forExpression.loopRange!!
    val loopRangeType = bindingContext.getType(loopRange)!!
    val asmLoopRangeType = asmType(loopRangeType)

    return when {
        asmLoopRangeType.sort == Type.ARRAY ->
            ForInArrayLoopGenerator(this, forExpression)
        RangeCodegenUtil.isRange(loopRangeType) ->
            ForInRangeInstanceLoopGenerator(this, forExpression)
        RangeCodegenUtil.isProgression(loopRangeType) ->
            ForInProgressionExpressionLoopGenerator(this, forExpression)
        isSubtypeOfCharSequence(loopRangeType, state.module.builtIns) ->
            ForInCharSequenceLoopGenerator(this, forExpression)
        else ->
            IteratorForLoopGenerator(this, forExpression)
    }
}

private fun isSubtypeOfCharSequence(type: KotlinType, builtIns: KotlinBuiltIns) =
        KotlinTypeChecker.DEFAULT.isSubtypeOf(type, builtIns.getBuiltInClassByName(Name.identifier("CharSequence")).defaultType)

private fun ExpressionCodegen.createOptimizedForLoopGeneratorOrNull(
        forExpression: KtForExpression,
        loopRangeCall: ResolvedCall<out CallableDescriptor>
): AbstractForLoopGenerator? {
    val loopRangeCallee = loopRangeCall.resultingDescriptor

    return when {
        RangeCodegenUtil.isPrimitiveNumberRangeTo(loopRangeCallee) ->
            ForInRangeLiteralLoopGenerator(this, forExpression, loopRangeCall)
        RangeCodegenUtil.isPrimitiveNumberDownTo(loopRangeCallee) ->
            ForInDownToProgressionLoopGenerator(this, forExpression, loopRangeCall)
        RangeCodegenUtil.isPrimitiveNumberUntil(loopRangeCallee) ->
            ForInUntilRangeLoopGenerator(this, forExpression, loopRangeCall)
        RangeCodegenUtil.isArrayOrPrimitiveArrayIndices(loopRangeCallee) ->
            ForInArrayIndicesRangeLoopGenerator(this, forExpression, loopRangeCall)
        RangeCodegenUtil.isCollectionIndices(loopRangeCallee) ->
            ForInCollectionIndicesRangeLoopGenerator(this, forExpression, loopRangeCall)
        RangeCodegenUtil.isCharSequenceIndices(loopRangeCallee) ->
            ForInCharSequenceIndicesRangeLoopGenerator(this, forExpression, loopRangeCall)
        else -> null
    }
}

private fun getLoopRangeResolvedCall(forExpression: KtForExpression, bindingContext: BindingContext): ResolvedCall<out CallableDescriptor>? {
    val loopRange = KtPsiUtil.deparenthesize(forExpression.loopRange)

    when (loopRange) {
        is KtQualifiedExpression -> {
            val qualifiedExpression = loopRange as KtQualifiedExpression?
            val selector = qualifiedExpression!!.selectorExpression
            if (selector is KtCallExpression || selector is KtSimpleNameExpression) {
                return selector.getResolvedCall(bindingContext)
            }
        }
        is KtSimpleNameExpression, is KtCallExpression -> return loopRange.getResolvedCall(bindingContext)
        is KtBinaryExpression -> return loopRange.operationReference.getResolvedCall(bindingContext)
    }

    return null
}