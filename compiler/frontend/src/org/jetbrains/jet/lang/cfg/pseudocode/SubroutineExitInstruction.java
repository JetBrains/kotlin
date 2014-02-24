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
    private final boolean isError;
    private SubroutineSinkInstruction sinkInstruction;

    public SubroutineExitInstruction(@NotNull JetElement subroutine, @NotNull LexicalScope lexicalScope, boolean isError) {
        super(lexicalScope);
        this.subroutine = subroutine;
        this.isError = isError;
    }

    public JetElement getSubroutine() {
        return subroutine;
    }

    public boolean isError() {
        return isError;
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
    public void accept(@NotNull InstructionVisitor visitor) {
        visitor.visitSubroutineExit(this);
    }

    @Override
    public <R> R accept(@NotNull InstructionVisitorWithResult<R> visitor) {
        return visitor.visitSubroutineExit(this);
    }

    @Override
    public String toString() {
        return isError ? "<ERROR>" : "<END>";
    }

    @NotNull
    @Override
    protected Instruction createCopy() {
        return new SubroutineExitInstruction(subroutine, lexicalScope, isError);
    }
}
