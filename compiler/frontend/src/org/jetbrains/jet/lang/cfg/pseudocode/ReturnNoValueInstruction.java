package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.Label;
import org.jetbrains.jet.lang.psi.JetElement;

/**
 * @author abreslav
 */
public class ReturnNoValueInstruction extends AbstractJumpInstruction implements  JetElementInstruction {

    private final JetElement element;

    public ReturnNoValueInstruction(@NotNull JetElement element, Label targetLabel) {
        super(targetLabel);
        this.element = element;
    }

    @NotNull
    @Override
    public JetElement getElement() {
        return element;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitReturnNoValue(this);
    }

    @Override
    public String toString() {
        return "ret " + getTargetLabel();
    }
}
