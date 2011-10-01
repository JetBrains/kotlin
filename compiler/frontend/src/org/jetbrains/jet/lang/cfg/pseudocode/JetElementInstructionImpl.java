package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;

/**
 * @author abreslav
 */
public abstract class JetElementInstructionImpl extends InstructionImpl implements JetElementInstruction {
    protected final JetElement element;

    public JetElementInstructionImpl(@NotNull JetElement element) {
        this.element = element;
    }

    @NotNull
    @Override
    public JetElement getElement() {
        return element;
    }
}
