package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.JetProperty;

/**
 * @author svtk
 */
public class VariableDeclarationInstruction extends InstructionWithNext {
    protected VariableDeclarationInstruction(@NotNull JetParameter element) {
        super(element);
    }

    protected VariableDeclarationInstruction(@NotNull JetProperty element) {
        super(element);
    }
    
    public JetDeclaration getVariableDeclarationElement() {
        return (JetDeclaration) element;
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitVariableDeclarationInstruction(this);
    }

    @Override
    public String toString() {
        return "v(" + element.getText() + ")";
    }
}
