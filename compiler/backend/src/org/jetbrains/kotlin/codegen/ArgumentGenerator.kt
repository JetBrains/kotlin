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

import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.utils.mapToIndex
import org.jetbrains.org.objectweb.asm.Type
import java.util.*

class ArgumentAndIndex(val arg: ResolvedValueArgument, val declIndex: Int, var type: Type? = null, var reorder: Boolean = false, var tempValue: StackValue? = null)

abstract class ArgumentGenerator {
    /**
     * @return a `List` of bit masks of default arguments that should be passed as last arguments to $default method, if there were
     * * any default arguments, or an empty `List` if there were none
     * *
     * @see kotlin.reflect.jvm.internal.KCallableImpl.callBy
     * @param valueArgumentsByIndex
     * *
     * @param actualArgs
     */
    open fun generate(valueArgumentsByIndex: List<ResolvedValueArgument>, actualArgs: List<ResolvedValueArgument>, codegen: ExpressionCodegen): DefaultCallMask {
        //HACK: see tempVariable in ExpressionCodegen
        val actualArguments = if (actualArgs.isNotEmpty()) actualArgs else valueArgumentsByIndex

        assert(valueArgumentsByIndex.size() == actualArguments.size()) {
            "Value arguments collection should have same size, but ${valueArgumentsByIndex.size()} != ${actualArguments.size()}"
        }

        val arg2Index = valueArgumentsByIndex.mapToIndex()

        val actualArgsWithDeclIndex: ArrayList<ArgumentAndIndex> = ArrayList(actualArguments.filter { it !is DefaultValueArgument }.map {
            ArgumentAndIndex(it, arg2Index[it]!!)
        })

        valueArgumentsByIndex.withIndex().forEach {
            if (it.value is DefaultValueArgument) {
                actualArgsWithDeclIndex.add(it.index, ArgumentAndIndex(it.value, it.index))
            }
        }

        val masks = DefaultCallMask(valueArgumentsByIndex.size())
        var orderChanged = false

        for ((actualIndex, argumentWithDeclIndex) in actualArgsWithDeclIndex.withIndex()) {
            val argument = argumentWithDeclIndex.arg
            val declIndex = argumentWithDeclIndex.declIndex

            argumentWithDeclIndex.type = when (argument) {
                is ExpressionValueArgument -> {
                    generateExpression(declIndex, argument)
                }
                is DefaultValueArgument -> {
                    masks.mark(declIndex)
                    generateDefault(declIndex, argument)
                }
                is VarargValueArgument -> {
                    generateVararg(declIndex, argument)
                }
                else -> {
                    generateOther(declIndex, argument)
                }
            }

            if (actualIndex != declIndex || orderChanged) {
                orderChanged = true
                argumentWithDeclIndex.reorder = true
            }
        }

        reorderArgumentsIfNeeded(actualArgsWithDeclIndex)

        return masks
    }

    protected open fun generateExpression(i: Int, argument: ExpressionValueArgument): Type {
        throw UnsupportedOperationException("Unsupported expression value argument #$i: $argument")
    }

    protected open fun generateDefault(i: Int, argument: DefaultValueArgument): Type {
        throw UnsupportedOperationException("Unsupported default value argument #$i: $argument")
    }

    protected open fun generateVararg(i: Int, argument: VarargValueArgument): Type {
        throw UnsupportedOperationException("Unsupported vararg value argument #$i: $argument")
    }

    protected open fun generateOther(i: Int, argument: ResolvedValueArgument): Type {
        throw UnsupportedOperationException("Unsupported value argument #$i: $argument")
    }

    protected open fun reorderArgumentsIfNeeded(args: ArrayList<ArgumentAndIndex>) {
        throw UnsupportedOperationException("Unsupported operation")
    }
}
