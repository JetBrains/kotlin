/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.cfg.Label;

import java.util.Arrays;
import java.util.Collection;

public class ConditionalJumpInstruction extends AbstractJumpInstruction {
    private final boolean onTrue;
    private Instruction nextOnTrue;
    private Instruction nextOnFalse;

    public ConditionalJumpInstruction(boolean onTrue, Label targetLabel) {
        super(targetLabel);
        this.onTrue = onTrue;
    }

    public boolean onTrue() {
        return onTrue;
    }

    public Instruction getNextOnTrue() {
        return nextOnTrue;
    }

    public void setNextOnTrue(Instruction nextOnTrue) {
        this.nextOnTrue = outgoingEdgeTo(nextOnTrue);
    }

    public Instruction getNextOnFalse() {
        return nextOnFalse;
    }

    public void setNextOnFalse(Instruction nextOnFalse) {
        this.nextOnFalse = outgoingEdgeTo(nextOnFalse);
    }

    @NotNull
    @Override
    public Collection<Instruction> getNextInstructions() {
        return Arrays.asList(getNextOnFalse(), getNextOnTrue());
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitConditionalJump(this);
    }

    @Override
    public String toString() {
        String instr = onTrue ? "jt" : "jf";
        return instr + "(" + getTargetLabel().getName() + ")";
    }

    @Override
    protected AbstractJumpInstruction createCopy(@NotNull Label newLabel) {
        return new ConditionalJumpInstruction(onTrue, newLabel);
    }
}
