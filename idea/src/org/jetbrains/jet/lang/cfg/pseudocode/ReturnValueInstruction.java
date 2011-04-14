package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.Label;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;

/**
 * @author abreslav
 */
public class ReturnValueInstruction extends AbstractJumpInstruction implements JetElementInstruction {

    private final JetElement element;

    public ReturnValueInstruction(@NotNull JetExpression returnExpression, @NotNull Label targetLabel) {
        super(targetLabel);
        this.element = returnExpression;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitReturnValue(this);
    }

    @Override
    public String toString() {
        return "ret(*) " + getTargetLabel();
    }

    @NotNull
    @Override
    public JetElement getElement() {
        return element;
    }
}
