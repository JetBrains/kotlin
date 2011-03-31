package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
* @author abreslav
*/
public abstract class Instruction {
    private Collection<Instruction> previousInstructions = new LinkedHashSet<Instruction>();

    public Collection<Instruction> getPreviousInstructions() {
        return previousInstructions;
    }

    @Nullable
    protected Instruction outgoingEdgeTo(@Nullable Instruction target) {
        if (target != null) {
            target.getPreviousInstructions().add(this);
        }
        return target;
    }

    public abstract void accept(InstructionVisitor visitor);
}
