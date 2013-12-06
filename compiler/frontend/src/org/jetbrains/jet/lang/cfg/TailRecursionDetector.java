/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.pseudocode.*;
import org.jetbrains.jet.lang.psi.JetElement;

public class TailRecursionDetector extends InstructionVisitorWithResult<Boolean> implements PseudocodeTraverser.InstructionHandler {
    private final JetElement subroutine;
    private final Instruction start;

    public TailRecursionDetector(@NotNull JetElement subroutine, @NotNull Instruction start) {
        this.subroutine = subroutine;
        this.start = start;
    }

    @Override
    public boolean handle(@NotNull Instruction instruction) {
        return instruction == start || instruction.accept(this);
    }

    @Override
    public Boolean visitInstruction(Instruction instruction) {
        return false;
    }

    @Override
    public Boolean visitSubroutineExit(SubroutineExitInstruction instruction) {
        return !instruction.isError() && instruction.getSubroutine() == subroutine;
    }

    @Override
    public Boolean visitSubroutineSink(SubroutineSinkInstruction instruction) {
        return instruction.getSubroutine() == subroutine;
    }

    @Override
    public Boolean visitJump(AbstractJumpInstruction instruction) {
        return true;
    }

    @Override
    public Boolean visitThrowExceptionInstruction(ThrowExceptionInstruction instruction) {
        return false;
    }

    @Override
    public Boolean visitMarkInstruction(MarkInstruction instruction) {
        return true;
    }
}
