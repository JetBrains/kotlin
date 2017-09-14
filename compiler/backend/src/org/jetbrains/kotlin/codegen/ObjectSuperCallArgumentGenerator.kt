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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.AsmUtil.pushDefaultValueOnStack
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

internal class ObjectSuperCallArgumentGenerator(
        private val parameters: List<JvmMethodParameterSignature>,
        private val iv: InstructionAdapter,
        offset: Int,
        superConstructorCall: ResolvedCall<ConstructorDescriptor>
) : ArgumentGenerator() {

    private val offsets = IntArray(parameters.size) { -1 }

    init {
        var currentOffset = offset
        superConstructorCall.valueArguments.forEach {
            (descriptor, argument) ->
            if (argument !is DefaultValueArgument) {
                val index = descriptor.index
                offsets[index] = currentOffset
                currentOffset += parameters[index].asmType.size
            }
        }
    }

    override fun generate(
            valueArgumentsByIndex: List<ResolvedValueArgument>,
            actualArgs: List<ResolvedValueArgument>,
            calleeDescriptor: CallableDescriptor?
    ): DefaultCallArgs = super.generate(valueArgumentsByIndex, valueArgumentsByIndex, calleeDescriptor)

    public override fun generateExpression(i: Int, argument: ExpressionValueArgument) {
        generateSuperCallArgument(i)
    }

    public override fun generateDefault(i: Int, argument: DefaultValueArgument) {
        val type = parameters[i].asmType
        pushDefaultValueOnStack(type, iv)
    }

    public override fun generateVararg(i: Int, argument: VarargValueArgument) {
        generateSuperCallArgument(i)
    }

    private fun generateSuperCallArgument(i: Int) {
        val type = parameters[i].asmType
        if (offsets[i] == -1) {
            throw AssertionError("Unknown parameter value at index $i with type $type")
        }
        iv.load(offsets[i], type)
    }

    override fun reorderArgumentsIfNeeded(args: List<ArgumentAndDeclIndex>) {

    }
}
