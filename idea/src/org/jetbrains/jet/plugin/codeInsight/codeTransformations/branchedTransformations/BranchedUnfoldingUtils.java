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

    private static JetExpression getOutermostLastBlockElement(@Nullable JetExpression expression) {
        return (JetExpression) JetPsiUtil.getOutermostLastBlockElement(expression, JetPsiUtil.ANY_JET_ELEMENT);
    }

    @Nullable
    public static UnfoldableKind getUnfoldableExpressionKind(@Nullable JetExpression root) {
        if (root == null) return null;

        if (JetPsiUtil.isAssignment(root)) {
            JetBinaryExpression assignment = (JetBinaryExpression)root;
            JetExpression lhs = assignment.getLeft();
            JetExpression rhs = assignment.getRight();

            if (!(lhs instanceof JetSimpleNameExpression)) return null;

            if (rhs instanceof JetIfExpression) return UnfoldableKind.ASSIGNMENT_TO_IF;
            if (rhs instanceof JetWhenExpression) return UnfoldableKind.ASSIGNMENT_TO_WHEN;
        } else if (root instanceof JetReturnExpression) {
            JetExpression resultExpr = ((JetReturnExpression)root).getReturnedExpression();

            if (resultExpr instanceof JetIfExpression) return UnfoldableKind.RETURN_TO_IF;
            if (resultExpr instanceof JetWhenExpression) return UnfoldableKind.RETURN_TO_WHEN;
        }

        return null;
    }

    public static final String UNFOLD_WITHOUT_CHECK = "Expression must be checked before unfolding";

    public static void unfoldAssignmentToIf(@NotNull JetBinaryExpression assignment) {
        Project project = assignment.getProject();
        String op = assignment.getOperationReference().getText();
        String lhsText = assignment.getLeft().getText();
        JetIfExpression ifExpression = (JetIfExpression)assignment.getRight();

        assert ifExpression != null : UNFOLD_WITHOUT_CHECK;

        ifExpression = (JetIfExpression)assignment.replace(ifExpression);

        JetExpression thenExpr = getOutermostLastBlockElement(ifExpression.getThen());
        JetExpression elseExpr = getOutermostLastBlockElement(ifExpression.getElse());

        assert thenExpr != null : UNFOLD_WITHOUT_CHECK;
        assert elseExpr != null : UNFOLD_WITHOUT_CHECK;

        thenExpr.replace(JetPsiFactory.createBinaryExpression(project, JetPsiFactory.createExpression(project, lhsText), op, thenExpr));
        elseExpr.replace(JetPsiFactory.createBinaryExpression(project, JetPsiFactory.createExpression(project, lhsText), op, elseExpr));
    }

    public static void unfoldAssignmentToWhen(@NotNull JetBinaryExpression assignment) {
        Project project = assignment.getProject();
        String op = assignment.getOperationReference().getText();
        JetExpression lhs = (JetExpression)assignment.getLeft().copy();
        JetWhenExpression whenExpression = (JetWhenExpression)assignment.getRight();

        assert whenExpression != null : UNFOLD_WITHOUT_CHECK;

        whenExpression = (JetWhenExpression)assignment.replace(whenExpression);

        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JetExpression currExpr = getOutermostLastBlockElement(entry.getExpression());

            assert currExpr != null : UNFOLD_WITHOUT_CHECK;

            currExpr.replace(JetPsiFactory.createBinaryExpression(project, lhs, op, currExpr));
        }
    }

    public static void unfoldReturnToIf(@NotNull JetReturnExpression returnExpression) {
        Project project = returnExpression.getProject();
        JetIfExpression ifExpression = (JetIfExpression)returnExpression.getReturnedExpression();

        assert ifExpression != null : UNFOLD_WITHOUT_CHECK;

        ifExpression = (JetIfExpression)returnExpression.replace(ifExpression);

        JetExpression thenExpr = getOutermostLastBlockElement(ifExpression.getThen());
        JetExpression elseExpr = getOutermostLastBlockElement(ifExpression.getElse());

        assert thenExpr != null : UNFOLD_WITHOUT_CHECK;
        assert elseExpr != null : UNFOLD_WITHOUT_CHECK;

        thenExpr.replace(JetPsiFactory.createReturn(project, thenExpr));
        elseExpr.replace(JetPsiFactory.createReturn(project, elseExpr));
    }

    public static void unfoldReturnToWhen(@NotNull JetReturnExpression returnExpression) {
        Project project = returnExpression.getProject();
        JetWhenExpression whenExpression = (JetWhenExpression)returnExpression.getReturnedExpression();

        assert whenExpression != null : UNFOLD_WITHOUT_CHECK;

        whenExpression = (JetWhenExpression)returnExpression.replace(whenExpression);

        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JetExpression currExpr = getOutermostLastBlockElement(entry.getExpression());

            assert currExpr != null : UNFOLD_WITHOUT_CHECK;

            currExpr.replace(JetPsiFactory.createReturn(project, currExpr));
        }
    }
}
