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

package org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations;

import com.google.common.base.Predicate;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;

public class BranchedUnfoldingUtils {
    private BranchedUnfoldingUtils() {
    }

    private static boolean checkUnfoldableAssignment(@NotNull JetExpression expression) {
        if (!JetPsiUtil.isAssignment(expression)) return false;

        JetBinaryExpression assignment = (JetBinaryExpression)expression;
        return assignment.getLeft() instanceof JetSimpleNameExpression && JetPsiUtil.isBranchedExpression(assignment.getRight());
    }

    private static boolean checkUnfoldableReturn(@NotNull JetExpression expression) {
        if (!(expression instanceof JetReturnExpression)) return false;

        JetReturnExpression returnExpression = (JetReturnExpression)expression;
        return JetPsiUtil.isBranchedExpression(returnExpression.getReturnedExpression());
    }

    public static boolean checkUnfoldableExpression(@NotNull JetExpression root) {
        return checkUnfoldableAssignment(root) || checkUnfoldableReturn(root);
    }

    private static JetExpression getOutermostLastBlockElement(@Nullable JetExpression expression) {
        return (JetExpression) JetPsiUtil.getOutermostLastBlockElement(expression, JetPsiUtil.ANY_JET_ELEMENT);
    }

    private static void unfoldAssignmentToIf(@NotNull JetBinaryExpression assignment) {
        Project project = assignment.getProject();
        String op = assignment.getOperationReference().getText();
        String lhsText = assignment.getLeft().getText();
        JetIfExpression ifExpression = (JetIfExpression)assignment.getRight();

        assert ifExpression != null;

        ifExpression = (JetIfExpression)assignment.replace(ifExpression);

        JetExpression thenExpr = getOutermostLastBlockElement(ifExpression.getThen());
        JetExpression elseExpr = getOutermostLastBlockElement(ifExpression.getElse());

        assert thenExpr != null;
        assert elseExpr != null;

        thenExpr.replace(JetPsiFactory.createBinaryExpression(project, JetPsiFactory.createExpression(project, lhsText), op, thenExpr));
        elseExpr.replace(JetPsiFactory.createBinaryExpression(project, JetPsiFactory.createExpression(project, lhsText), op, elseExpr));
    }

    private static void unfoldAssignmentToWhen(@NotNull JetBinaryExpression assignment) {
        Project project = assignment.getProject();
        String op = assignment.getOperationReference().getText();
        JetExpression lhs = (JetExpression)assignment.getLeft().copy();
        JetWhenExpression whenExpression = (JetWhenExpression)assignment.getRight();

        assert whenExpression != null;

        whenExpression = (JetWhenExpression)assignment.replace(whenExpression);

        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JetExpression currExpr = getOutermostLastBlockElement(entry.getExpression());

            assert currExpr != null;

            currExpr.replace(JetPsiFactory.createBinaryExpression(project, lhs, op, currExpr));
        }
    }

    private static void unfoldReturnToIf(@NotNull JetReturnExpression returnExpression) {
        Project project = returnExpression.getProject();
        JetIfExpression ifExpression = (JetIfExpression)returnExpression.getReturnedExpression();

        assert ifExpression != null;

        ifExpression = (JetIfExpression)returnExpression.replace(ifExpression);

        JetExpression thenExpr = getOutermostLastBlockElement(ifExpression.getThen());
        JetExpression elseExpr = getOutermostLastBlockElement(ifExpression.getElse());

        assert thenExpr != null;
        assert elseExpr != null;

        thenExpr.replace(JetPsiFactory.createReturn(project, thenExpr));
        elseExpr.replace(JetPsiFactory.createReturn(project, elseExpr));
    }

    private static void unfoldReturnToWhen(@NotNull JetReturnExpression returnExpression) {
        Project project = returnExpression.getProject();
        JetWhenExpression whenExpression = (JetWhenExpression)returnExpression.getReturnedExpression();

        assert whenExpression != null;

        whenExpression = (JetWhenExpression)returnExpression.replace(whenExpression);

        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JetExpression currExpr = getOutermostLastBlockElement(entry.getExpression());

            assert currExpr != null;

            currExpr.replace(JetPsiFactory.createReturn(project, currExpr));
        }
    }

    public static void unfoldExpression(@NotNull JetExpression root) {
        if (checkUnfoldableAssignment(root)) {
            JetBinaryExpression assignment = (JetBinaryExpression)root;
            if (assignment.getRight() instanceof JetIfExpression) {
                unfoldAssignmentToIf(assignment);
            } else if (assignment.getRight() instanceof JetWhenExpression) {
                unfoldAssignmentToWhen(assignment);
            }
        } else if (checkUnfoldableReturn(root)) {
            JetReturnExpression returnExpression = (JetReturnExpression)root;
            if (returnExpression.getReturnedExpression() instanceof JetIfExpression) {
                unfoldReturnToIf(returnExpression);
            } else if (returnExpression.getReturnedExpression() instanceof JetWhenExpression) {
                unfoldReturnToWhen(returnExpression);
            }
        }
    }

    public static final Predicate<PsiElement> UNFOLDABLE_EXPRESSION = new Predicate<PsiElement>() {
        @Override
        public boolean apply(@Nullable PsiElement input) {
            return (input instanceof JetExpression) && checkUnfoldableExpression((JetExpression)input);
        }
    };
}
