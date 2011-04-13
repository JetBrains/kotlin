package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.jet.lang.cfg.Label;

/**
 * @author abreslav
 */
public class ReturnValueInstruction extends AbstractJumpInstruction {

    public ReturnValueInstruction(Label targetLabel) {
        super(targetLabel);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitReturnValue(this);
    }

    @Override
    public String toString() {
        return "ret(*) " + getTargetLabel();
    }
}
