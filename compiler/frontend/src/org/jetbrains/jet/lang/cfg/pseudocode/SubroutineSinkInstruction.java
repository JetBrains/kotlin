package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;

import java.util.Collection;
import java.util.Collections;

/**
 * @author svtk
 */
public class SubroutineSinkInstruction extends InstructionImpl {
    private final JetElement subroutine;
    private final String debugLabel;

    public SubroutineSinkInstruction(@NotNull JetElement subroutine, @NotNull String debugLabel) {
        this.subroutine = subroutine;
        this.debugLabel = debugLabel;
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
        visitor.visitSubroutineSink(this);
    }

    @Override
    public String toString() {
        return debugLabel;
    }
}
