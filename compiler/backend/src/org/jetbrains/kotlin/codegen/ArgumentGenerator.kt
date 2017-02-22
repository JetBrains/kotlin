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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.utils.mapToIndex

class ArgumentAndDeclIndex(val arg: ResolvedValueArgument, val declIndex: Int)

abstract class ArgumentGenerator {
    /**
     * @return a `List` of bit masks of default arguments that should be passed as last arguments to $default method, if there were
     * any default arguments, or an empty `List` if there were none
     *
     * @see kotlin.reflect.jvm.internal.KCallableImpl.callBy
     */
    open fun generate(
            valueArgumentsByIndex: List<ResolvedValueArgument>,
            actualArgs: List<ResolvedValueArgument>,
            // may be null for a constructor of an object literal
            calleeDescriptor: CallableDescriptor?
    ): DefaultCallArgs {
        assert(valueArgumentsByIndex.size == actualArgs.size) {
            "Value arguments collection should have same size, but ${valueArgumentsByIndex.size} != ${actualArgs.size}"
        }

        val arg2Index = valueArgumentsByIndex.mapToIndex()

        val actualArgsWithDeclIndex = actualArgs.filter { it !is DefaultValueArgument }.map {
            ArgumentAndDeclIndex(it, arg2Index[it]!!)
        }.toMutableList()

        valueArgumentsByIndex.withIndex().forEach {
            if (it.value is DefaultValueArgument) {
                actualArgsWithDeclIndex.add(it.index, ArgumentAndDeclIndex(it.value, it.index))
            }
        }

        // Use unwrapped version, because additional synthetic parameters can't have default values
        val defaultArgs = DefaultCallArgs(calleeDescriptor?.unwrapFrontendVersion()?.valueParameters?.size ?: 0)

        for (argumentWithDeclIndex in actualArgsWithDeclIndex) {
            val argument = argumentWithDeclIndex.arg
            val declIndex = argumentWithDeclIndex.declIndex

            when (argument) {
                is ExpressionValueArgument -> {
                    generateExpression(declIndex, argument)
                }
                is DefaultValueArgument -> {
                    defaultArgs.mark(declIndex)
                    generateDefault(declIndex, argument)
                }
                is VarargValueArgument -> {
                    generateVararg(declIndex, argument)
                }
                else -> {
                    generateOther(declIndex, argument)
                }
            }
        }

        reorderArgumentsIfNeeded(actualArgsWithDeclIndex)

        return defaultArgs
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

    protected open fun generateOther(i: Int, argument: ResolvedValueArgument) {
        throw UnsupportedOperationException("Unsupported value argument #$i: $argument")
    }

    protected open fun reorderArgumentsIfNeeded(args: List<ArgumentAndDeclIndex>) {
        throw UnsupportedOperationException("Unsupported operation")
    }
}
