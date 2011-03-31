package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetExpression;

/**
* @author abreslav
*/
public class ValueInstruction extends InstructionWithNext {

    public ValueInstruction(@NotNull JetExpression expression) {
        super(expression);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitRead(this);
    }

    @Override
    public String toString() {
        return "r(" + expression.getText() + ")";
    }
}
