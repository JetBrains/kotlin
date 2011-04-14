package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.Label;

import java.util.Collection;
import java.util.Collections;

/**
* @author abreslav
*/
public abstract class AbstractJumpInstruction extends InstructionImpl {
    private final Label targetLabel;
    private Instruction resolvedTarget;

    public AbstractJumpInstruction(Label targetLabel) {
        this.targetLabel = targetLabel;
    }

    public Label getTargetLabel() {
        return targetLabel;
    }

    public Instruction getResolvedTarget() {
        return resolvedTarget;
    }

    @NotNull
    @Override
    public Collection<Instruction> getNextInstructions() {
        return Collections.singleton(getResolvedTarget());
    }

    public void setResolvedTarget(Instruction resolvedTarget) {
        this.resolvedTarget = outgoingEdgeTo(resolvedTarget);
    }
}
