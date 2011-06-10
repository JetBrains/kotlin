package org.jetbrains.jet.lang.cfg;

import org.jetbrains.jet.lang.psi.JetElement;

/**
* @author abreslav
*/
public class BreakableBlockInfo extends BlockInfo {
    private final JetElement element;
    private final Label entryPoint;
    private final Label exitPoint;

    public BreakableBlockInfo(JetElement element, Label entryPoint, Label exitPoint) {
        this.element = element;
        this.entryPoint = entryPoint;
        this.exitPoint = exitPoint;
    }

    public JetElement getElement() {
        return element;
    }

    public Label getEntryPoint() {
        return entryPoint;
    }

    public Label getExitPoint() {
        return exitPoint;
    }
}
