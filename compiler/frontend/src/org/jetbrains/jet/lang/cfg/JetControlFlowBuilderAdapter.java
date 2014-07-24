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

public abstract class JetControlFlowBuilderAdapter implements JetControlFlowBuilder {

    @NotNull
    protected abstract JetControlFlowBuilder getDelegateBuilder();

    @Override
    public void loadUnit(@NotNull JetExpression expression) {
        getDelegateBuilder().loadUnit(expression);
    }

    @NotNull
    @Override
    public InstructionWithValue loadConstant(@NotNull JetExpression expression, @Nullable CompileTimeConstant<?> constant) {
        return getDelegateBuilder().loadConstant(expression, constant);
    }

    @NotNull
    @Override
    public InstructionWithValue createAnonymousObject(@NotNull JetObjectLiteralExpression expression) {
        return getDelegateBuilder().createAnonymousObject(expression);
    }

    @NotNull
    @Override
    public InstructionWithValue createFunctionLiteral(@NotNull JetFunctionLiteralExpression expression) {
        return getDelegateBuilder().createFunctionLiteral(expression);
    }

    @NotNull
    @Override
    public InstructionWithValue loadStringTemplate(@NotNull JetStringTemplateExpression expression, @NotNull List<PseudoValue> inputValues) {
        return getDelegateBuilder().loadStringTemplate(expression, inputValues);
    }

    @NotNull
    @Override
    public MagicInstruction magic(
            @NotNull JetElement instructionElement,
            @Nullable JetElement valueElement,
            @NotNull List<PseudoValue> inputValues,
            @NotNull Map<PseudoValue, TypePredicate> expectedTypes,
            @NotNull MagicKind kind
    ) {
        return getDelegateBuilder().magic(instructionElement, valueElement, inputValues, expectedTypes, kind);
    }

    @NotNull
    @Override
    public MergeInstruction merge(@NotNull JetExpression expression, @NotNull List<PseudoValue> inputValues) {
        return getDelegateBuilder().merge(expression, inputValues);
    }

    @NotNull
    @Override
    public ReadValueInstruction readVariable(
            @NotNull JetExpression expression,
            @NotNull ResolvedCall<?> resolvedCall,
            @NotNull Map<PseudoValue, ReceiverValue> receiverValues
    ) {
        return getDelegateBuilder().readVariable(expression, resolvedCall, receiverValues);
    }

    @NotNull
    @Override
    public CallInstruction call(
            @NotNull JetElement valueElement,
            @NotNull ResolvedCall<?> resolvedCall,
            @NotNull Map<PseudoValue, ReceiverValue> receiverValues,
            @NotNull Map<PseudoValue, ValueParameterDescriptor> arguments
    ) {
        return getDelegateBuilder().call(valueElement, resolvedCall, receiverValues, arguments);
    }

    @NotNull
    @Override
    public OperationInstruction predefinedOperation(
            @NotNull JetExpression expression,
            @NotNull PredefinedOperation operation,
            @NotNull List<PseudoValue> inputValues
    ) {
        return getDelegateBuilder().predefinedOperation(expression, operation, inputValues);
    }

    @Override
    public void compilationError(@NotNull JetElement element, @NotNull String message) {
        getDelegateBuilder().compilationError(element, message);
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
    public void jump(@NotNull Label label, @NotNull JetElement element) {
        getDelegateBuilder().jump(label, element);
    }

    @Override
    public void jumpOnFalse(@NotNull Label label, @NotNull JetElement element, @Nullable PseudoValue conditionValue) {
        getDelegateBuilder().jumpOnFalse(label, element, conditionValue);
    }

    @Override
    public void jumpOnTrue(@NotNull Label label, @NotNull JetElement element, @Nullable PseudoValue conditionValue) {
        getDelegateBuilder().jumpOnTrue(label, element, conditionValue);
    }

    @Override
    public void nondeterministicJump(@NotNull Label label, @NotNull JetElement element, @Nullable PseudoValue inputValue) {
        getDelegateBuilder().nondeterministicJump(label, element, inputValue);
    }

    @Override
    public void nondeterministicJump(@NotNull List<Label> labels, @NotNull JetElement element) {
        getDelegateBuilder().nondeterministicJump(labels, element);
    }

    @Override
    public void jumpToError(@NotNull JetElement element) {
        getDelegateBuilder().jumpToError(element);
    }

    @Override
    public void throwException(@NotNull JetThrowExpression throwExpression, @NotNull PseudoValue thrownValue) {
        getDelegateBuilder().throwException(throwExpression, thrownValue);
    }

    @Override
    @NotNull
    public Label getEntryPoint(@NotNull JetElement labelElement) {
        return getDelegateBuilder().getEntryPoint(labelElement);
    }

    @NotNull
    @Override
    public Label getExitPoint(@NotNull JetElement labelElement) {
        return getDelegateBuilder().getExitPoint(labelElement);
    }

    @Override
    public LoopInfo enterLoop(@NotNull JetExpression expression, @Nullable Label loopExitPoint, Label conditionEntryPoint) {
        return getDelegateBuilder().enterLoop(expression, loopExitPoint, conditionEntryPoint);
    }

    @Override
    public void exitLoop(@NotNull JetExpression expression) {
        getDelegateBuilder().exitLoop(expression);
    }

    @Override
    @Nullable
    public JetElement getCurrentLoop() {
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
    public void enterSubroutine(@NotNull JetElement subroutine) {
        getDelegateBuilder().enterSubroutine(subroutine);
    }

    @NotNull
    @Override
    public Pseudocode exitSubroutine(@NotNull JetElement subroutine) {
        return getDelegateBuilder().exitSubroutine(subroutine);
    }

    @NotNull
    @Override
    public JetElement getCurrentSubroutine() {
        return getDelegateBuilder().getCurrentSubroutine();
    }

    @Override
    @Nullable
    public JetElement getReturnSubroutine() {
        return getDelegateBuilder().getReturnSubroutine();
    }

    @Override
    public void returnValue(@NotNull JetExpression returnExpression, @NotNull PseudoValue returnValue, @NotNull JetElement subroutine) {
        getDelegateBuilder().returnValue(returnExpression, returnValue, subroutine);
    }

    @Override
    public void returnNoValue(@NotNull JetReturnExpression returnExpression, @NotNull JetElement subroutine) {
        getDelegateBuilder().returnNoValue(returnExpression, subroutine);
    }

    @Override
    public void unsupported(JetElement element) {
        getDelegateBuilder().unsupported(element);
    }

    @Override
    public void write(
            @NotNull JetElement assignment,
            @NotNull JetElement lValue,
            @NotNull PseudoValue rValue,
            @NotNull AccessTarget target,
            @NotNull Map<PseudoValue, ReceiverValue> receiverValues) {
        getDelegateBuilder().write(assignment, lValue, rValue, target, receiverValues);
    }

    @Override
    public void declareParameter(@NotNull JetParameter parameter) {
        getDelegateBuilder().declareParameter(parameter);
    }

    @Override
    public void declareVariable(@NotNull JetVariableDeclaration property) {
        getDelegateBuilder().declareVariable(property);
    }

    @Override
    public void declareFunction(@NotNull JetElement subroutine, @NotNull Pseudocode pseudocode) {
        getDelegateBuilder().declareFunction(subroutine, pseudocode);
    }

    @Override
    public void repeatPseudocode(@NotNull Label startLabel, @NotNull Label finishLabel) {
        getDelegateBuilder().repeatPseudocode(startLabel, finishLabel);
    }

    @Override
    public void mark(@NotNull JetElement element) {
        getDelegateBuilder().mark(element);
    }

    @Nullable
    @Override
    public PseudoValue getBoundValue(@Nullable JetElement element) {
        return getDelegateBuilder().getBoundValue(element);
    }

    @Override
    public void bindValue(@NotNull PseudoValue value, @NotNull JetElement element) {
        getDelegateBuilder().bindValue(value, element);
    }

    @Override
    public void enterLexicalScope(@NotNull JetElement element) {
        getDelegateBuilder().enterLexicalScope(element);
    }

    @Override
    public void exitLexicalScope(@NotNull JetElement element) {
        getDelegateBuilder().exitLexicalScope(element);
    }
}
