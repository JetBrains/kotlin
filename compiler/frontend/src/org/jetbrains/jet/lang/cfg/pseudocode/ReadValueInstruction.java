package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;

/**
* @author abreslav
*/
public class ReadValueInstruction extends InstructionWithNext {

    public ReadValueInstruction(@NotNull JetExpression expression) {
        super(expression);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitReadValue(this);
    }

    @Override
    public String toString() {
        return "r(" + element.getText() + ")";
    }
}
