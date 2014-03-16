/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;

import java.util.List;

public interface JetControlFlowBuilder {
    // Subroutines
    void enterSubroutine(@NotNull JetElement subroutine);

    @NotNull
    Pseudocode exitSubroutine(@NotNull JetElement subroutine);

    @NotNull
    JetElement getCurrentSubroutine();
    @Nullable
    JetElement getReturnSubroutine();

    // Lexical scopes
    void enterLexicalScope(@NotNull JetElement element);
    void exitLexicalScope(@NotNull JetElement element);

    // Entry/exit points
    @NotNull
    Label getEntryPoint(@NotNull JetElement labelElement);
    @NotNull
    Label getExitPoint(@NotNull JetElement labelElement);

    // Declarations
    void declareParameter(@NotNull JetParameter parameter);
    void declareVariable(@NotNull JetVariableDeclaration property);
    void declareFunction(@NotNull JetElement subroutine, @NotNull Pseudocode pseudocode);

    // Labels
    @NotNull
    Label createUnboundLabel();
    @NotNull
    Label createUnboundLabel(@NotNull String name);

    void bindLabel(@NotNull Label label);

    // Jumps
    void jump(@NotNull Label label);
    void jumpOnFalse(@NotNull Label label);
    void jumpOnTrue(@NotNull Label label);
    void nondeterministicJump(@NotNull Label label); // Maybe, jump to label
    void nondeterministicJump(@NotNull List<Label> label);
    void jumpToError();

    void returnValue(@NotNull JetExpression returnExpression, @NotNull JetElement subroutine);
    void returnNoValue(@NotNull JetElement returnExpression, @NotNull JetElement subroutine);

    void throwException(@NotNull JetThrowExpression throwExpression);

    // Loops
    LoopInfo enterLoop(@NotNull JetExpression expression, @Nullable Label loopExitPoint, @Nullable Label conditionEntryPoint);

    void exitLoop(@NotNull JetExpression expression);
    @Nullable
    JetElement getCurrentLoop();

    // Try-Finally
    void enterTryFinally(@NotNull GenerationTrigger trigger);
    void exitTryFinally();

    void repeatPseudocode(@NotNull Label startLabel, @NotNull Label finishLabel);

    // Reading values
    void mark(@NotNull JetElement element);

    void loadUnit(@NotNull JetExpression expression);
    void loadConstant(@NotNull JetExpression expression, @Nullable CompileTimeConstant<?> constant);
    void createAnonymousObject(@NotNull JetObjectLiteralExpression expression);
    void createFunctionLiteral(@NotNull JetFunctionLiteralExpression expression);
    void loadStringTemplate(@NotNull JetStringTemplateExpression expression);

    void readThis(@NotNull JetExpression expression, @Nullable ReceiverParameterDescriptor parameterDescriptor);
    void readVariable(@NotNull JetExpression expression, @Nullable VariableDescriptor variableDescriptor);

    void call(@NotNull JetExpression expression, @NotNull ResolvedCall<?> resolvedCall);

    enum PredefinedOperation {
        AND,
        OR,
        NOT_NULL_ASSERTION
    }
    void predefinedOperation(@NotNull JetExpression expression, @Nullable PredefinedOperation operation);

    void compilationError(@NotNull JetElement element, @NotNull String message);

    void write(@NotNull JetElement assignment, @NotNull JetElement lValue);
    
    // Other
    void unsupported(JetElement element);
}
