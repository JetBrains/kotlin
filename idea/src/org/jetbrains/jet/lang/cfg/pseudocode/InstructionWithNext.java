package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.jet.lang.psi.JetExpression;

/**
 * @author abreslav
 */
public abstract class InstructionWithNext extends ExpressionInstruction {
    private Instruction next;

    public InstructionWithNext(JetExpression expression) {
        super(expression);
    }

    public Instruction getNext() {
        return next;
    }

    public void setNext(Instruction next) {
        this.next = outgoingEdgeTo(next);
    }
}
