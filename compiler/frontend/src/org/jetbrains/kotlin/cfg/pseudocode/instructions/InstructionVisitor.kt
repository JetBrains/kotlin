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

package org.jetbrains.kotlin.cfg.pseudocode.instructions

import org.jetbrains.kotlin.cfg.pseudocode.instructions.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*

open class InstructionVisitor {
    open fun visitAccessInstruction(instruction: AccessValueInstruction) {
        visitInstructionWithNext(instruction)
    }

    open fun visitReadValue(instruction: ReadValueInstruction) {
        visitAccessInstruction(instruction)
    }

    open fun visitLocalFunctionDeclarationInstruction(instruction: LocalFunctionDeclarationInstruction) {
        visitInstructionWithNext(instruction)
    }

    open fun visitVariableDeclarationInstruction(instruction: VariableDeclarationInstruction) {
        visitInstructionWithNext(instruction)
    }

    open fun visitUnconditionalJump(instruction: UnconditionalJumpInstruction) {
        visitJump(instruction)
    }

    open fun visitConditionalJump(instruction: ConditionalJumpInstruction) {
        visitJump(instruction)
    }

    open fun visitReturnValue(instruction: ReturnValueInstruction) {
        visitJump(instruction)
    }

    open fun visitReturnNoValue(instruction: ReturnNoValueInstruction) {
        visitJump(instruction)
    }

    open fun visitThrowExceptionInstruction(instruction: ThrowExceptionInstruction) {
        visitJump(instruction)
    }

    open fun visitNondeterministicJump(instruction: NondeterministicJumpInstruction) {
        visitInstruction(instruction)
    }

    open fun visitSubroutineExit(instruction: SubroutineExitInstruction) {
        visitInstruction(instruction)
    }

    open fun visitSubroutineSink(instruction: SubroutineSinkInstruction) {
        visitInstruction(instruction)
    }

    open fun visitJump(instruction: AbstractJumpInstruction) {
        visitInstruction(instruction)
    }

    open fun visitInstructionWithNext(instruction: InstructionWithNext) {
        visitInstruction(instruction)
    }

    open fun visitInstruction(instruction: Instruction) {
    }

    open fun visitSubroutineEnter(instruction: SubroutineEnterInstruction) {
        visitInstructionWithNext(instruction)
    }

    open fun visitWriteValue(instruction: WriteValueInstruction) {
        visitAccessInstruction(instruction)
    }

    open fun visitLoadUnitValue(instruction: LoadUnitValueInstruction) {
        visitInstructionWithNext(instruction)
    }

    open fun visitOperation(instruction: OperationInstruction) {
        visitInstructionWithNext(instruction)
    }

    open fun visitCallInstruction(instruction: CallInstruction) {
        visitOperation(instruction)
    }

    open fun visitMerge(instruction: MergeInstruction) {
        visitOperation(instruction)
    }

    open fun visitMarkInstruction(instruction: MarkInstruction) {
        visitInstructionWithNext(instruction)
    }

    open fun visitMagic(instruction: MagicInstruction) {
        visitOperation(instruction)
    }
}
