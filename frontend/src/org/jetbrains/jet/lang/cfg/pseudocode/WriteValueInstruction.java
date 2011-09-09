package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;

/**
 * @author abreslav
 */
public class WriteValueInstruction extends InstructionWithNext {
    @NotNull
    private final JetElement lValue;

    public WriteValueInstruction(@NotNull JetElement assignment, @NotNull JetElement lValue) {
        super(assignment);
        this.lValue = lValue;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitWriteValue(this);
    }

    @Override
    public String toString() {
        if (lValue instanceof JetNamedDeclaration) {
            JetNamedDeclaration value = (JetNamedDeclaration) lValue;
            return "w(" + value.getName() + ")";
        }
        return "w(" + lValue.getText() + ")";
    }
}
