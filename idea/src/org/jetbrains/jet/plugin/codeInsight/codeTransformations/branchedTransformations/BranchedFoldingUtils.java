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
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
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

    private static boolean checkFoldableIfExpressionWithAssignments(JetIfExpression ifExpression) {
        JetExpression thenBranch = ifExpression.getThen();
        JetExpression elseBranch = ifExpression.getElse();

        JetBinaryExpression thenAssignment = checkAndGetFoldableBranchedAssignment(thenBranch);
        JetBinaryExpression elseAssignment = checkAndGetFoldableBranchedAssignment(elseBranch);

        if (thenAssignment == null || elseAssignment == null) return false;

        return checkEquivalence(thenAssignment.getLeft(), elseAssignment.getLeft()) &&
                thenAssignment.getOperationToken().equals(elseAssignment.getOperationToken());
    }

    private static boolean checkFoldableWhenExpressionWithAssignments(JetWhenExpression whenExpression) {
        List<JetWhenEntry> entries = whenExpression.getEntries();

        if (entries.isEmpty()) return false;

        boolean hasElse = false;
        List<JetBinaryExpression> assignments = new ArrayList<JetBinaryExpression>();
        for (JetWhenEntry entry : entries) {
            if (entry.isElse()) {
                hasElse = true;
            }
            JetBinaryExpression assignment = checkAndGetFoldableBranchedAssignment(entry.getExpression());
            if (assignment == null) return false;
            assignments.add(assignment);
        }

        if (!hasElse) return false;

        assert !assignments.isEmpty();

        JetExpression lhs = assignments.get(0).getLeft();
        IElementType opToken = assignments.get(0).getOperationToken();
        for (int i = 1; i < assignments.size(); i++) {
            if (!checkEquivalence(lhs, assignments.get(i).getLeft())) return false;
            if (!opToken.equals(assignments.get(i).getOperationToken())) return false;
        }

        return true;
    }

    private static boolean checkFoldableIfExpressionWithReturns(JetIfExpression ifExpression) {
        return checkAndGetFoldableBranchedReturn(ifExpression.getThen()) != null &&
               checkAndGetFoldableBranchedReturn(ifExpression.getElse()) != null;
    }

    private static boolean checkFoldableWhenExpressionWithReturns(JetWhenExpression whenExpression) {
        List<JetWhenEntry> entries = whenExpression.getEntries();

        if (entries.isEmpty()) return false;

        boolean hasElse = false;
        for (JetWhenEntry entry : entries) {
            if (entry.isElse()) {
                hasElse = true;
            }
            if (checkAndGetFoldableBranchedReturn(entry.getExpression()) == null) return false;
        }

        return hasElse;
    }

    private static boolean checkFoldableIfExpressionWithAsymmetricReturns(JetIfExpression ifExpression) {
        if (checkAndGetFoldableBranchedReturn(ifExpression.getThen()) == null ||
            checkAndGetFoldableBranchedReturn(ifExpression.getElse()) != null) {
            return false;
        }

        PsiElement nextElement = PsiTreeUtil.skipSiblingsForward(ifExpression, PsiWhiteSpace.class);
        return (nextElement instanceof JetExpression) && checkAndGetFoldableBranchedReturn((JetExpression)nextElement) != null;
    }

    public static boolean checkFoldableExpression(@Nullable JetExpression root) {
        if (root instanceof JetIfExpression) {
            JetIfExpression ifExpression = (JetIfExpression)root;
            return checkFoldableIfExpressionWithAssignments(ifExpression) ||
                   checkFoldableIfExpressionWithReturns(ifExpression) ||
                   checkFoldableIfExpressionWithAsymmetricReturns(ifExpression) ;
        }

        if (root instanceof JetWhenExpression) {
            JetWhenExpression whenExpression = (JetWhenExpression)root;
            return checkFoldableWhenExpressionWithAssignments(whenExpression) ||
                   checkFoldableWhenExpressionWithReturns(whenExpression);
        }

        return false;
    }

    private static void foldIfExpressionWithAssignments(JetIfExpression ifExpression) {
        Project project = ifExpression.getProject();

        JetBinaryExpression thenAssignment = checkAndGetFoldableBranchedAssignment(ifExpression.getThen());

        assert thenAssignment != null;

        String op = thenAssignment.getOperationReference().getText();
        JetSimpleNameExpression lhs = (JetSimpleNameExpression) thenAssignment.getLeft();

        JetBinaryExpression assignment =
                (JetBinaryExpression)ifExpression.replace(JetPsiFactory.createBinaryExpression(project, lhs, op, ifExpression));
        ifExpression = (JetIfExpression)assignment.getRight();

        assert ifExpression != null;

        thenAssignment = checkAndGetFoldableBranchedAssignment(ifExpression.getThen());
        JetBinaryExpression elseAssignment = checkAndGetFoldableBranchedAssignment(ifExpression.getElse());

        assert thenAssignment != null;
        assert elseAssignment != null;

        JetExpression thenRhs = thenAssignment.getRight();
        JetExpression elseRhs = elseAssignment.getRight();

        assert thenRhs != null;
        assert elseRhs != null;

        thenAssignment.replace(thenRhs);
        elseAssignment.replace(elseRhs);
    }

    private static void foldIfExpressionWithReturns(JetIfExpression ifExpression) {
        Project project = ifExpression.getProject();

        JetReturnExpression returnExpr = (JetReturnExpression)ifExpression.replace(JetPsiFactory.createReturn(project, ifExpression));
        ifExpression = (JetIfExpression)returnExpr.getReturnedExpression();

        assert ifExpression != null;

        JetReturnExpression thenReturn = checkAndGetFoldableBranchedReturn(ifExpression.getThen());
        JetReturnExpression elseReturn = checkAndGetFoldableBranchedReturn(ifExpression.getElse());

        assert thenReturn != null;
        assert elseReturn != null;

        JetExpression thenExpr = thenReturn.getReturnedExpression();
        JetExpression elseExpr = elseReturn.getReturnedExpression();

        assert thenExpr != null;
        assert elseExpr != null;

        thenReturn.replace(thenExpr);
        elseReturn.replace(elseExpr);
    }

    private static void foldIfExpressionWithAsymmetricReturns(JetIfExpression ifExpression) {
        Project project = ifExpression.getProject();

        JetExpression condition = ifExpression.getCondition();
        JetExpression thenRoot = ifExpression.getThen();
        JetExpression elseRoot = (JetExpression)PsiTreeUtil.skipSiblingsForward(ifExpression, PsiWhiteSpace.class);

        assert condition != null;
        assert thenRoot != null;
        assert elseRoot != null;

        JetIfExpression newIfExpr = JetPsiFactory.createIf(project, condition, thenRoot, elseRoot);
        JetReturnExpression newReturnExpr = JetPsiFactory.createReturn(project, newIfExpr);
        newReturnExpr = (JetReturnExpression) ifExpression.replace(newReturnExpr);

        JetReturnExpression oldReturn = (JetReturnExpression)PsiTreeUtil.skipSiblingsForward(newReturnExpr, PsiWhiteSpace.class);

        assert oldReturn != null;

        oldReturn.delete();

        newIfExpr = (JetIfExpression)newReturnExpr.getReturnedExpression();

        assert newIfExpr != null;

        JetReturnExpression thenReturn = checkAndGetFoldableBranchedReturn(newIfExpr.getThen());
        JetReturnExpression elseReturn = checkAndGetFoldableBranchedReturn(newIfExpr.getElse());

        assert thenReturn != null;
        assert elseReturn != null;

        JetExpression thenExpr = thenReturn.getReturnedExpression();
        JetExpression elseExpr = elseReturn.getReturnedExpression();

        assert thenExpr != null;
        assert elseExpr != null;

        thenReturn.replace(thenExpr);
        elseReturn.replace(elseExpr);
    }

    private static void foldWhenExpressionWithAssignments(JetWhenExpression whenExpression) {
        Project project = whenExpression.getProject();

        assert !whenExpression.getEntries().isEmpty();

        JetBinaryExpression firstAssignment = checkAndGetFoldableBranchedAssignment(whenExpression.getEntries().get(0).getExpression());

        assert firstAssignment != null;

        String op = firstAssignment.getOperationReference().getText();
        JetSimpleNameExpression lhs = (JetSimpleNameExpression) firstAssignment.getLeft();

        JetBinaryExpression assignment =
                (JetBinaryExpression)whenExpression.replace(JetPsiFactory.createBinaryExpression(project, lhs, op, whenExpression));
        whenExpression = (JetWhenExpression)assignment.getRight();

        assert whenExpression != null;

        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JetBinaryExpression currAssignment = checkAndGetFoldableBranchedAssignment(entry.getExpression());

            assert currAssignment != null;

            JetExpression currRhs = currAssignment.getRight();

            assert currRhs != null;

            currAssignment.replace(currRhs);
        }
    }

    private static void foldWhenExpressionWithReturns(JetWhenExpression whenExpression) {
        Project project = whenExpression.getProject();

        assert !whenExpression.getEntries().isEmpty();

        JetReturnExpression returnExpr = (JetReturnExpression)whenExpression.replace(JetPsiFactory.createReturn(project, whenExpression));
        whenExpression = (JetWhenExpression)returnExpr.getReturnedExpression();

        assert whenExpression != null;

        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JetReturnExpression currReturn = checkAndGetFoldableBranchedReturn(entry.getExpression());

            assert currReturn != null;

            JetExpression currExpr = currReturn.getReturnedExpression();

            assert currExpr != null;

            currReturn.replace(currExpr);
        }
    }

    public static void foldExpression(@Nullable JetExpression root) {
        if (root instanceof JetIfExpression) {
            JetIfExpression ifExpression = (JetIfExpression)root;
            if (checkFoldableIfExpressionWithAssignments(ifExpression)) {
                foldIfExpressionWithAssignments(ifExpression);
            } else if (checkFoldableIfExpressionWithReturns(ifExpression)) {
                foldIfExpressionWithReturns(ifExpression);
            } else if (checkFoldableIfExpressionWithAsymmetricReturns(ifExpression)) {
                foldIfExpressionWithAsymmetricReturns(ifExpression);
            }
        }

        if (root instanceof JetWhenExpression) {
            JetWhenExpression whenExpression = (JetWhenExpression)root;
            if (checkFoldableWhenExpressionWithAssignments(whenExpression)) {
                foldWhenExpressionWithAssignments(whenExpression);
            } else if (checkFoldableWhenExpressionWithReturns(whenExpression)) {
                foldWhenExpressionWithReturns(whenExpression);
            }
        }
    }

    public static final Predicate<PsiElement> FOLDABLE_EXPRESSION = new Predicate<PsiElement>() {
        @Override
        public boolean apply(@Nullable PsiElement input) {
            return (input instanceof JetExpression) && checkFoldableExpression((JetExpression)input);
        }
    };
}
