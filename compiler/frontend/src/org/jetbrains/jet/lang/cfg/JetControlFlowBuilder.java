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
import org.jetbrains.jet.lang.cfg.pseudocode.PseudoValue;
import org.jetbrains.jet.lang.cfg.pseudocode.Pseudocode;
import org.jetbrains.jet.lang.cfg.pseudocode.TypePredicate;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.eval.*;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;

import java.util.List;
import java.util.Map;

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
    void jump(@NotNull Label label, @NotNull JetElement element);
    void jumpOnFalse(@NotNull Label label, @NotNull JetElement element, @Nullable PseudoValue conditionValue);
    void jumpOnTrue(@NotNull Label label, @NotNull JetElement element, @Nullable PseudoValue conditionValue);
    void nondeterministicJump(@NotNull Label label, @NotNull JetElement element, @Nullable PseudoValue inputValue); // Maybe, jump to label
    void nondeterministicJump(@NotNull List<Label> label, @NotNull JetElement element);
    void jumpToError(@NotNull JetElement element);

    void returnValue(@NotNull JetExpression returnExpression, @NotNull PseudoValue returnValue, @NotNull JetElement subroutine);
    void returnNoValue(@NotNull JetReturnExpression returnExpression, @NotNull JetElement subroutine);

    void throwException(@NotNull JetThrowExpression throwExpression, @NotNull PseudoValue thrownValue);

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

    @Nullable
    PseudoValue getBoundValue(@Nullable JetElement element);
    void bindValue(@NotNull PseudoValue value, @NotNull JetElement element);

    void loadUnit(@NotNull JetExpression expression);

    @NotNull
    InstructionWithValue loadConstant(@NotNull JetExpression expression, @Nullable CompileTimeConstant<?> constant);
    @NotNull
    InstructionWithValue createAnonymousObject(@NotNull JetObjectLiteralExpression expression);
    @NotNull
    InstructionWithValue createFunctionLiteral(@NotNull JetFunctionLiteralExpression expression);
    @NotNull
    InstructionWithValue loadStringTemplate(@NotNull JetStringTemplateExpression expression, @NotNull List<PseudoValue> inputValues);

    @NotNull
    MagicInstruction magic(
            @NotNull JetElement instructionElement,
            @Nullable JetElement valueElement,
            @NotNull List<PseudoValue> inputValues,
            @NotNull Map<PseudoValue, TypePredicate> expectedTypes,
            @NotNull MagicKind kind
    );

    @NotNull
    MergeInstruction merge(
            @NotNull JetExpression expression,
            @NotNull List<PseudoValue> inputValues
    );

    @NotNull
    ReadValueInstruction readVariable(
            @NotNull JetExpression expression,
            @NotNull ResolvedCall<?> resolvedCall,
            @NotNull Map<PseudoValue, ReceiverValue> receiverValues
    );

    @NotNull
    CallInstruction call(
            @NotNull JetElement valueElement,
            @NotNull ResolvedCall<?> resolvedCall,
            @NotNull Map<PseudoValue, ReceiverValue> receiverValues,
            @NotNull Map<PseudoValue, ValueParameterDescriptor> arguments
    );

    enum PredefinedOperation {
        AND,
        OR,
        NOT_NULL_ASSERTION
    }
    @NotNull
    OperationInstruction predefinedOperation(
            @NotNull JetExpression expression,
            @NotNull PredefinedOperation operation,
            @NotNull List<PseudoValue> inputValues
    );

    void compilationError(@NotNull JetElement element, @NotNull String message);

    void write(
            @NotNull JetElement assignment,
            @NotNull JetElement lValue,
            @NotNull PseudoValue rValue,
            @NotNull AccessTarget target,
            @NotNull Map<PseudoValue, ReceiverValue> receiverValues);
    
    // Other
    void unsupported(JetElement element);
}
