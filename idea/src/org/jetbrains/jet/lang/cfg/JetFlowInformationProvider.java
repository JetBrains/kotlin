package org.jetbrains.jet.lang.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFunction;

import java.util.Collection;

/**
 * @author abreslav
 */
public interface JetFlowInformationProvider {
    JetFlowInformationProvider ERROR = new JetFlowInformationProvider() {
        @Override
        public void collectReturnedInformation(@NotNull JetFunction function, Collection<JetExpression> returnedExpressions, Collection<JetElement> elementsReturningUnit) {
            throw new UnsupportedOperationException();
        }
    };

    void collectReturnedInformation(@NotNull JetFunction function, Collection<JetExpression> returnedExpressions, Collection<JetElement> elementsReturningUnit);
}
