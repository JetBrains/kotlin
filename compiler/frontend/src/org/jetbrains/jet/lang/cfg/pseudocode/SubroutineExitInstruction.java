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

package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;

import java.util.Collection;
import java.util.Collections;

public class SubroutineExitInstruction extends InstructionImpl {
    private final JetElement subroutine;
    private final String debugLabel;
    private SubroutineSinkInstruction sinkInstruction;

    public SubroutineExitInstruction(@NotNull JetElement subroutine, @NotNull String debugLabel) {
        this.subroutine = subroutine;
        this.debugLabel = debugLabel;
    }

    public JetElement getSubroutine() {
        return subroutine;
    }

    public void setSink(SubroutineSinkInstruction instruction) {
        sinkInstruction = (SubroutineSinkInstruction) outgoingEdgeTo(instruction);
    }

    @NotNull
    @Override
    public Collection<Instruction> getNextInstructions() {
        assert sinkInstruction != null;
        return Collections.<Instruction>singleton(sinkInstruction);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitSubroutineExit(this);
    }

    @Override
    public String toString() {
        return debugLabel;
    }

    @NotNull
    @Override
    protected Instruction createCopy() {
        return new SubroutineExitInstruction(subroutine, debugLabel);
    }
}
