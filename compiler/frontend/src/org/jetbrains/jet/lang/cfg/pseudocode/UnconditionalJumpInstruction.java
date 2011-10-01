package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.jet.lang.cfg.Label;

/**
* @author abreslav
*/
public class UnconditionalJumpInstruction extends AbstractJumpInstruction {


    public UnconditionalJumpInstruction(Label targetLabel) {
        super(targetLabel);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitUnconditionalJump(this);
    }

    @Override
    public String toString() {
        return "jmp(" + getTargetLabel().getName() + ")";
    }

}
