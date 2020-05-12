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
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.utils.DFS
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

        for ((index, value) in valueArgumentsByIndex.withIndex()) {
            if (value is DefaultValueArgument) {
                actualArgsWithDeclIndex.add(index, ArgumentAndDeclIndex(value, index))
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
                    if (calleeDescriptor?.defaultValueFromJava(declIndex) == true) {
                        generateDefaultJava(declIndex, argument)
                    } else {
                        defaultArgs.mark(declIndex)
                        generateDefault(declIndex, argument)
                    }
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

    protected open fun generateDefaultJava(i: Int, argument: DefaultValueArgument) {
        throw UnsupportedOperationException("Unsupported default java argument #$i: $argument")
    }

    protected open fun generateOther(i: Int, argument: ResolvedValueArgument) {
        throw UnsupportedOperationException("Unsupported value argument #$i: $argument")
    }

    protected open fun reorderArgumentsIfNeeded(args: List<ArgumentAndDeclIndex>) {
        throw UnsupportedOperationException("Unsupported operation")
    }
}

private fun CallableDescriptor.defaultValueFromJava(index: Int): Boolean = DFS.ifAny(
    listOf(this),
    { current -> current.original.overriddenDescriptors.map { it.original } },
    { descriptor ->
        descriptor.original.overriddenDescriptors.isEmpty() &&
                descriptor is JavaCallableMemberDescriptor &&
                descriptor.valueParameters[index].declaresDefaultValue()
    }
)

fun shouldInvokeDefaultArgumentsStub(resolvedCall: ResolvedCall<*>): Boolean {
    val descriptor = resolvedCall.resultingDescriptor
    val valueArgumentsByIndex = resolvedCall.valueArgumentsByIndex ?: return false
    for (index in valueArgumentsByIndex.indices) {
        val resolvedValueArgument = valueArgumentsByIndex[index]
        if (resolvedValueArgument is DefaultValueArgument && !descriptor.defaultValueFromJava(index)) {
            return true
        }
    }
    return false
}

fun getFunctionWithDefaultArguments(functionDescriptor: FunctionDescriptor): FunctionDescriptor {
    if (functionDescriptor.containingDeclaration !is ClassDescriptor) return functionDescriptor
    if (functionDescriptor.overriddenDescriptors.isEmpty()) return functionDescriptor

    // We are calling a function with some arguments mapped as defaults.
    // Multiple override-equivalent functions from different supertypes with (potentially different) default values
    // can't be overridden by any function in a subtype.
    // Also, a function overriding some other function can't introduce default parameter values.
    // Thus, among all overridden functions should be one (and only one) function
    // that doesn't override anything and has parameters with default values.
    return functionDescriptor.overriddenTreeUniqueAsSequence(true)
        .firstOrNull { function ->
            function.kind == CallableMemberDescriptor.Kind.DECLARATION &&
                    function.overriddenDescriptors.isEmpty() &&
                    function.valueParameters.any { valueParameter -> valueParameter.hasDefaultValue() }
        }
        ?: functionDescriptor
}