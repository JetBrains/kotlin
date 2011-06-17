package org.jetbrains.jet.lang.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetLoopExpression;

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
        public void collectReturnExpressions(@NotNull JetElement subroutine, @NotNull Collection<JetExpression> returnedExpressions) {
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

        @Override
        public boolean isBreakable(JetLoopExpression loop) {
            throw new UnsupportedOperationException();
        }

    };

    JetFlowInformationProvider NONE = new JetFlowInformationProvider() {
        @Override
        public void collectReturnedInformation(@NotNull JetElement subroutine, @NotNull Collection<JetExpression> returnedExpressions, @NotNull Collection<JetElement> elementsReturningUnit) {

        }

        @Override
        public void collectReturnExpressions(@NotNull JetElement subroutine, @NotNull Collection<JetExpression> returnedExpressions) {

        }

        @Override
        public void collectUnreachableExpressions(@NotNull JetElement subroutine, @NotNull Collection<JetElement> unreachableElements) {

        }

        @Override
        public void collectDominatedExpressions(@NotNull JetExpression dominator, @NotNull Collection<JetElement> dominated) {

        }

        @Override
        public boolean isBreakable(JetLoopExpression loop) {
            return false;
        }

    };

    /**
     * Collects expressions returned from the given subroutine and 'return;' expressions
     */
    void collectReturnedInformation(
            @NotNull JetElement subroutine,
            @NotNull Collection<JetExpression> returnedExpressions,
            @NotNull Collection<JetElement> elementsReturningUnit);

    /**
     * Collects all 'return ...' expressions that return from the given subroutine and all the expressions that precede the exit point
     */
    void collectReturnExpressions(@NotNull JetElement subroutine, @NotNull Collection<JetExpression> returnedExpressions);

    void collectUnreachableExpressions(
            @NotNull JetElement subroutine,
            @NotNull Collection<JetElement> unreachableElements);

    void collectDominatedExpressions(
            @NotNull JetExpression dominator,
            @NotNull Collection<JetElement> dominated);

    boolean isBreakable(JetLoopExpression loop);
}
