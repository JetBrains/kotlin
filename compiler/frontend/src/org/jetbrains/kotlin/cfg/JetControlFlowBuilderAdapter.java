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

public abstract class JetControlFlowBuilderAdapter implements JetControlFlowBuilder {

    @NotNull
    protected abstract JetControlFlowBuilder getDelegateBuilder();

    @Override
    public void loadUnit(@NotNull KtExpression expression) {
        getDelegateBuilder().loadUnit(expression);
    }

    @NotNull
    @Override
    public InstructionWithValue loadConstant(@NotNull KtExpression expression, @Nullable CompileTimeConstant<?> constant) {
        return getDelegateBuilder().loadConstant(expression, constant);
    }

    @NotNull
    @Override
    public InstructionWithValue createAnonymousObject(@NotNull KtObjectLiteralExpression expression) {
        return getDelegateBuilder().createAnonymousObject(expression);
    }

    @NotNull
    @Override
    public InstructionWithValue createLambda(@NotNull KtFunction expression) {
        return getDelegateBuilder().createLambda(expression);
    }

    @NotNull
    @Override
    public InstructionWithValue loadStringTemplate(@NotNull KtStringTemplateExpression expression, @NotNull List<PseudoValue> inputValues) {
        return getDelegateBuilder().loadStringTemplate(expression, inputValues);
    }

    @NotNull
    @Override
    public MagicInstruction magic(
            @NotNull KtElement instructionElement,
            @Nullable KtElement valueElement,
            @NotNull List<PseudoValue> inputValues,
            @NotNull MagicKind kind
    ) {
        return getDelegateBuilder().magic(instructionElement, valueElement, inputValues, kind);
    }

    @NotNull
    @Override
    public MergeInstruction merge(@NotNull KtExpression expression, @NotNull List<PseudoValue> inputValues) {
        return getDelegateBuilder().merge(expression, inputValues);
    }

    @NotNull
    @Override
    public ReadValueInstruction readVariable(
            @NotNull KtExpression expression,
            @NotNull ResolvedCall<?> resolvedCall,
            @NotNull Map<PseudoValue, ReceiverValue> receiverValues
    ) {
        return getDelegateBuilder().readVariable(expression, resolvedCall, receiverValues);
    }

    @NotNull
    @Override
    public CallInstruction call(
            @NotNull KtElement valueElement,
            @NotNull ResolvedCall<?> resolvedCall,
            @NotNull Map<PseudoValue, ReceiverValue> receiverValues,
            @NotNull Map<PseudoValue, ValueParameterDescriptor> arguments
    ) {
        return getDelegateBuilder().call(valueElement, resolvedCall, receiverValues, arguments);
    }

    @NotNull
    @Override
    public OperationInstruction predefinedOperation(
            @NotNull KtExpression expression,
            @NotNull PredefinedOperation operation,
            @NotNull List<PseudoValue> inputValues
    ) {
        return getDelegateBuilder().predefinedOperation(expression, operation, inputValues);
    }

    @Override
    @NotNull
    public Label createUnboundLabel() {
        return getDelegateBuilder().createUnboundLabel();
    }

    @NotNull
    @Override
    public Label createUnboundLabel(@NotNull String name) {
        return getDelegateBuilder().createUnboundLabel(name);
    }

    @Override
    public void bindLabel(@NotNull Label label) {
        getDelegateBuilder().bindLabel(label);
    }

    @Override
    public void jump(@NotNull Label label, @NotNull KtElement element) {
        getDelegateBuilder().jump(label, element);
    }

    @Override
    public void jumpOnFalse(@NotNull Label label, @NotNull KtElement element, @Nullable PseudoValue conditionValue) {
        getDelegateBuilder().jumpOnFalse(label, element, conditionValue);
    }

    @Override
    public void jumpOnTrue(@NotNull Label label, @NotNull KtElement element, @Nullable PseudoValue conditionValue) {
        getDelegateBuilder().jumpOnTrue(label, element, conditionValue);
    }

    @Override
    public void nondeterministicJump(@NotNull Label label, @NotNull KtElement element, @Nullable PseudoValue inputValue) {
        getDelegateBuilder().nondeterministicJump(label, element, inputValue);
    }

    @Override
    public void nondeterministicJump(@NotNull List<Label> labels, @NotNull KtElement element) {
        getDelegateBuilder().nondeterministicJump(labels, element);
    }

    @Override
    public void jumpToError(@NotNull KtElement element) {
        getDelegateBuilder().jumpToError(element);
    }

    @Override
    public void throwException(@NotNull KtThrowExpression throwExpression, @NotNull PseudoValue thrownValue) {
        getDelegateBuilder().throwException(throwExpression, thrownValue);
    }

    @Override
    @NotNull
    public Label getEntryPoint(@NotNull KtElement labelElement) {
        return getDelegateBuilder().getEntryPoint(labelElement);
    }

    @NotNull
    @Override
    public Label getExitPoint(@NotNull KtElement labelElement) {
        return getDelegateBuilder().getExitPoint(labelElement);
    }

    @NotNull
    @Override
    public Label getConditionEntryPoint(@NotNull KtElement labelElement) {
        return getDelegateBuilder().getConditionEntryPoint(labelElement);
    }

    @NotNull
    @Override
    public LoopInfo enterLoop(@NotNull KtLoopExpression expression) {
        return getDelegateBuilder().enterLoop(expression);
    }

    @Override
    public void enterLoopBody(@NotNull KtLoopExpression expression) {
        getDelegateBuilder().enterLoopBody(expression);
    }

    @Override
    public void exitLoopBody(@NotNull KtLoopExpression expression) {
        getDelegateBuilder().exitLoopBody(expression);
    }

    @Override
    @Nullable
    public KtLoopExpression getCurrentLoop() {
        return getDelegateBuilder().getCurrentLoop();
    }

    @Override
    public void enterTryFinally(@NotNull GenerationTrigger trigger) {
        getDelegateBuilder().enterTryFinally(trigger);
    }

    @Override
    public void exitTryFinally() {
        getDelegateBuilder().exitTryFinally();
    }

    @Override
    public void enterSubroutine(@NotNull KtElement subroutine) {
        getDelegateBuilder().enterSubroutine(subroutine);
    }

    @NotNull
    @Override
    public Pseudocode exitSubroutine(@NotNull KtElement subroutine) {
        return getDelegateBuilder().exitSubroutine(subroutine);
    }

    @NotNull
    @Override
    public KtElement getCurrentSubroutine() {
        return getDelegateBuilder().getCurrentSubroutine();
    }

    @Override
    @Nullable
    public KtElement getReturnSubroutine() {
        return getDelegateBuilder().getReturnSubroutine();
    }

    @Override
    public void returnValue(@NotNull KtExpression returnExpression, @NotNull PseudoValue returnValue, @NotNull KtElement subroutine) {
        getDelegateBuilder().returnValue(returnExpression, returnValue, subroutine);
    }

    @Override
    public void returnNoValue(@NotNull KtReturnExpression returnExpression, @NotNull KtElement subroutine) {
        getDelegateBuilder().returnNoValue(returnExpression, subroutine);
    }

    @Override
    public void write(
            @NotNull KtElement assignment,
            @NotNull KtElement lValue,
            @NotNull PseudoValue rValue,
            @NotNull AccessTarget target,
            @NotNull Map<PseudoValue, ReceiverValue> receiverValues) {
        getDelegateBuilder().write(assignment, lValue, rValue, target, receiverValues);
    }

    @Override
    public void declareParameter(@NotNull KtParameter parameter) {
        getDelegateBuilder().declareParameter(parameter);
    }

    @Override
    public void declareVariable(@NotNull KtVariableDeclaration property) {
        getDelegateBuilder().declareVariable(property);
    }

    @Override
    public void declareFunction(@NotNull KtElement subroutine, @NotNull Pseudocode pseudocode) {
        getDelegateBuilder().declareFunction(subroutine, pseudocode);
    }

    @Override
    public void repeatPseudocode(@NotNull Label startLabel, @NotNull Label finishLabel) {
        getDelegateBuilder().repeatPseudocode(startLabel, finishLabel);
    }

    @Override
    public void mark(@NotNull KtElement element) {
        getDelegateBuilder().mark(element);
    }

    @Nullable
    @Override
    public PseudoValue getBoundValue(@Nullable KtElement element) {
        return getDelegateBuilder().getBoundValue(element);
    }

    @Override
    public void bindValue(@NotNull PseudoValue value, @NotNull KtElement element) {
        getDelegateBuilder().bindValue(value, element);
    }

    @NotNull
    @Override
    public PseudoValue newValue(@Nullable KtElement element) {
        return getDelegateBuilder().newValue(element);
    }

    @Override
    public void enterLexicalScope(@NotNull KtElement element) {
        getDelegateBuilder().enterLexicalScope(element);
    }

    @Override
    public void exitLexicalScope(@NotNull KtElement element) {
        getDelegateBuilder().exitLexicalScope(element);
    }
}
