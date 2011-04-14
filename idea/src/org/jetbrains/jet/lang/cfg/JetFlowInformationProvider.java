package org.jetbrains.jet.lang.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.Collection;

/**
 * @author abreslav
 */
public interface JetFlowInformationProvider {
    JetFlowInformationProvider THROW_EXCEPTION = new JetFlowInformationProvider() {
        @Override
        public void collectReturnedInformation(@NotNull JetElement subroutine, @NotNull Collection<JetExpression> returnedExpressions, @NotNull Collection<JetElement> elementsReturningUnit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void collectUnreachableExpressions(@NotNull JetElement subroutine, @NotNull Collection<JetElement> unreachableElements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void collectDominatedExpressions(@NotNull JetExpression dominator, @NotNull Collection<JetElement> dominated) {
            throw new UnsupportedOperationException();
        }
    };

    JetFlowInformationProvider NONE = new JetFlowInformationProvider() {
        @Override
        public void collectReturnedInformation(@NotNull JetElement subroutine, @NotNull Collection<JetExpression> returnedExpressions, @NotNull Collection<JetElement> elementsReturningUnit) {

        }

        @Override
        public void collectUnreachableExpressions(@NotNull JetElement subroutine, @NotNull Collection<JetElement> unreachableElements) {

        }

        @Override
        public void collectDominatedExpressions(@NotNull JetExpression dominator, @NotNull Collection<JetElement> dominated) {

        }
    };

    void collectReturnedInformation(
            @NotNull JetElement subroutine,
            @NotNull Collection<JetExpression> returnedExpressions,
            @NotNull Collection<JetElement> elementsReturningUnit);

    void collectUnreachableExpressions(
            @NotNull JetElement subroutine,
            @NotNull Collection<JetElement> unreachableElements);

    void collectDominatedExpressions(
            @NotNull JetExpression dominator,
            @NotNull Collection<JetElement> dominated);
}
