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

package org.jetbrains.jet.lang.resolve.calls;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;

public class TailRecursionDetectorVisitor extends JetVisitor<BacktraceVisitorStatus<RecursionStatus>, VisitorData<RecursionStatus>> {

    @Override
    public BacktraceVisitorStatus<RecursionStatus> visitNamedFunction(
            @NotNull JetNamedFunction function, VisitorData<RecursionStatus> data
    ) {
        return new BacktraceVisitorStatus<RecursionStatus>(data.data, true);
    }

    @Override
    public BacktraceVisitorStatus<RecursionStatus> visitTryExpression(
            @NotNull JetTryExpression expression, VisitorData<RecursionStatus> data
    ) {
        return noTailRecursion(RecursionStatus.FOUND_IN_FINALLY);
    }

    @Override
    public BacktraceVisitorStatus<RecursionStatus> visitCatchSection(
            @NotNull JetCatchClause catchClause, VisitorData<RecursionStatus> data
    ) {
        return continueTrace(data);
    }

    @Override
    public BacktraceVisitorStatus<RecursionStatus> visitFinallySection(
            @NotNull JetFinallySection finallySection, VisitorData<RecursionStatus> data
    ) {
        return continueTrace(data);
    }

    @Override
    public BacktraceVisitorStatus<RecursionStatus> visitBlockExpression(
            @NotNull JetBlockExpression expression, VisitorData<RecursionStatus> data
    ) {

        if (data.data.isReturn()) return continueTrace(data);

        PsiElement child = data.last;

        JetElement last = findLastJetElement(expression.getLastChild());
        if (last != child) { // if last statement
            if (!(last instanceof JetReturnExpression)) {
                return noTailRecursion();
            }

            JetReturnExpression returnExpression = (JetReturnExpression) last;
            if (returnExpression.getReturnedExpression() != null) {
                return noTailRecursion();
            }

            if (findLastJetElement(returnExpression.getPrevSibling()) != child) {
                return noTailRecursion();
            }
        }

        return continueTrace(data);
    }

    @Override
    public BacktraceVisitorStatus<RecursionStatus> visitWhenExpression(
            @NotNull JetWhenExpression expression, VisitorData<RecursionStatus> data
    ) {
        if (expression.getSubjectExpression() == data.last) {
            return noTailRecursion();
        }

        return continueTrace(data);
    }

    @Override
    public BacktraceVisitorStatus<RecursionStatus> visitWhenEntry(
            @NotNull JetWhenEntry jetWhenEntry, VisitorData<RecursionStatus> data
    ) {
        return continueTrace(data);
    }

    @Override
    public BacktraceVisitorStatus<RecursionStatus> visitWhenConditionExpression(
            @NotNull JetWhenConditionWithExpression condition, VisitorData<RecursionStatus> data
    ) {
        return noTailRecursion();
    }

    @Override
    public BacktraceVisitorStatus<RecursionStatus> visitIfExpression(
            @NotNull JetIfExpression expression, VisitorData<RecursionStatus> data
    ) {
        if (expression.getCondition() == data.last) {
            return noTailRecursion();
        }

        return continueTrace(data);
    }

    @Override
    public BacktraceVisitorStatus<RecursionStatus> visitQualifiedExpression(
            @NotNull JetQualifiedExpression expression, VisitorData<RecursionStatus> data
    ) {
        if (!(expression.getReceiverExpression() instanceof JetThisExpression)) {
            return noTailRecursion();
        }

        return continueTrace(data);
    }

    @Override
    public BacktraceVisitorStatus<RecursionStatus> visitReturnExpression(
            @NotNull JetReturnExpression expression, VisitorData<RecursionStatus> data
    ) {
        JetExpression returned = expression.getReturnedExpression();
        if (returned == null) {
            throw new IllegalStateException("Bad case: how could we reach void return?");
        }

        if (returned != data.visitedPath.get(0)) {
            return noTailRecursion();
        }

        return new BacktraceVisitorStatus<RecursionStatus>(RecursionStatus.FOUND_IN_RETURN, false);
    }

    @Override
    public BacktraceVisitorStatus<RecursionStatus> visitJetElement(
            @NotNull JetElement element, VisitorData<RecursionStatus> state
    ) {
        if (element instanceof JetContainerNode) {
            return continueTrace(state);
        }

        if (state.data == RecursionStatus.MIGHT_BE) {
            return noTailRecursion();
        }

        return continueTrace(state);
    }

    private static BacktraceVisitorStatus<RecursionStatus> noTailRecursion() {
        return new BacktraceVisitorStatus<RecursionStatus>(RecursionStatus.NO_TAIL, true);
    }

    private static BacktraceVisitorStatus<RecursionStatus> noTailRecursion(RecursionStatus status) {
        return new BacktraceVisitorStatus<RecursionStatus>(status, true);
    }

    private static BacktraceVisitorStatus<RecursionStatus> continueTrace(VisitorData<RecursionStatus> data) {
        return continueTrace(data, RecursionStatus.MIGHT_BE);
    }

    private static BacktraceVisitorStatus<RecursionStatus> continueTrace(VisitorData<RecursionStatus> data, RecursionStatus desiredStatus) {
        RecursionStatus newStatus = data.data.and(desiredStatus);
        return new BacktraceVisitorStatus<RecursionStatus>(newStatus, !newStatus.isDoGenerateTailRecursion());
    }

    @Nullable
    private static JetElement findLastJetElement(@Nullable PsiElement rightNode) {
        PsiElement node = rightNode;
        while (node != null) {
            if (node instanceof JetElement) {
                return (JetElement) node;
            }
            node = node.getPrevSibling();
        }

        return null;
    }
}
