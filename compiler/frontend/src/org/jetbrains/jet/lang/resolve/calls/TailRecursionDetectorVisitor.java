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

public class TailRecursionDetectorVisitor extends JetVisitor<BacktraceVisitorStatus<TailRecursionKind>, VisitorData<TailRecursionKind>> {

    @Override
    public BacktraceVisitorStatus<TailRecursionKind> visitNamedFunction(
            @NotNull JetNamedFunction function, VisitorData<TailRecursionKind> data
    ) {
        return new BacktraceVisitorStatus<TailRecursionKind>(data.data, true);
    }

    @Override
    public BacktraceVisitorStatus<TailRecursionKind> visitTryExpression(
            @NotNull JetTryExpression expression, VisitorData<TailRecursionKind> data
    ) {
        return noTailRecursion(TailRecursionKind.IN_FINALLY);
    }

    @Override
    public BacktraceVisitorStatus<TailRecursionKind> visitCatchSection(
            @NotNull JetCatchClause catchClause, VisitorData<TailRecursionKind> data
    ) {
        return continueTrace(data);
    }

    @Override
    public BacktraceVisitorStatus<TailRecursionKind> visitFinallySection(
            @NotNull JetFinallySection finallySection, VisitorData<TailRecursionKind> data
    ) {
        return continueTrace(data);
    }

    @Override
    public BacktraceVisitorStatus<TailRecursionKind> visitBlockExpression(
            @NotNull JetBlockExpression expression, VisitorData<TailRecursionKind> data
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
    public BacktraceVisitorStatus<TailRecursionKind> visitWhenExpression(
            @NotNull JetWhenExpression expression, VisitorData<TailRecursionKind> data
    ) {
        if (expression.getSubjectExpression() == data.last) {
            return noTailRecursion();
        }

        return continueTrace(data);
    }

    @Override
    public BacktraceVisitorStatus<TailRecursionKind> visitWhenEntry(
            @NotNull JetWhenEntry jetWhenEntry, VisitorData<TailRecursionKind> data
    ) {
        return continueTrace(data);
    }

    @Override
    public BacktraceVisitorStatus<TailRecursionKind> visitWhenConditionExpression(
            @NotNull JetWhenConditionWithExpression condition, VisitorData<TailRecursionKind> data
    ) {
        return noTailRecursion();
    }

    @Override
    public BacktraceVisitorStatus<TailRecursionKind> visitIfExpression(
            @NotNull JetIfExpression expression, VisitorData<TailRecursionKind> data
    ) {
        if (expression.getCondition() == data.last) {
            return noTailRecursion();
        }

        return continueTrace(data);
    }

    @Override
    public BacktraceVisitorStatus<TailRecursionKind> visitQualifiedExpression(
            @NotNull JetQualifiedExpression expression, VisitorData<TailRecursionKind> data
    ) {
        if (!(expression.getReceiverExpression() instanceof JetThisExpression)) {
            return noTailRecursion();
        }

        return continueTrace(data);
    }

    @Override
    public BacktraceVisitorStatus<TailRecursionKind> visitReturnExpression(
            @NotNull JetReturnExpression expression, VisitorData<TailRecursionKind> data
    ) {
        JetExpression returned = expression.getReturnedExpression();
        if (returned == null) {
            throw new IllegalStateException("Bad case: how could we reach void return?");
        }

        if (returned != data.visitedPath.get(0)) {
            return noTailRecursion();
        }

        return new BacktraceVisitorStatus<TailRecursionKind>(TailRecursionKind.IN_RETURN, false);
    }

    @Override
    public BacktraceVisitorStatus<TailRecursionKind> visitJetElement(
            @NotNull JetElement element, VisitorData<TailRecursionKind> state
    ) {
        if (element instanceof JetContainerNode) {
            return continueTrace(state);
        }

        if (state.data == TailRecursionKind.MIGHT_BE) {
            return noTailRecursion();
        }

        return continueTrace(state);
    }

    private static BacktraceVisitorStatus<TailRecursionKind> noTailRecursion() {
        return new BacktraceVisitorStatus<TailRecursionKind>(TailRecursionKind.NON_TAIL, true);
    }

    private static BacktraceVisitorStatus<TailRecursionKind> noTailRecursion(TailRecursionKind status) {
        return new BacktraceVisitorStatus<TailRecursionKind>(status, true);
    }

    private static BacktraceVisitorStatus<TailRecursionKind> continueTrace(VisitorData<TailRecursionKind> data) {
        return continueTrace(data, TailRecursionKind.MIGHT_BE);
    }

    private static BacktraceVisitorStatus<TailRecursionKind> continueTrace(VisitorData<TailRecursionKind> data, TailRecursionKind desiredStatus) {
        TailRecursionKind newStatus = data.data.and(desiredStatus);
        return new BacktraceVisitorStatus<TailRecursionKind>(newStatus, !newStatus.isDoGenerateTailRecursion());
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
