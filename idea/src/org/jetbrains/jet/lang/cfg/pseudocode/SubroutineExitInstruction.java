package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;

import java.util.Collection;
import java.util.Collections;

/**
 * @author abreslav
 */
public class SubroutineExitInstruction extends InstructionImpl {
    private final JetElement subroutine;

    public SubroutineExitInstruction(@NotNull JetElement subroutine) {
        this.subroutine = subroutine;
    }

    public JetElement getSubroutine() {
        return subroutine;
    }

    @NotNull
    @Override
    public Collection<Instruction> getNextInstructions() {
        return Collections.emptyList();
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
