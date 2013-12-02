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

public class JetControlFlowBuilderAdapter implements JetControlFlowBuilder {
    protected JetControlFlowBuilder builder;

    @Override
    public void loadUnit(@NotNull JetExpression expression) {
        builder.loadUnit(expression);
    }

    @Override
    public void loadConstant(@NotNull JetExpression expression, @Nullable CompileTimeConstant<?> constant) {
        builder.loadConstant(expression, constant);
    }

    @Override
    public void createAnonymousObject(@NotNull JetObjectLiteralExpression expression) {
        builder.createAnonymousObject(expression);
    }

    @Override
    public void loadStringTemplate(@NotNull JetStringTemplateExpression expression) {
        builder.loadStringTemplate(expression);
    }

    @Override
    public void readThis(@NotNull JetExpression expression, @Nullable ReceiverParameterDescriptor parameterDescriptor) {
        builder.readThis(expression, parameterDescriptor);
    }

    @Override
    public void readVariable(@NotNull JetExpression expression, @Nullable VariableDescriptor variableDescriptor) {
        builder.readVariable(expression, variableDescriptor);
    }

    @Override
    public void call(@NotNull JetExpression expression, @NotNull ResolvedCall<?> resolvedCall) {
        builder.call(expression, resolvedCall);
    }

    @Override
    public void predefinedOperation(@NotNull JetExpression expression, @Nullable PredefinedOperation operation) {
        builder.predefinedOperation(expression, operation);
    }

    @Override
    public void compilationError(@NotNull JetElement element, @NotNull String message) {
        builder.compilationError(element, message);
    }

    @Override
    @NotNull
    public Label createUnboundLabel() {
        return builder.createUnboundLabel();
    }

    @NotNull
    @Override
    public Label createUnboundLabel(@NotNull String name) {
        return builder.createUnboundLabel(name);
    }

    @Override
    public void bindLabel(@NotNull Label label) {
        builder.bindLabel(label);
    }

    @Override
    public void jump(@NotNull Label label) {
        builder.jump(label);
    }

    @Override
    public void jumpOnFalse(@NotNull Label label) {
        builder.jumpOnFalse(label);
    }

    @Override
    public void jumpOnTrue(@NotNull Label label) {
        builder.jumpOnTrue(label);
    }

    @Override
    public void nondeterministicJump(@NotNull Label label) {
        builder.nondeterministicJump(label);
    }

    @Override
    public void nondeterministicJump(@NotNull List<Label> labels) {
        builder.nondeterministicJump(labels);
    }

    @Override
    public void jumpToError() {
        builder.jumpToError();
    }

    @Override
    public void throwException(@NotNull JetThrowExpression throwExpression) {
        builder.throwException(throwExpression);
    }

    @Override
    @NotNull
    public Label getEntryPoint(@NotNull JetElement labelElement) {
        return builder.getEntryPoint(labelElement);
    }

    @NotNull
    @Override
    public Label getExitPoint(@NotNull JetElement labelElement) {
        return builder.getExitPoint(labelElement);
    }

    @Override
    public LoopInfo enterLoop(@NotNull JetExpression expression, @Nullable Label loopExitPoint, Label conditionEntryPoint) {
        return builder.enterLoop(expression, loopExitPoint, conditionEntryPoint);
    }

    @Override
    public void exitLoop(@NotNull JetExpression expression) {
        builder.exitLoop(expression);
    }

    @Override
    @Nullable
    public JetElement getCurrentLoop() {
        return builder.getCurrentLoop();
    }

    @Override
    public void enterTryFinally(@NotNull GenerationTrigger trigger) {
        builder.enterTryFinally(trigger);
    }

    @Override
    public void exitTryFinally() {
        builder.exitTryFinally();
    }

    @Override
    public void enterSubroutine(@NotNull JetElement subroutine) {
        builder.enterSubroutine(subroutine);
    }

    @NotNull
    @Override
    public Pseudocode exitSubroutine(@NotNull JetElement subroutine) {
        return builder.exitSubroutine(subroutine);
    }

    @NotNull
    @Override
    public JetElement getCurrentSubroutine() {
        return builder.getCurrentSubroutine();
    }

    @Override
    @Nullable
    public JetElement getReturnSubroutine() {
        return builder.getReturnSubroutine();
    }

    @Override
    public void returnValue(@NotNull JetExpression returnExpression, @NotNull JetElement subroutine) {
        builder.returnValue(returnExpression, subroutine);
    }

    @Override
    public void returnNoValue(@NotNull JetElement returnExpression, @NotNull JetElement subroutine) {
        builder.returnNoValue(returnExpression, subroutine);
    }

    @Override
    public void unsupported(JetElement element) {
        builder.unsupported(element);
    }

    @Override
    public void write(@NotNull JetElement assignment, @NotNull JetElement lValue) {
        builder.write(assignment, lValue);
    }

    @Override
    public void declareParameter(@NotNull JetParameter parameter) {
        builder.declareParameter(parameter);
    }

    @Override
    public void declareVariable(@NotNull JetVariableDeclaration property) {
        builder.declareVariable(property);
    }

    @Override
    public void declareFunction(@NotNull JetElement subroutine, @NotNull Pseudocode pseudocode) {
        builder.declareFunction(subroutine, pseudocode);
    }

    @Override
    public void repeatPseudocode(@NotNull Label startLabel, @NotNull Label finishLabel) {
        builder.repeatPseudocode(startLabel, finishLabel);
    }
}
