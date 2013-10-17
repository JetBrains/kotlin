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

import java.util.List;

/**
* Created by cy6ergn0m on 18.10.13.
*/
class TailRecursionDetectorVisitor extends JetVisitor<TailRecursionGeneratorUtil.TraceStatus<Boolean>, List<? extends PsiElement>> {
    @Nullable
    private Boolean isTail = null;

    @Override
    public TailRecursionGeneratorUtil.TraceStatus<Boolean> visitNamedFunction(
            @NotNull JetNamedFunction function, List<? extends PsiElement> data
    ) {
        return new TailRecursionGeneratorUtil.TraceStatus<Boolean>(true, true);
    }

    @Override
    public TailRecursionGeneratorUtil.TraceStatus<Boolean> visitTryExpression(
            @NotNull JetTryExpression expression, List<? extends PsiElement> track
    ) {
        PsiElement last = last(track);
        if (last instanceof JetCatchClause && expression.getFinallyBlock() != null) {
            return noTailRecursion();
        }

        return status();
    }

    @Override
    public TailRecursionGeneratorUtil.TraceStatus<Boolean> visitCatchSection(
            @NotNull JetCatchClause catchClause, List<? extends PsiElement> data
    ) {
        return status();
    }

    @Override
    public TailRecursionGeneratorUtil.TraceStatus<Boolean> visitFinallySection(
            @NotNull JetFinallySection finallySection, List<? extends PsiElement> data
    ) {
        return status();
    }

    @Override
    public TailRecursionGeneratorUtil.TraceStatus<Boolean> visitBlockExpression(
            @NotNull JetBlockExpression expression, List<? extends PsiElement> data
    ) {

        if (isTail()) return status();

        PsiElement child = last(data);

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

        return status();
    }

    @Override
    public TailRecursionGeneratorUtil.TraceStatus<Boolean> visitWhenExpression(
            @NotNull JetWhenExpression expression, List<? extends PsiElement> data
    ) {
        if (expression.getSubjectExpression() == last(data)) {
            return noTailRecursion();
        }

        return status();
    }

    @Override
    public TailRecursionGeneratorUtil.TraceStatus<Boolean> visitWhenEntry(
            @NotNull JetWhenEntry jetWhenEntry, List<? extends PsiElement> data
    ) {
        return status();
    }

    @Override
    public TailRecursionGeneratorUtil.TraceStatus<Boolean> visitWhenConditionExpression(
            @NotNull JetWhenConditionWithExpression condition, List<? extends PsiElement> data
    ) {
        return noTailRecursion();
    }

    @Override
    public TailRecursionGeneratorUtil.TraceStatus<Boolean> visitIfExpression(
            @NotNull JetIfExpression expression, List<? extends PsiElement> track
    ) {
        if (expression.getCondition() == last(track)) {
            return noTailRecursion();
        }

        return status();
    }

    @Override
    public TailRecursionGeneratorUtil.TraceStatus<Boolean> visitQualifiedExpression(
            @NotNull JetQualifiedExpression expression, List<? extends PsiElement> data
    ) {
        if (!(expression.getReceiverExpression() instanceof JetThisExpression)) {
            return noTailRecursion();
        }

        return status();
    }

    @Override
    public TailRecursionGeneratorUtil.TraceStatus<Boolean> visitReturnExpression(
            @NotNull JetReturnExpression expression, List<? extends PsiElement> data
    ) {
        JetExpression returned = expression.getReturnedExpression();
        if (returned == null) {
            throw new IllegalStateException("Bad case: how could we reach void return?");
        }

        if (returned != data.get(0)) {
            return noTailRecursion();
        }

        return status();
    }

    @Override
    public TailRecursionGeneratorUtil.TraceStatus<Boolean> visitJetElement(
            @NotNull JetElement element, List<? extends PsiElement> data
    ) {
        if (element instanceof JetContainerNode) {
            return status();
        }

        if (isTail == null) {
            return noTailRecursion();
        }

        return status();
    }

    private TailRecursionGeneratorUtil.TraceStatus<Boolean> noTailRecursion() {
        isTail = false;
        return status();
    }

    private TailRecursionGeneratorUtil.TraceStatus<Boolean> status() {
        return new TailRecursionGeneratorUtil.TraceStatus<Boolean>(mightBeTail(), surelyNoTail());
    }

    private boolean surelyNoTail() {
        return isTail != null && !isTail;
    }

    private boolean mightBeTail() {
        return isTail == null || isTail;
    }

    private boolean isTail() {
        return isTail != null && isTail;
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

    @NotNull
    private static PsiElement last(List<? extends PsiElement> track) {
        if (track.isEmpty()) {
            throw new IllegalArgumentException("list is null");
        }
        return track.get(track.size() - 1);
    }
}
