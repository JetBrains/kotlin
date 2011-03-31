package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;

/**
 * @author abreslav
 */
public abstract class JetElementInstruction extends Instruction {
    protected final JetElement element;

    public JetElementInstruction(@NotNull JetElement element) {
        this.element = element;
    }

    @NotNull
    public JetElement getElement() {
        return element;
    }
}
