package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author abreslav
 */
public interface Instruction {
    @NotNull
    Pseudocode getOwner();

    void setOwner(@NotNull Pseudocode owner);

    Collection<Instruction> getPreviousInstructions();

    @NotNull
    Collection<Instruction> getNextInstructions();

    void accept(InstructionVisitor visitor);
}
