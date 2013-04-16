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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;

import java.util.ArrayList;
import java.util.List;

public class BranchedFoldingUtils {
    private BranchedFoldingUtils() {
    }

    private static boolean checkEquivalence(JetExpression e1, JetExpression e2) {
        return e1.getText().equals(e2.getText());
    }

    private static final Predicate<JetElement> CHECK_ASSIGNMENT = new Predicate<JetElement>() {
        @Override
        public boolean apply(@Nullable JetElement input) {
            if (input == null || !JetPsiUtil.isAssignment(input)) {
                return false;
            }

            JetBinaryExpression assignment = (JetBinaryExpression)input;

            if (assignment.getRight() == null || !(assignment.getLeft() instanceof JetSimpleNameExpression)) {
                return false;
            }

            if (assignment.getParent() instanceof JetBlockExpression) {
                return !JetPsiUtil.checkVariableDeclarationInBlock((JetBlockExpression) assignment.getParent(), assignment.getLeft().getText());
            }

            return true;
        }
    };

    private static final Predicate<JetElement> CHECK_RETURN = new Predicate<JetElement>() {
        @Override
        public boolean apply(@Nullable JetElement input) {
            return (input instanceof JetReturnExpression) && ((JetReturnExpression)input).getReturnedExpression() != null;
        }
    };

    private static JetBinaryExpression checkAndGetFoldableBranchedAssignment(JetExpression branch) {
        return (JetBinaryExpression)JetPsiUtil.getOutermostLastBlockElement(branch, CHECK_ASSIGNMENT);
    }

    private static JetReturnExpression checkAndGetFoldableBranchedReturn(JetExpression branch) {
        return (JetReturnExpression)JetPsiUtil.getOutermostLastBlockElement(branch, CHECK_RETURN);
    }

    private static boolean checkAssignmentsMatch(JetBinaryExpression a1, JetBinaryExpression a2) {
        return checkEquivalence(a1.getLeft(), a2.getLeft()) && a1.getOperationToken().equals(a2.getOperationToken());
    }

    private static boolean checkFoldableIfExpressionWithAssignments(JetIfExpression ifExpression) {
        JetExpression thenBranch = ifExpression.getThen();
        JetExpression elseBranch = ifExpression.getElse();

        JetBinaryExpression thenAssignment = checkAndGetFoldableBranchedAssignment(thenBranch);
        JetBinaryExpression elseAssignment = checkAndGetFoldableBranchedAssignment(elseBranch);

        if (thenAssignment == null || elseAssignment == null) return false;

        return checkAssignmentsMatch(thenAssignment, elseAssignment);
    }

    private static boolean checkFoldableWhenExpressionWithAssignments(JetWhenExpression whenExpression) {
        if (!JetPsiUtil.checkWhenExpressionHasSingleElse(whenExpression)) return false;

        List<JetWhenEntry> entries = whenExpression.getEntries();

        if (entries.isEmpty()) return false;

        List<JetBinaryExpression> assignments = new ArrayList<JetBinaryExpression>();
        for (JetWhenEntry entry : entries) {
            JetBinaryExpression assignment = checkAndGetFoldableBranchedAssignment(entry.getExpression());
            if (assignment == null) return false;
            assignments.add(assignment);
        }

        assert !assignments.isEmpty();

        JetBinaryExpression firstAssignment = assignments.get(0);
        for (JetBinaryExpression assignment : assignments) {
            if (!checkAssignmentsMatch(assignment, firstAssignment)) return false;
        }

        return true;
    }

    private static boolean checkFoldableIfExpressionWithReturns(JetIfExpression ifExpression) {
        return checkAndGetFoldableBranchedReturn(ifExpression.getThen()) != null &&
               checkAndGetFoldableBranchedReturn(ifExpression.getElse()) != null;
    }

    private static boolean checkFoldableWhenExpressionWithReturns(JetWhenExpression whenExpression) {
        if (!JetPsiUtil.checkWhenExpressionHasSingleElse(whenExpression)) return false;

        List<JetWhenEntry> entries = whenExpression.getEntries();

        if (entries.isEmpty()) return false;

        for (JetWhenEntry entry : entries) {
            if (checkAndGetFoldableBranchedReturn(entry.getExpression()) == null) return false;
        }

        return true;
    }

    private static boolean checkFoldableIfExpressionWithAsymmetricReturns(JetIfExpression ifExpression) {
        if (checkAndGetFoldableBranchedReturn(ifExpression.getThen()) == null ||
            ifExpression.getElse() != null) {
            return false;
        }

        PsiElement nextElement = JetPsiUtil.skipTrailingWhitespacesAndComments(ifExpression);
        return (nextElement instanceof JetExpression) && checkAndGetFoldableBranchedReturn((JetExpression)nextElement) != null;
    }

    @Nullable
    public static FoldableKind getFoldableExpressionKind(@Nullable JetExpression root) {
        if (root instanceof JetIfExpression) {
            JetIfExpression ifExpression = (JetIfExpression)root;

            if (checkFoldableIfExpressionWithAssignments(ifExpression)) return FoldableKind.IF_TO_ASSIGNMENT;
            if (checkFoldableIfExpressionWithReturns(ifExpression)) return FoldableKind.IF_TO_RETURN;
            if (checkFoldableIfExpressionWithAsymmetricReturns(ifExpression)) return FoldableKind.IF_TO_RETURN_ASYMMETRICALLY;
        } else if (root instanceof JetWhenExpression) {
            JetWhenExpression whenExpression = (JetWhenExpression)root;

            if (checkFoldableWhenExpressionWithAssignments(whenExpression)) return FoldableKind.WHEN_TO_ASSIGNMENT;
            if (checkFoldableWhenExpressionWithReturns(whenExpression)) return FoldableKind.WHEN_TO_RETURN;
        }

        return null;
    }

    public static final String FOLD_WITHOUT_CHECK = "Expression must be checked before folding";

    public static void foldIfExpressionWithAssignments(JetIfExpression ifExpression) {
        Project project = ifExpression.getProject();

        JetBinaryExpression thenAssignment = checkAndGetFoldableBranchedAssignment(ifExpression.getThen());

        assert thenAssignment != null : FOLD_WITHOUT_CHECK;

        String op = thenAssignment.getOperationReference().getText();
        JetSimpleNameExpression lhs = (JetSimpleNameExpression) thenAssignment.getLeft();

        JetBinaryExpression assignment =
                (JetBinaryExpression)ifExpression.replace(JetPsiFactory.createBinaryExpression(project, lhs, op, ifExpression));
        ifExpression = (JetIfExpression)assignment.getRight();

        assert ifExpression != null : FOLD_WITHOUT_CHECK;

        thenAssignment = checkAndGetFoldableBranchedAssignment(ifExpression.getThen());
        JetBinaryExpression elseAssignment = checkAndGetFoldableBranchedAssignment(ifExpression.getElse());

        assert thenAssignment != null : FOLD_WITHOUT_CHECK;
        assert elseAssignment != null : FOLD_WITHOUT_CHECK;

        JetExpression thenRhs = thenAssignment.getRight();
        JetExpression elseRhs = elseAssignment.getRight();

        assert thenRhs != null : FOLD_WITHOUT_CHECK;
        assert elseRhs != null : FOLD_WITHOUT_CHECK;

        thenAssignment.replace(thenRhs);
        elseAssignment.replace(elseRhs);
    }

    public static void foldIfExpressionWithReturns(JetIfExpression ifExpression) {
        Project project = ifExpression.getProject();

        JetReturnExpression returnExpr = (JetReturnExpression)ifExpression.replace(JetPsiFactory.createReturn(project, ifExpression));
        ifExpression = (JetIfExpression)returnExpr.getReturnedExpression();

        assert ifExpression != null : FOLD_WITHOUT_CHECK;

        JetReturnExpression thenReturn = checkAndGetFoldableBranchedReturn(ifExpression.getThen());
        JetReturnExpression elseReturn = checkAndGetFoldableBranchedReturn(ifExpression.getElse());

        assert thenReturn != null : FOLD_WITHOUT_CHECK;
        assert elseReturn != null : FOLD_WITHOUT_CHECK;

        JetExpression thenExpr = thenReturn.getReturnedExpression();
        JetExpression elseExpr = elseReturn.getReturnedExpression();

        assert thenExpr != null : FOLD_WITHOUT_CHECK;
        assert elseExpr != null : FOLD_WITHOUT_CHECK;

        thenReturn.replace(thenExpr);
        elseReturn.replace(elseExpr);
    }

    public static void foldIfExpressionWithAsymmetricReturns(JetIfExpression ifExpression) {
        Project project = ifExpression.getProject();

        JetExpression condition = ifExpression.getCondition();
        JetExpression thenRoot = ifExpression.getThen();
        JetExpression elseRoot = (JetExpression)JetPsiUtil.skipTrailingWhitespacesAndComments(ifExpression);

        assert condition != null : FOLD_WITHOUT_CHECK;
        assert thenRoot != null : FOLD_WITHOUT_CHECK;
        assert elseRoot != null : FOLD_WITHOUT_CHECK;

        JetIfExpression newIfExpr = JetPsiFactory.createIf(project, condition, thenRoot, elseRoot);
        JetReturnExpression newReturnExpr = JetPsiFactory.createReturn(project, newIfExpr);
        newReturnExpr = (JetReturnExpression) ifExpression.replace(newReturnExpr);

        JetReturnExpression oldReturn = (JetReturnExpression)JetPsiUtil.skipTrailingWhitespacesAndComments(newReturnExpr);

        assert oldReturn != null : FOLD_WITHOUT_CHECK;

        oldReturn.delete();

        newIfExpr = (JetIfExpression)newReturnExpr.getReturnedExpression();

        assert newIfExpr != null : FOLD_WITHOUT_CHECK;

        JetReturnExpression thenReturn = checkAndGetFoldableBranchedReturn(newIfExpr.getThen());
        JetReturnExpression elseReturn = checkAndGetFoldableBranchedReturn(newIfExpr.getElse());

        assert thenReturn != null : FOLD_WITHOUT_CHECK;
        assert elseReturn != null : FOLD_WITHOUT_CHECK;

        JetExpression thenExpr = thenReturn.getReturnedExpression();
        JetExpression elseExpr = elseReturn.getReturnedExpression();

        assert thenExpr != null : FOLD_WITHOUT_CHECK;
        assert elseExpr != null : FOLD_WITHOUT_CHECK;

        thenReturn.replace(thenExpr);
        elseReturn.replace(elseExpr);
    }

    public static void foldWhenExpressionWithAssignments(JetWhenExpression whenExpression) {
        Project project = whenExpression.getProject();

        assert !whenExpression.getEntries().isEmpty() : FOLD_WITHOUT_CHECK;

        JetBinaryExpression firstAssignment = checkAndGetFoldableBranchedAssignment(whenExpression.getEntries().get(0).getExpression());

        assert firstAssignment != null : FOLD_WITHOUT_CHECK;

        String op = firstAssignment.getOperationReference().getText();
        JetSimpleNameExpression lhs = (JetSimpleNameExpression) firstAssignment.getLeft();

        JetBinaryExpression assignment =
                (JetBinaryExpression)whenExpression.replace(JetPsiFactory.createBinaryExpression(project, lhs, op, whenExpression));
        whenExpression = (JetWhenExpression)assignment.getRight();

        assert whenExpression != null : FOLD_WITHOUT_CHECK;

        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JetBinaryExpression currAssignment = checkAndGetFoldableBranchedAssignment(entry.getExpression());

            assert currAssignment != null : FOLD_WITHOUT_CHECK;

            JetExpression currRhs = currAssignment.getRight();

            assert currRhs != null : FOLD_WITHOUT_CHECK;

            currAssignment.replace(currRhs);
        }
    }

    public static void foldWhenExpressionWithReturns(JetWhenExpression whenExpression) {
        Project project = whenExpression.getProject();

        assert !whenExpression.getEntries().isEmpty() : FOLD_WITHOUT_CHECK;

        JetReturnExpression returnExpr = (JetReturnExpression)whenExpression.replace(JetPsiFactory.createReturn(project, whenExpression));
        whenExpression = (JetWhenExpression)returnExpr.getReturnedExpression();

        assert whenExpression != null : FOLD_WITHOUT_CHECK;

        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JetReturnExpression currReturn = checkAndGetFoldableBranchedReturn(entry.getExpression());

            assert currReturn != null : FOLD_WITHOUT_CHECK;

            JetExpression currExpr = currReturn.getReturnedExpression();

            assert currExpr != null : FOLD_WITHOUT_CHECK;

            currReturn.replace(currExpr);
        }
    }
}
