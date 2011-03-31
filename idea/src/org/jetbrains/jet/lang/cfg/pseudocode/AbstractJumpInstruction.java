package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.jet.lang.cfg.Label;

/**
* @author abreslav
*/
public abstract class AbstractJumpInstruction extends Instruction {
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

    public void setResolvedTarget(Instruction resolvedTarget) {
        this.resolvedTarget = outgoingEdgeTo(resolvedTarget);
    }
}
