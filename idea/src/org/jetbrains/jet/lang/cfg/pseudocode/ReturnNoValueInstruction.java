package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.jet.lang.cfg.Label;

/**
 * @author abreslav
 */
public class ReturnNoValueInstruction extends AbstractJumpInstruction {

    public ReturnNoValueInstruction(Label targetLabel) {
        super(targetLabel);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitReturnNoValue(this);
    }

    @Override
    public String toString() {
        return "ret";
    }
}
