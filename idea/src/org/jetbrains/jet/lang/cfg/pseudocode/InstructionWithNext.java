package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;

import java.util.Collection;
import java.util.Collections;

/**
 * @author abreslav
 */
public abstract class InstructionWithNext extends JetElementInstruction {
    private Instruction next;

    protected InstructionWithNext(@NotNull JetElement element) {
        super(element);
    }

    public Instruction getNext() {
        return next;
    }

    @NotNull
    @Override
    public Collection<Instruction> getNextInstructions() {
        return Collections.singleton(next);
    }

    public void setNext(Instruction next) {
        this.next = outgoingEdgeTo(next);
    }
}
