package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;

/**
* @author abreslav
*/
public class ReadUnitValueInstruction extends InstructionWithNext {

    public ReadUnitValueInstruction(@NotNull JetExpression expression) {
        super(expression);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitReadUnitValue(this);
    }

    @Override
    public String toString() {
        return "read (Unit)";
    }
}
