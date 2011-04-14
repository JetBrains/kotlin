package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
* @author abreslav
*/
public abstract class InstructionImpl implements Instruction {
    private Pseudocode owner;
    private final Collection<Instruction> previousInstructions = new LinkedHashSet<Instruction>();

    protected InstructionImpl() {
    }

    @Override
    @NotNull
    public Pseudocode getOwner() {
        return owner;
    }

    @Override
    public void setOwner(@NotNull Pseudocode owner) {
        assert this.owner == null || this.owner == owner;
        this.owner = owner;
    }

    @Override
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

}
