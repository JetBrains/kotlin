/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue;
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;

import java.util.List;
import java.util.Map;

public interface JetControlFlowBuilder {
    // Subroutines
    void enterSubroutine(@NotNull KtElement subroutine);

    @NotNull
    Pseudocode exitSubroutine(@NotNull KtElement subroutine);

    @NotNull
    KtElement getCurrentSubroutine();
    @Nullable
    KtElement getReturnSubroutine();

    // Lexical scopes
    void enterLexicalScope(@NotNull KtElement element);
    void exitLexicalScope(@NotNull KtElement element);

    // Entry/exit points
    @NotNull
    Label getEntryPoint(@NotNull KtElement labelElement);
    @NotNull
    Label getExitPoint(@NotNull KtElement labelElement);
    @NotNull
    Label getConditionEntryPoint(@NotNull KtElement labelElement);

    // Declarations
    void declareParameter(@NotNull KtParameter parameter);
    void declareVariable(@NotNull KtVariableDeclaration property);
    void declareFunction(@NotNull KtElement subroutine, @NotNull Pseudocode pseudocode);

    // Labels
    @NotNull
    Label createUnboundLabel();
    @NotNull
    Label createUnboundLabel(@NotNull String name);

    void bindLabel(@NotNull Label label);

    // Jumps
    void jump(@NotNull Label label, @NotNull KtElement element);
    void jumpOnFalse(@NotNull Label label, @NotNull KtElement element, @Nullable PseudoValue conditionValue);
    void jumpOnTrue(@NotNull Label label, @NotNull KtElement element, @Nullable PseudoValue conditionValue);
    void nondeterministicJump(@NotNull Label label, @NotNull KtElement element, @Nullable PseudoValue inputValue); // Maybe, jump to label
    void nondeterministicJump(@NotNull List<Label> label, @NotNull KtElement element);
    void jumpToError(@NotNull KtElement element);

    void returnValue(@NotNull KtExpression returnExpression, @NotNull PseudoValue returnValue, @NotNull KtElement subroutine);
    void returnNoValue(@NotNull KtReturnExpression returnExpression, @NotNull KtElement subroutine);

    void throwException(@NotNull KtThrowExpression throwExpression, @NotNull PseudoValue thrownValue);

    // Loops
    @NotNull
    LoopInfo enterLoop(@NotNull KtLoopExpression expression);
    void enterLoopBody(@NotNull KtLoopExpression expression);
    void exitLoopBody(@NotNull KtLoopExpression expression);
    @Nullable
    KtLoopExpression getCurrentLoop();

    // Try-Finally
    void enterTryFinally(@NotNull GenerationTrigger trigger);
    void exitTryFinally();

    void repeatPseudocode(@NotNull Label startLabel, @NotNull Label finishLabel);

    // Reading values
    void mark(@NotNull KtElement element);

    @Nullable
    PseudoValue getBoundValue(@Nullable KtElement element);
    void bindValue(@NotNull PseudoValue value, @NotNull KtElement element);
    @NotNull
    PseudoValue newValue(@Nullable KtElement element);

    void loadUnit(@NotNull KtExpression expression);

    @NotNull
    InstructionWithValue loadConstant(@NotNull KtExpression expression, @Nullable CompileTimeConstant<?> constant);
    @NotNull
    InstructionWithValue createAnonymousObject(@NotNull KtObjectLiteralExpression expression);
    @NotNull
    InstructionWithValue createLambda(@NotNull KtFunction expression);
    @NotNull
    InstructionWithValue loadStringTemplate(@NotNull KtStringTemplateExpression expression, @NotNull List<PseudoValue> inputValues);

    @NotNull
    MagicInstruction magic(
            @NotNull KtElement instructionElement,
            @Nullable KtElement valueElement,
            @NotNull List<PseudoValue> inputValues,
            @NotNull MagicKind kind
    );

    @NotNull
    MergeInstruction merge(
            @NotNull KtExpression expression,
            @NotNull List<PseudoValue> inputValues
    );

    @NotNull
    ReadValueInstruction readVariable(
            @NotNull KtExpression expression,
            @NotNull ResolvedCall<?> resolvedCall,
            @NotNull Map<PseudoValue, ReceiverValue> receiverValues
    );

    @NotNull
    CallInstruction call(
            @NotNull KtElement valueElement,
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
            @NotNull KtExpression expression,
            @NotNull PredefinedOperation operation,
            @NotNull List<PseudoValue> inputValues
    );

    void write(
            @NotNull KtElement assignment,
            @NotNull KtElement lValue,
            @NotNull PseudoValue rValue,
            @NotNull AccessTarget target,
            @NotNull Map<PseudoValue, ReceiverValue> receiverValues);
}
