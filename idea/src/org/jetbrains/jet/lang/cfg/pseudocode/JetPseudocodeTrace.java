package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;

/**
 * @author abreslav
 */
public interface JetPseudocodeTrace {

    JetPseudocodeTrace EMPTY = new JetPseudocodeTrace() {
        @Override
        public void recordControlFlowData(@NotNull JetElement element, @NotNull Pseudocode pseudocode) {
        }

        @Override
        public void recordRepresentativeInstruction(@NotNull JetElement element, @NotNull Instruction instruction) {

        }

        @Override
        public void close() {
        }
    };

    void recordControlFlowData(@NotNull JetElement element, @NotNull Pseudocode pseudocode);
    void recordRepresentativeInstruction(@NotNull JetElement element, @NotNull Instruction instruction);
    void close();

}
