package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;

/**
 * @author abreslav
 */
public class SubroutineExitInstruction extends Instruction {
    private final JetElement subroutine;

    public SubroutineExitInstruction(@NotNull JetElement subroutine) {
        this.subroutine = subroutine;
    }

    public JetElement getSubroutine() {
        return subroutine;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitSubroutineExit(this);
    }

    @Override
    public String toString() {
        return "<END>";
    }
}
