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

public abstract class JetControlFlowBuilderAdapter implements JetControlFlowBuilder {

    @NotNull
    protected abstract JetControlFlowBuilder getDelegateBuilder();

    @Override
    public void loadUnit(@NotNull JetExpression expression) {
        getDelegateBuilder().loadUnit(expression);
    }

    @Override
    public void loadConstant(@NotNull JetExpression expression, @Nullable CompileTimeConstant<?> constant) {
        getDelegateBuilder().loadConstant(expression, constant);
    }

    @Override
    public void createAnonymousObject(@NotNull JetObjectLiteralExpression expression) {
        getDelegateBuilder().createAnonymousObject(expression);
    }

    @Override
    public void createFunctionLiteral(@NotNull JetFunctionLiteralExpression expression) {
        getDelegateBuilder().createFunctionLiteral(expression);
    }

    @Override
    public void loadStringTemplate(@NotNull JetStringTemplateExpression expression) {
        getDelegateBuilder().loadStringTemplate(expression);
    }

    @Override
    public void readThis(@NotNull JetExpression expression, @Nullable ReceiverParameterDescriptor parameterDescriptor) {
        getDelegateBuilder().readThis(expression, parameterDescriptor);
    }

    @Override
    public void readVariable(@NotNull JetExpression expression, @Nullable VariableDescriptor variableDescriptor) {
        getDelegateBuilder().readVariable(expression, variableDescriptor);
    }

    @Override
    public void call(@NotNull JetExpression expression, @NotNull ResolvedCall<?> resolvedCall) {
        getDelegateBuilder().call(expression, resolvedCall);
    }

    @Override
    public void predefinedOperation(@NotNull JetExpression expression, @Nullable PredefinedOperation operation) {
        getDelegateBuilder().predefinedOperation(expression, operation);
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
    public void jump(@NotNull Label label) {
        getDelegateBuilder().jump(label);
    }

    @Override
    public void jumpOnFalse(@NotNull Label label) {
        getDelegateBuilder().jumpOnFalse(label);
    }

    @Override
    public void jumpOnTrue(@NotNull Label label) {
        getDelegateBuilder().jumpOnTrue(label);
    }

    @Override
    public void nondeterministicJump(@NotNull Label label) {
        getDelegateBuilder().nondeterministicJump(label);
    }

    @Override
    public void nondeterministicJump(@NotNull List<Label> labels) {
        getDelegateBuilder().nondeterministicJump(labels);
    }

    @Override
    public void jumpToError() {
        getDelegateBuilder().jumpToError();
    }

    @Override
    public void throwException(@NotNull JetThrowExpression throwExpression) {
        getDelegateBuilder().throwException(throwExpression);
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
    public void returnValue(@NotNull JetExpression returnExpression, @NotNull JetElement subroutine) {
        getDelegateBuilder().returnValue(returnExpression, subroutine);
    }

    @Override
    public void returnNoValue(@NotNull JetElement returnExpression, @NotNull JetElement subroutine) {
        getDelegateBuilder().returnNoValue(returnExpression, subroutine);
    }

    @Override
    public void unsupported(JetElement element) {
        getDelegateBuilder().unsupported(element);
    }

    @Override
    public void write(@NotNull JetElement assignment, @NotNull JetElement lValue) {
        getDelegateBuilder().write(assignment, lValue);
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
}
