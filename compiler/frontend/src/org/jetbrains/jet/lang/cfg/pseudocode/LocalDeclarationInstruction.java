package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;

/**
* @author abreslav
*/
public class LocalDeclarationInstruction extends ReadValueInstruction {

    private Pseudocode body;

    public LocalDeclarationInstruction(@NotNull JetDeclaration element, Pseudocode body) {
        super(element);
        this.body = body;
    }

    public Pseudocode getBody() {
        return body;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitFunctionLiteralValue(this);
    }

    @Override
    public String toString() {
        String kind = "!";
        if (element instanceof JetFunction) {
            kind = "f";
        }
        else if (element instanceof JetClass) {
            kind = "c";
        }
        else if (element instanceof JetObjectDeclaration) {
            kind = "o";
        }
        return "r" + kind + "(" + element.getText() + ")";
    }
}
