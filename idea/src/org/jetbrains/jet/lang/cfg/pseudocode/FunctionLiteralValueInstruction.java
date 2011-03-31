package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;

/**
* @author abreslav
*/
public class FunctionLiteralValueInstruction extends InstructionWithNext {

    private Pseudocode body;

    public FunctionLiteralValueInstruction(@NotNull JetElement element) {
        super(element);
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
