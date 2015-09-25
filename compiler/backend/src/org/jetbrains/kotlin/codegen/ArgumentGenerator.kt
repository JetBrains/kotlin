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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument

import java.util.ArrayList
import java.util.Collections

abstract class ArgumentGenerator {
    /**
     * @return a `List` of bit masks of default arguments that should be passed as last arguments to $default method, if there were
     * * any default arguments, or an empty `List` if there were none
     * *
     * @see kotlin.reflect.jvm.internal.KCallableImpl.callBy
     */
    open fun generate(valueArguments: List<ResolvedValueArgument>): List<Int> {
        val masks = ArrayList<Int>(1)
        var mask = 0
        val n = valueArguments.size()
        for (i in 0..n - 1) {
            if (i != 0 && i % Integer.SIZE == 0) {
                masks.add(mask)
                mask = 0
            }
            val argument = valueArguments.get(i)
            if (argument is ExpressionValueArgument) {
                generateExpression(i, argument)
            }
            else if (argument is DefaultValueArgument) {
                mask = mask or (1 shl (i % Integer.SIZE))
                generateDefault(i, argument)
            }
            else if (argument is VarargValueArgument) {
                generateVararg(i, argument)
            }
            else {
                generateOther(i, argument)
            }
        }

        if (mask == 0 && masks.isEmpty()) {
            return emptyList()
        }

        masks.add(mask)
        return masks
    }

    protected open fun generateExpression(i: Int, argument: ExpressionValueArgument) {
        throw UnsupportedOperationException("Unsupported expression value argument #$i: $argument")
    }

    protected open fun generateDefault(i: Int, argument: DefaultValueArgument) {
        throw UnsupportedOperationException("Unsupported default value argument #$i: $argument")
    }

    protected open fun generateVararg(i: Int, argument: VarargValueArgument) {
        throw UnsupportedOperationException("Unsupported vararg value argument #$i: $argument")
    }

    @SuppressWarnings("MethodMayBeStatic") // is supposed to be overridden
    protected fun generateOther(i: Int, argument: ResolvedValueArgument) {
        throw UnsupportedOperationException("Unsupported value argument #$i: $argument")
    }
}
