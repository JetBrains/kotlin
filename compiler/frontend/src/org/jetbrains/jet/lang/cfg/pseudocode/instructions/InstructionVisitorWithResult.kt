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

package org.jetbrains.jet.lang.cfg.pseudocode.instructions

import org.jetbrains.jet.lang.cfg.pseudocode.instructions.*
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.jumps.*
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.eval.*
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.special.*

public abstract class InstructionVisitorWithResult<R>() {
    public abstract fun visitInstruction(instruction: Instruction): R

    public open fun visitAccessInstruction(instruction: AccessValueInstruction): R {
        return visitInstructionWithNext(instruction)
    }

    public open fun visitReadValue(instruction: ReadValueInstruction): R {
        return visitAccessInstruction(instruction)
    }

    public open fun visitLocalFunctionDeclarationInstruction(instruction: LocalFunctionDeclarationInstruction): R {
        return visitInstructionWithNext(instruction)
    }

    public open fun visitVariableDeclarationInstruction(instruction: VariableDeclarationInstruction): R {
        return visitInstructionWithNext(instruction)
    }

    public open fun visitUnconditionalJump(instruction: UnconditionalJumpInstruction): R {
        return visitJump(instruction)
    }

    public open fun visitConditionalJump(instruction: ConditionalJumpInstruction): R {
        return visitJump(instruction)
    }

    public open fun visitReturnValue(instruction: ReturnValueInstruction): R {
        return visitJump(instruction)
    }

    public open fun visitReturnNoValue(instruction: ReturnNoValueInstruction): R {
        return visitJump(instruction)
    }

    public open fun visitThrowExceptionInstruction(instruction: ThrowExceptionInstruction): R {
        return visitJump(instruction)
    }

    public open fun visitNondeterministicJump(instruction: NondeterministicJumpInstruction): R {
        return visitInstruction(instruction)
    }

    public open fun visitSubroutineExit(instruction: SubroutineExitInstruction): R {
        return visitInstruction(instruction)
    }

    public open fun visitSubroutineSink(instruction: SubroutineSinkInstruction): R {
        return visitInstruction(instruction)
    }

    public open fun visitJump(instruction: AbstractJumpInstruction): R {
        return visitInstruction(instruction)
    }

    public open fun visitInstructionWithNext(instruction: InstructionWithNext): R {
        return visitInstruction(instruction)
    }

    public open fun visitSubroutineEnter(instruction: SubroutineEnterInstruction): R {
        return visitInstructionWithNext(instruction)
    }

    public open fun visitWriteValue(instruction: WriteValueInstruction): R {
        return visitAccessInstruction(instruction)
    }

    public open fun visitLoadUnitValue(instruction: LoadUnitValueInstruction): R {
        return visitInstructionWithNext(instruction)
    }

    public open fun visitOperation(instruction: OperationInstruction): R {
        return visitInstructionWithNext(instruction)
    }

    public open fun visitCallInstruction(instruction: CallInstruction): R {
        return visitOperation(instruction)
    }

    public open fun visitMerge(instruction: MergeInstruction): R {
        return visitOperation(instruction)
    }

    public open fun visitCompilationErrorInstruction(instruction: CompilationErrorInstruction): R {
        return visitInstructionWithNext(instruction)
    }

    public open fun visitMarkInstruction(instruction: MarkInstruction): R {
        return visitInstructionWithNext(instruction)
    }

    public open fun visitMagic(instruction: MagicInstruction): R {
        return visitOperation(instruction)
    }
}
