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

package org.jetbrains.kotlin.codegen.range

import org.jetbrains.kotlin.codegen.getAsmRangeElementTypeForPrimitiveRangeOrProgression
import org.jetbrains.kotlin.codegen.isClosedFloatingPointRangeContains
import org.jetbrains.kotlin.codegen.isIntPrimitiveRangeExtensionForInt
import org.jetbrains.kotlin.codegen.isPrimitiveRangeContains
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

abstract class PrimitiveNumberRangeIntrinsicRangeValue(rangeCall: ResolvedCall<out CallableDescriptor>): CallIntrinsicRangeValue(rangeCall) {
    protected val asmElementType = getAsmRangeElementTypeForPrimitiveRangeOrProgression(rangeCall.resultingDescriptor)

    override fun isIntrinsicInCall(resolvedCallForIn: ResolvedCall<out CallableDescriptor>) =
            resolvedCallForIn.resultingDescriptor.let {
                isPrimitiveRangeContains(it) ||
                isClosedFloatingPointRangeContains(it) ||
                isIntPrimitiveRangeExtensionForInt(it)
            }
}