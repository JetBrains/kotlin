/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.Variance

class IsArrayOf : IntrinsicMethod() {
    override fun toCallable(fd: FunctionDescriptor, isSuper: Boolean, resolvedCall: ResolvedCall<*>, codegen: ExpressionCodegen): Callable {
        val typeArguments = resolvedCall.typeArguments
        assert(typeArguments.size == 1) { "Expected only one type parameter for Any?.isArrayOf(), got: $typeArguments" }

        val typeMapper = codegen.state.typeMapper
        val method = typeMapper.mapToCallableMethod(fd, false)

        val builtIns = fd.module.builtIns
        val elementType = typeArguments.values.first()
        val arrayKtType = builtIns.getArrayType(Variance.INVARIANT, elementType)
        val arrayType = typeMapper.mapType(arrayKtType)

        return createIntrinsicCallable(method) {
            it.instanceOf(arrayType)
        }
    }
}