package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;

/**
 * @author abreslav
 */
public interface JetControlFlowDataTraceFactory {
    JetControlFlowDataTraceFactory EMPTY = new JetControlFlowDataTraceFactory() {
        @NotNull
        @Override
        public JetControlFlowDataTrace createTrace(JetElement element) {
            return JetControlFlowDataTrace.EMPTY;
        }
    };

    @NotNull
    JetControlFlowDataTrace createTrace(JetElement element);
}
