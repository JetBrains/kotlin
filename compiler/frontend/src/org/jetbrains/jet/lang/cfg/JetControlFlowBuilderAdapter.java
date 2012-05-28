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

import java.util.List;

/**
 * @author abreslav
 */
public class JetControlFlowBuilderAdapter implements JetControlFlowBuilder {
    protected @Nullable JetControlFlowBuilder builder;

    @Override
    public void read(@NotNull JetElement element) {
        assert builder != null;
        builder.read(element);
    }

    @Override
    public void readUnit(@NotNull JetExpression expression) {
        assert builder != null;
        builder.readUnit(expression);
    }

    @Override
    @NotNull
    public Label createUnboundLabel() {
        assert builder != null;
        return builder.createUnboundLabel();
    }

    @Override
    public void bindLabel(@NotNull Label label) {
        assert builder != null;
        builder.bindLabel(label);
    }

    @Override
    public void allowDead() {
        assert builder != null;
        builder.allowDead();
    }

    @Override
    public void stopAllowDead() {
        assert builder != null;
        builder.stopAllowDead();
    }

    @Override
    public void jump(@NotNull Label label) {
        assert builder != null;
        builder.jump(label);
    }

    @Override
    public void jumpOnFalse(@NotNull Label label) {
        assert builder != null;
        builder.jumpOnFalse(label);
    }

    @Override
    public void jumpOnTrue(@NotNull Label label) {
        assert builder != null;
        builder.jumpOnTrue(label);
    }

    @Override
    public void nondeterministicJump(Label label) {
        assert builder != null;
        builder.nondeterministicJump(label);
    }

    @Override
    public void nondeterministicJump(List<Label> labels) {
        assert builder != null;
        builder.nondeterministicJump(labels);
    }

    @Override
    public void jumpToError(JetThrowExpression expression) {
        assert builder != null;
        builder.jumpToError(expression);
    }

    @Override
    public void jumpToError(JetExpression nothingExpression) {
        assert builder != null;
        builder.jumpToError(nothingExpression);
    }

    @Override
    public Label getEntryPoint(@NotNull JetElement labelElement) {
        assert builder != null;
        return builder.getEntryPoint(labelElement);
    }

    @Override
    public Label getExitPoint(@NotNull JetElement labelElement) {
        assert builder != null;
        return builder.getExitPoint(labelElement);
    }

    @Override
    public LoopInfo enterLoop(@NotNull JetExpression expression, Label loopExitPoint, Label conditionEntryPoint) {
        assert builder != null;
        return builder.enterLoop(expression, loopExitPoint, conditionEntryPoint);
    }

    @Override
    public void exitLoop(@NotNull JetExpression expression) {
        assert builder != null;
        builder.exitLoop(expression);
    }

    @Override
    @Nullable
    public JetElement getCurrentLoop() {
        assert builder != null;
        return builder.getCurrentLoop();
    }

    @Override
    public void enterTryFinally(@NotNull GenerationTrigger trigger) {
        assert builder != null;
        builder.enterTryFinally(trigger);
    }

    @Override
    public void exitTryFinally() {
        assert builder != null;
        builder.exitTryFinally();
    }

    @Override
    public void enterSubroutine(@NotNull JetDeclaration subroutine) {
        assert builder != null;
        builder.enterSubroutine(subroutine);
    }

    @Override
    public Pseudocode exitSubroutine(@NotNull JetDeclaration subroutine) {
        assert builder != null;
        return builder.exitSubroutine(subroutine);
    }

    @NotNull
    @Override
    public JetElement getCurrentSubroutine() {
        assert builder != null;
        return builder.getCurrentSubroutine();
    }

    @Override
    @Nullable
    public JetElement getReturnSubroutine() {
        assert builder != null;
        return builder.getReturnSubroutine();
    }

    @Override
    public void returnValue(@NotNull JetExpression returnExpression, @NotNull JetElement subroutine) {
        assert builder != null;
        builder.returnValue(returnExpression, subroutine);
    }

    @Override
    public void returnNoValue(@NotNull JetElement returnExpression, @NotNull JetElement subroutine) {
        assert builder != null;
        builder.returnNoValue(returnExpression, subroutine);
    }

    @Override
    public void unsupported(JetElement element) {
        assert builder != null;
        builder.unsupported(element);
    }

    @Override
    public void write(@NotNull JetElement assignment, @NotNull JetElement lValue) {
        assert builder != null;
        builder.write(assignment, lValue);
    }

    @Override
    public void declare(@NotNull JetParameter parameter) {
        assert builder != null;
        builder.declare(parameter);
    }

    @Override
    public void declare(@NotNull JetProperty property) {
        assert builder != null;
        builder.declare(property);
    }
}
