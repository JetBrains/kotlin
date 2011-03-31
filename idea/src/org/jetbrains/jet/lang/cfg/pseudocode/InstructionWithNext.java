package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;

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

    public void setNext(Instruction next) {
        this.next = outgoingEdgeTo(next);
    }
}
