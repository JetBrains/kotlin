package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression;

/**
* @author abreslav
*/
public class FunctionLiteralValueInstruction extends ReadValueInstruction {

    private Pseudocode body;

    public FunctionLiteralValueInstruction(@NotNull JetFunctionLiteralExpression expression) {
        super(expression);
    }

    public Pseudocode getBody() {
        return body;
    }

    public void setBody(Pseudocode body) {
        this.body = body;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitFunctionLiteralValue(this);
    }

    @Override
    public String toString() {
        return "rf(" + element.getText() + ")";
    }
}
