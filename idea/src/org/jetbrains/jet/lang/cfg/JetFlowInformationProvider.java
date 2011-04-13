package org.jetbrains.jet.lang.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.Collection;

/**
 * @author abreslav
 */
public interface JetFlowInformationProvider {
    JetFlowInformationProvider ERROR = new JetFlowInformationProvider() {
        @Override
        public void collectReturnedInformation(@NotNull JetElement subroutine, Collection<JetExpression> returnedExpressions, Collection<JetElement> elementsReturningUnit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void collectUnreachableExpressions(@NotNull JetElement subroutine, Collection<JetElement> unreachableElements) {
            throw new UnsupportedOperationException();
        }
    };

    void collectReturnedInformation(@NotNull JetElement subroutine, Collection<JetExpression> returnedExpressions, Collection<JetElement> elementsReturningUnit);
    void collectUnreachableExpressions(@NotNull JetElement subroutine, Collection<JetElement> unreachableElements);
}
