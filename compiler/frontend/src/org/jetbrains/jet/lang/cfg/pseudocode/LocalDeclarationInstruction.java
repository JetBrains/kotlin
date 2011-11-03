package org.jetbrains.jet.lang.cfg.pseudocode;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;

import java.util.ArrayList;
import java.util.Collection;

/**
* @author abreslav
*/
public class LocalDeclarationInstruction extends InstructionWithNext {

    private final Pseudocode body;
    private Instruction sink;

    public LocalDeclarationInstruction(@NotNull JetDeclaration element, Pseudocode body) {
        super(element);
        this.body = body;
    }

    public Pseudocode getBody() {
        return body;
    }

    @NotNull
    @Override
    public Collection<Instruction> getNextInstructions() {
        if (sink != null) {
            ArrayList<Instruction> instructions = Lists.newArrayList(sink);
            instructions.addAll(super.getNextInstructions());
            return instructions;
        }
        return super.getNextInstructions();
    }

    public void setSink(SubroutineSinkInstruction sink) {
        this.sink = outgoingEdgeTo(sink);
    }

    @Override
    public void accept(InstructionVisitor visitor) {
        visitor.visitLocalDeclarationInstruction(this);
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
        return "d" + kind + "(" + element.getText() + ")";
    }
}
