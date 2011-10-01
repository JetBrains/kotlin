package org.jetbrains.jet.lang.cfg;

import org.jetbrains.jet.lang.psi.JetElement;

/**
* @author abreslav
*/
public class LoopInfo extends BreakableBlockInfo {
    private final Label bodyEntryPoint;
    private final Label conditionEntryPoint;

    public LoopInfo(JetElement element, Label entryPoint, Label exitPoint, Label bodyEntryPoint, Label conditionEntryPoint) {
        super(element, entryPoint, exitPoint);
        this.bodyEntryPoint = bodyEntryPoint;
        this.conditionEntryPoint = conditionEntryPoint;
    }

    public Label getBodyEntryPoint() {
        return bodyEntryPoint;
    }

    public Label getConditionEntryPoint() {
        return conditionEntryPoint;
    }
}
