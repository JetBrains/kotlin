/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.pseudocode.Pseudocode;
import org.jetbrains.jet.lang.psi.*;

import java.util.Collection;
import java.util.List;

public interface JetControlFlowBuilder {
    void read(@NotNull JetElement element);
    void readUnit(@NotNull JetExpression expression);

    // General label management
    @NotNull
    Label createUnboundLabel();
    @NotNull
    Label createUnboundLabel(@NotNull String name);

    void bindLabel(@NotNull Label label);

    // Jumps
    void jump(@NotNull Label label);
    void jumpOnFalse(@NotNull Label label);
    void jumpOnTrue(@NotNull Label label);
    void nondeterministicJump(Label label); // Maybe, jump to label
    void nondeterministicJump(List<Label> label);
    void jumpToError();

    // Entry/exit points
    Label getEntryPoint(@NotNull JetElement labelElement);
    Label getExitPoint(@NotNull JetElement labelElement);

    // Loops
    LoopInfo enterLoop(@NotNull JetExpression expression, @Nullable Label loopExitPoint, @Nullable Label conditionEntryPoint);

    void exitLoop(@NotNull JetExpression expression);
    @Nullable
    JetElement getCurrentLoop();

    // Finally
    void enterTryFinally(@NotNull GenerationTrigger trigger);
    void exitTryFinally();

    // Subroutines
    void enterSubroutine(@NotNull JetElement subroutine);

    Pseudocode exitSubroutine(@NotNull JetElement subroutine);

    @NotNull
    JetElement getCurrentSubroutine();
    @Nullable
    JetElement getReturnSubroutine();
    
    void returnValue(@NotNull JetExpression returnExpression, @NotNull JetElement subroutine);

    void returnNoValue(@NotNull JetElement returnExpression, @NotNull JetElement subroutine);
    
    void throwException(@NotNull JetThrowExpression throwExpression);

    void write(@NotNull JetElement assignment, @NotNull JetElement lValue);
    
    void declare(@NotNull JetParameter parameter);
    void declare(@NotNull JetVariableDeclaration property);

    // Other
    void unsupported(JetElement element);

    void repeatPseudocode(@NotNull Label startLabel, @NotNull Label finishLabel);
}
