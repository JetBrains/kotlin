/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.optimization.boxing

import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.kotlin.codegen.optimization.common.BasicValueWrapper
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.Opcodes

class NullabilityInterpreter(insns: InsnList) : BoxingInterpreter(insns) {
    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue) = makeNotNullIfNeeded(insn, super.unaryOperation(insn, value))

    override fun newOperation(insn: AbstractInsnNode) = makeNotNullIfNeeded(insn, super.newOperation(insn))

    override fun isExactValue(value: BasicValue) = super.isExactValue(value) || value is NotNullBasicValue

    override fun createNewBoxing(insn: AbstractInsnNode, type: Type, progressionIterator: ProgressionIteratorBasicValue?) =
            NotNullBasicValue(BasicValue(type))
}

private fun makeNotNullIfNeeded(insn: AbstractInsnNode, value: BasicValue?): BasicValue? =
    when (insn.opcode) {
        Opcodes.ANEWARRAY, Opcodes.NEWARRAY, Opcodes.LDC, Opcodes.NEW -> NotNullBasicValue(value)
        else -> value
    }

class NotNullBasicValue(wrappedValue: BasicValue?) : BasicValueWrapper(wrappedValue) {
    override fun equals(other: Any?): Boolean = other is NotNullBasicValue
    // We do not differ not-nullable values, so we should always return the same hashCode
    // Actually it doesn't really matter because analyzer is not supposed to store values in hashtables
    override fun hashCode() = 0
}
