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
        public JetPseudocodeTrace createTrace(JetElement element) {
            return JetPseudocodeTrace.EMPTY;
        }
    };

    @NotNull
    JetPseudocodeTrace createTrace(JetElement element);
}
