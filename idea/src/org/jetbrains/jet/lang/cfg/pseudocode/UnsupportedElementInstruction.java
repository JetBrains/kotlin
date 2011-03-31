package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;

/**
 * @author abreslav
 */
public class UnsupportedElementInstruction extends InstructionWithNext {

    protected UnsupportedElementInstruction(@NotNull JetElement element) {
        super(element);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitUnsupportedElementInstruction(this);
    }

    @Override
    public String toString() {
        return "unsupported(" + element + " : " + element.getText() + ")";
    }
}
