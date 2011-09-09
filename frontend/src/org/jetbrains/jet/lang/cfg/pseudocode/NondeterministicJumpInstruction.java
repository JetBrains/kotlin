package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.Label;

import java.util.Arrays;
import java.util.Collection;

/**
* @author abreslav
*/
public class NondeterministicJumpInstruction extends AbstractJumpInstruction {

    private Instruction next;

    public NondeterministicJumpInstruction(Label targetLabel) {
        super(targetLabel);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitNondeterministicJump(this);
    }

    public Instruction getNext() {
        return next;
    }

    public void setNext(Instruction next) {
        this.next = next;
    }

    @NotNull
    @Override
    public Collection<Instruction> getNextInstructions() {
        return Arrays.asList(getResolvedTarget(), getNext());
    }

    @Override
    public String toString() {
        return "jmp?(" + getTargetLabel().getName() + ")";
    }
}
