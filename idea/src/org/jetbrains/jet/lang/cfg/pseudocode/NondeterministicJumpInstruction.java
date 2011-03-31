package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.jet.lang.cfg.Label;

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

    @Override
    public String toString() {
        return "jmp?(" + getTargetLabel().getName() + ")";
    }
}
