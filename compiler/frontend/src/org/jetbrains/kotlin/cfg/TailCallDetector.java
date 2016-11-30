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

package org.jetbrains.kotlin.cfg;

import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitorWithResult;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MergeInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.AbstractJumpInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ThrowExceptionInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.MarkInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineExitInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineSinkInstruction;
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraverseInstructionResult;
import org.jetbrains.kotlin.psi.KtElement;

public class TailCallDetector extends InstructionVisitorWithResult<Boolean> implements Function1<Instruction, TraverseInstructionResult> {
    private final KtElement subroutine;
    private final Instruction start;

    public TailCallDetector(@NotNull KtElement subroutine, @NotNull Instruction start) {
        this.subroutine = subroutine;
        this.start = start;
    }

    @Override
    public TraverseInstructionResult invoke(@NotNull Instruction instruction) {
        return instruction == start || instruction.accept(this) ? TraverseInstructionResult.CONTINUE : TraverseInstructionResult.HALT;
    }

    @Override
    public Boolean visitInstruction(@NotNull Instruction instruction) {
        return false;
    }

    @Override
    public Boolean visitSubroutineExit(@NotNull SubroutineExitInstruction instruction) {
        return !instruction.isError() && instruction.getSubroutine() == subroutine;
    }

    @Override
    public Boolean visitSubroutineSink(@NotNull SubroutineSinkInstruction instruction) {
        return instruction.getSubroutine() == subroutine;
    }

    @Override
    public Boolean visitJump(@NotNull AbstractJumpInstruction instruction) {
        return true;
    }

    @Override
    public Boolean visitThrowExceptionInstruction(@NotNull ThrowExceptionInstruction instruction) {
        return false;
    }

    @Override
    public Boolean visitMarkInstruction(@NotNull MarkInstruction instruction) {
        return true;
    }

    @Override
    public Boolean visitMagic(@NotNull MagicInstruction instruction) {
        return instruction.getSynthetic();
    }

    @Override
    public Boolean visitMerge(@NotNull MergeInstruction instruction) {
        return true;
    }
}
