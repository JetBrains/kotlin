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

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

import org.jetbrains.kotlin.codegen.inline.LocalVarRemapper.RemapStatus.*

class LocalVarRemapper(private val params: Parameters, private val additionalShift: Int) {
    private val actualParamsSize: Int
    private val remapValues = arrayOfNulls<StackValue?>(params.argsSizeOnStack)

    init {
        var realSize = 0
        for (info in params) {
            val shift = params.getDeclarationSlot(info)
            if (!info.isSkippedOrRemapped) {
                remapValues[shift] = StackValue.local(realSize, AsmTypes.OBJECT_TYPE)
                realSize += info.getType().size
            }
            else {
                remapValues[shift] = if (info.isRemapped) info.remapValue else null
                if (CapturedParamInfo.isSynthetic(info)) {
                    realSize += info.getType().size
                }
            }
        }

        actualParamsSize = realSize
    }

    private fun doRemap(index: Int): RemapInfo {
        val remappedIndex: Int

        if (index < params.argsSizeOnStack) {
            val info = params.getParameterByDeclarationSlot(index)
            val remapped = remapValues[index]
            if (info.isSkipped || remapped == null) {
                return RemapInfo(info)
            }
            if (info.isRemapped) {
                return RemapInfo(info, remapped, REMAPPED)
            }
            else {
                remappedIndex = (remapped as StackValue.Local).index
            }
        }
        else {
            //captured params are not used directly in this inlined method, they are used in closure
            //except captured ones for default lambdas, they are generated in default body
            remappedIndex = actualParamsSize - params.argsSizeOnStack + index
        }

        return RemapInfo(null, StackValue.local(remappedIndex + additionalShift, AsmTypes.OBJECT_TYPE), SHIFT)
    }

    fun remap(index: Int): RemapInfo {
        val info = doRemap(index)
        if (FAIL == info.status) {
            assert(info.parameterInfo != null) { "Parameter info for $index variable should be not null" }
            throw RuntimeException("Trying to access skipped parameter: " + info.parameterInfo!!.type + " at " + index)
        }
        return info
    }

    fun visitIincInsn(`var`: Int, increment: Int, mv: MethodVisitor) {
        val remap = remap(`var`)
        if (remap.value !is StackValue.Local) {
            throw AssertionError("Remapped value should be a local: ${remap.value}")
        }
        mv.visitIincInsn(remap.value.index, increment)
    }

    fun visitLocalVariable(name: String, desc: String, signature: String?, start: Label, end: Label, index: Int, mv: MethodVisitor) {
        val info = doRemap(index)
        //add entries only for shifted vars
        if (SHIFT == info.status) {
            mv.visitLocalVariable(name, desc, signature, start, end, (info.value as StackValue.Local).index)
        }
    }

    fun visitVarInsn(opcode: Int, `var`: Int, mv: InstructionAdapter) {
        var opcode = opcode
        val remapInfo = remap(`var`)
        val value = remapInfo.value
        if (value is StackValue.Local) {
            val isStore = InlineCodegenUtil.isStoreInstruction(opcode)
            if (remapInfo.parameterInfo != null) {
                //All remapped value parameters can't be rewritten except case of default ones.
                //On remapping default parameter to actual value there is only one instruction that writes to it according to mask value
                //but if such parameter remapped then it passed and this mask branch code never executed
                //TODO add assertion about parameter default value: descriptor is required
                opcode = value.type.getOpcode(if (isStore) Opcodes.ISTORE else Opcodes.ILOAD)
            }
            mv.visitVarInsn(opcode, value.index)
            if (remapInfo.parameterInfo != null && !isStore) {
                StackValue.coerce(value.type, remapInfo.parameterInfo.type, mv)
            }
        }
        else {
            assert(remapInfo.parameterInfo != null) { "Non local value should have parameter info" }
            value!!.put(remapInfo.parameterInfo!!.type, mv)
        }
    }

    enum class RemapStatus {
        SHIFT,
        REMAPPED,
        FAIL
    }

    class RemapInfo(
            @JvmField val parameterInfo: ParameterInfo?,
            @JvmField val value: StackValue? = null,
            @JvmField val status: RemapStatus = FAIL
    )
}
