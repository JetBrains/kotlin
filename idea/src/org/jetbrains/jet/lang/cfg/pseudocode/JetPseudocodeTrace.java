package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.LoopInfo;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;

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

        @Override
        public void recordLoopInfo(JetExpression expression, LoopInfo blockInfo) {

        }
    };

    void recordControlFlowData(@NotNull JetElement element, @NotNull Pseudocode pseudocode);
    void recordRepresentativeInstruction(@NotNull JetElement element, @NotNull Instruction instruction);
    void close();

    void recordLoopInfo(JetExpression expression, LoopInfo blockInfo);
}
