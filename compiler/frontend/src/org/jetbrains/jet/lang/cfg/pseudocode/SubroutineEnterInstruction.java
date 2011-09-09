package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;

/**
 * @author abreslav
 */
public class SubroutineEnterInstruction extends InstructionWithNext {
    private final JetElement subroutine;

    public SubroutineEnterInstruction(@NotNull JetElement subroutine) {
        super(subroutine);
        this.subroutine = subroutine;
    }

    public JetElement getSubroutine() {
        return subroutine;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitSubroutineEnter(this);
    }

    @Override
    public String toString() {
        return "<START>";
    }
}
