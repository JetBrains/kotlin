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

package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;

public class TailRecursionDetectorVisitor extends JetVisitor<TraceStatus<RecursionStatus>, TraceData<RecursionStatus>> {

    @Override
    public TraceStatus<RecursionStatus> visitNamedFunction(
            @NotNull JetNamedFunction function, TraceData<RecursionStatus> data
    ) {
        return new TraceStatus<RecursionStatus>(data.data, true);
    }

    @Override
    public TraceStatus<RecursionStatus> visitTryExpression(
            @NotNull JetTryExpression expression, TraceData<RecursionStatus> data
    ) {
        PsiElement last = data.last;
        if (last instanceof JetCatchClause && expression.getFinallyBlock() != null) {
            return noTailRecursion();
        }
        else if (last == expression.getTryBlock()) {
            return noTailRecursion();
        }
        else if (last == expression.getFinallyBlock()) {
            return continueTrace(data, RecursionStatus.FOUND_IN_FINALLY);
        }

        return continueTrace(data);
    }

    @Override
    public TraceStatus<RecursionStatus> visitCatchSection(
            @NotNull JetCatchClause catchClause, TraceData<RecursionStatus> data
    ) {
        return continueTrace(data);
    }

    @Override
    public TraceStatus<RecursionStatus> visitFinallySection(
            @NotNull JetFinallySection finallySection, TraceData<RecursionStatus> data
    ) {
        return continueTrace(data);
    }

    @Override
    public TraceStatus<RecursionStatus> visitBlockExpression(
            @NotNull JetBlockExpression expression, TraceData<RecursionStatus> data
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
    public TraceStatus<RecursionStatus> visitWhenExpression(
            @NotNull JetWhenExpression expression, TraceData<RecursionStatus> data
    ) {
        if (expression.getSubjectExpression() == data.last) {
            return noTailRecursion();
        }

        return continueTrace(data);
    }

    @Override
    public TraceStatus<RecursionStatus> visitWhenEntry(
            @NotNull JetWhenEntry jetWhenEntry, TraceData<RecursionStatus> data
    ) {
        return continueTrace(data);
    }

    @Override
    public TraceStatus<RecursionStatus> visitWhenConditionExpression(
            @NotNull JetWhenConditionWithExpression condition, TraceData<RecursionStatus> data
    ) {
        return noTailRecursion();
    }

    @Override
    public TraceStatus<RecursionStatus> visitIfExpression(
            @NotNull JetIfExpression expression, TraceData<RecursionStatus> data
    ) {
        if (expression.getCondition() == data.last) {
            return noTailRecursion();
        }

        return continueTrace(data);
    }

    @Override
    public TraceStatus<RecursionStatus> visitQualifiedExpression(
            @NotNull JetQualifiedExpression expression, TraceData<RecursionStatus> data
    ) {
        if (!(expression.getReceiverExpression() instanceof JetThisExpression)) {
            return noTailRecursion();
        }

        return continueTrace(data);
    }

    @Override
    public TraceStatus<RecursionStatus> visitReturnExpression(
            @NotNull JetReturnExpression expression, TraceData<RecursionStatus> data
    ) {
        JetExpression returned = expression.getReturnedExpression();
        if (returned == null) {
            throw new IllegalStateException("Bad case: how could we reach void return?");
        }

        if (returned != data.track.get(0)) {
            return noTailRecursion();
        }

        return new TraceStatus<RecursionStatus>(RecursionStatus.FOUND_IN_RETURN, false);
    }

    @Override
    public TraceStatus<RecursionStatus> visitJetElement(
            @NotNull JetElement element, TraceData<RecursionStatus> state
    ) {
        if (element instanceof JetContainerNode) {
            return continueTrace(state);
        }

        if (state.data == RecursionStatus.MIGHT_BE || state.data ==  RecursionStatus.FOUND_IN_FINALLY) {
            return noTailRecursion();
        }

        return continueTrace(state);
    }

    private static TraceStatus<RecursionStatus> noTailRecursion() {
        return new TraceStatus<RecursionStatus>(RecursionStatus.NO_TAIL, true);
    }

    private static TraceStatus<RecursionStatus> continueTrace(TraceData<RecursionStatus> data) {
        return continueTrace(data, RecursionStatus.MIGHT_BE);
    }

    private static TraceStatus<RecursionStatus> continueTrace(TraceData<RecursionStatus> data, RecursionStatus desiredStatus) {
        RecursionStatus newStatus = data.data.and(desiredStatus);
        return new TraceStatus<RecursionStatus>(newStatus, !newStatus.isDoGenerateTailRecursion());
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
