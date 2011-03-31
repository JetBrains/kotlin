package org.jetbrains.jet.lang.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetBlockExpression;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;

/**
 * @author abreslav
 */
public interface JetControlFlowBuilder {
    void readNode(@NotNull JetExpression expression);

    // General label management
    @NotNull
    Label createUnboundLabel();
    void bindLabel(@NotNull Label label);

    // Jumps
    void jump(@NotNull Label label);
    void jumpOnFalse(@NotNull Label label);
    void jumpOnTrue(@NotNull Label label);

    void nondeterministicJump(Label label); // Maybe, jump to label

    // Entry/exit points
    Label getEntryPoint(@NotNull JetElement labelElement);
    Label getExitPoint(@NotNull JetElement labelElement);

    // Loops
    Label enterLoop(@NotNull JetExpression expression, Label loopExitPoint);
    void exitLoop(@NotNull JetExpression expression);

    @Nullable
    JetElement getCurrentLoop();

    // Finally
    void enterTryFinally(@NotNull JetBlockExpression expression);
    void exitTryFinally();

    // Subroutines
    void enterSubroutine(@NotNull JetElement subroutine, boolean isFunctionLiteral);
    void exitSubroutine(@NotNull JetElement subroutine, boolean functionLiteral);

    @Nullable
    JetElement getCurrentSubroutine();

    void returnValue(@NotNull JetElement subroutine);
    void returnNoValue(@NotNull JetElement subroutine);

    // Other
    void unsupported(JetElement element);
}
