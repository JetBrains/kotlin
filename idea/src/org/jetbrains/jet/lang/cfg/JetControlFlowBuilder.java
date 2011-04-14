package org.jetbrains.jet.lang.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetThrowExpression;

/**
 * @author abreslav
 */
public interface JetControlFlowBuilder {
    void read(@NotNull JetExpression expression);
    void readUnit(@NotNull JetExpression expression);

    // General label management
    @NotNull
    Label createUnboundLabel();

    void bindLabel(@NotNull Label label);

    // Jumps
    void jump(@NotNull Label label);
    void jumpOnFalse(@NotNull Label label);
    void jumpOnTrue(@NotNull Label label);
    void nondeterministicJump(Label label); // Maybe, jump to label
    void jumpToError(@NotNull JetThrowExpression expression);

    // Entry/exit points
    Label getEntryPoint(@NotNull JetElement labelElement);
    Label getExitPoint(@NotNull JetElement labelElement);

    // Loops
    Label enterLoop(@NotNull JetExpression expression, Label loopExitPoint);

    void exitLoop(@NotNull JetExpression expression);
    @Nullable
    JetElement getCurrentLoop();

    // Finally
    void enterTryFinally(@NotNull GenerationTrigger trigger);
    void exitTryFinally();

    // Subroutines
    void enterSubroutine(@NotNull JetElement subroutine, boolean isFunctionLiteral);

    void exitSubroutine(@NotNull JetElement subroutine, boolean functionLiteral);

    @Nullable
    JetElement getCurrentSubroutine();
    void returnValue(@NotNull JetExpression returnExpression, @NotNull JetElement subroutine);

    void returnNoValue(@NotNull JetElement returnExpression, @NotNull JetElement subroutine);

    void write(@NotNull JetElement assignment, @NotNull JetElement lValue);

    // Other
    void unsupported(JetElement element);
}
