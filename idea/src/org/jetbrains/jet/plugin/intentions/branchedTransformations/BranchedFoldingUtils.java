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

package org.jetbrains.jet.plugin.intentions.branchedTransformations;

import com.google.common.base.Predicate;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

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
                //noinspection ConstantConditions
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

    private static JetBinaryExpression getFoldableBranchedAssignment(JetExpression branch) {
        return (JetBinaryExpression)JetPsiUtil.getOutermostLastBlockElement(branch, CHECK_ASSIGNMENT);
    }

    private static JetReturnExpression getFoldableBranchedReturn(JetExpression branch) {
        return (JetReturnExpression)JetPsiUtil.getOutermostLastBlockElement(branch, CHECK_RETURN);
    }

    private static boolean checkAssignmentsMatch(JetBinaryExpression a1, JetBinaryExpression a2) {
        return checkEquivalence(a1.getLeft(), a2.getLeft()) && a1.getOperationToken().equals(a2.getOperationToken());
    }

    private static boolean checkFoldableIfExpressionWithAssignments(JetIfExpression ifExpression) {
        JetExpression thenBranch = ifExpression.getThen();
        JetExpression elseBranch = ifExpression.getElse();

        JetBinaryExpression thenAssignment = getFoldableBranchedAssignment(thenBranch);
        JetBinaryExpression elseAssignment = getFoldableBranchedAssignment(elseBranch);

        if (thenAssignment == null || elseAssignment == null) return false;

        return checkAssignmentsMatch(thenAssignment, elseAssignment);
    }

    private static boolean checkFoldableWhenExpressionWithAssignments(JetWhenExpression whenExpression) {
        if (!JetPsiUtil.checkWhenExpressionHasSingleElse(whenExpression)) return false;

        List<JetWhenEntry> entries = whenExpression.getEntries();

        if (entries.isEmpty()) return false;

        List<JetBinaryExpression> assignments = new ArrayList<JetBinaryExpression>();
        for (JetWhenEntry entry : entries) {
            JetBinaryExpression assignment = getFoldableBranchedAssignment(entry.getExpression());
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
        return getFoldableBranchedReturn(ifExpression.getThen()) != null &&
               getFoldableBranchedReturn(ifExpression.getElse()) != null;
    }

    private static boolean checkFoldableWhenExpressionWithReturns(JetWhenExpression whenExpression) {
        if (!JetPsiUtil.checkWhenExpressionHasSingleElse(whenExpression)) return false;

        List<JetWhenEntry> entries = whenExpression.getEntries();

        if (entries.isEmpty()) return false;

        for (JetWhenEntry entry : entries) {
            if (getFoldableBranchedReturn(entry.getExpression()) == null) return false;
        }

        return true;
    }

    private static boolean checkFoldableIfExpressionWithAsymmetricReturns(JetIfExpression ifExpression) {
        if (getFoldableBranchedReturn(ifExpression.getThen()) == null ||
            ifExpression.getElse() != null) {
            return false;
        }

        PsiElement nextElement = JetPsiUtil.skipTrailingWhitespacesAndComments(ifExpression);
        return (nextElement instanceof JetExpression) && getFoldableBranchedReturn((JetExpression) nextElement) != null;
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

    private static void assertNotNull(JetExpression expression) {
        assert expression != null : FOLD_WITHOUT_CHECK;
    }

    public static void foldIfExpressionWithAssignments(JetIfExpression ifExpression) {
        JetBinaryExpression thenAssignment = getFoldableBranchedAssignment(ifExpression.getThen());

        assertNotNull(thenAssignment);

        String op = thenAssignment.getOperationReference().getText();
        JetSimpleNameExpression lhs = (JetSimpleNameExpression) thenAssignment.getLeft();

        JetBinaryExpression assignment = JetPsiFactory(ifExpression).createBinaryExpression(lhs, op, ifExpression);
        JetIfExpression newIfExpression = (JetIfExpression)assignment.getRight();

        assertNotNull(newIfExpression);

        //noinspection ConstantConditions
        thenAssignment = getFoldableBranchedAssignment(newIfExpression.getThen());
        JetBinaryExpression elseAssignment = getFoldableBranchedAssignment(newIfExpression.getElse());

        assertNotNull(thenAssignment);
        assertNotNull(elseAssignment);

        JetExpression thenRhs = thenAssignment.getRight();
        JetExpression elseRhs = elseAssignment.getRight();

        assertNotNull(thenRhs);
        assertNotNull(elseRhs);

        //noinspection ConstantConditions
        thenAssignment.replace(thenRhs);
        //noinspection ConstantConditions
        elseAssignment.replace(elseRhs);

        ifExpression.replace(assignment);
    }

    public static void foldIfExpressionWithReturns(JetIfExpression ifExpression) {
        JetReturnExpression newReturnExpression = JetPsiFactory(ifExpression).createReturn(ifExpression);
        JetIfExpression newIfExpression = (JetIfExpression)newReturnExpression.getReturnedExpression();

        assertNotNull(newIfExpression);

        //noinspection ConstantConditions
        JetReturnExpression thenReturn = getFoldableBranchedReturn(newIfExpression.getThen());
        JetReturnExpression elseReturn = getFoldableBranchedReturn(newIfExpression.getElse());

        assertNotNull(thenReturn);
        assertNotNull(elseReturn);

        JetExpression thenExpr = thenReturn.getReturnedExpression();
        JetExpression elseExpr = elseReturn.getReturnedExpression();

        assertNotNull(thenExpr);
        assertNotNull(elseExpr);

        //noinspection ConstantConditions
        thenReturn.replace(thenExpr);
        //noinspection ConstantConditions
        elseReturn.replace(elseExpr);

        ifExpression.replace(newReturnExpression);
    }

    public static void foldIfExpressionWithAsymmetricReturns(JetIfExpression ifExpression) {
        JetExpression condition = ifExpression.getCondition();
        JetExpression thenRoot = ifExpression.getThen();
        JetExpression elseRoot = (JetExpression)JetPsiUtil.skipTrailingWhitespacesAndComments(ifExpression);

        assertNotNull(condition);
        assertNotNull(thenRoot);
        assertNotNull(elseRoot);

        //noinspection ConstantConditions
        JetPsiFactory psiFactory = JetPsiFactory(ifExpression);
        JetIfExpression newIfExpression = psiFactory.createIf(condition, thenRoot, elseRoot);
        JetReturnExpression newReturnExpression = psiFactory.createReturn(newIfExpression);

        newIfExpression = (JetIfExpression)newReturnExpression.getReturnedExpression();

        assertNotNull(newIfExpression);

        //noinspection ConstantConditions
        JetReturnExpression thenReturn = getFoldableBranchedReturn(newIfExpression.getThen());
        JetReturnExpression elseReturn = getFoldableBranchedReturn(newIfExpression.getElse());

        assertNotNull(thenReturn);
        assertNotNull(elseReturn);

        JetExpression thenExpr = thenReturn.getReturnedExpression();
        JetExpression elseExpr = elseReturn.getReturnedExpression();

        assertNotNull(thenExpr);
        assertNotNull(elseExpr);

        //noinspection ConstantConditions
        thenReturn.replace(thenExpr);
        //noinspection ConstantConditions
        elseReturn.replace(elseExpr);

        elseRoot.delete();
        ifExpression.replace(newReturnExpression);
    }

    @SuppressWarnings("ConstantConditions")
    public static void foldWhenExpressionWithAssignments(JetWhenExpression whenExpression) {
        assert !whenExpression.getEntries().isEmpty() : FOLD_WITHOUT_CHECK;

        JetBinaryExpression firstAssignment = getFoldableBranchedAssignment(whenExpression.getEntries().get(0).getExpression());

        assertNotNull(firstAssignment);

        String op = firstAssignment.getOperationReference().getText();
        JetSimpleNameExpression lhs = (JetSimpleNameExpression) firstAssignment.getLeft();

        JetBinaryExpression assignment = JetPsiFactory(whenExpression).createBinaryExpression(lhs, op, whenExpression);
        JetWhenExpression newWhenExpression = (JetWhenExpression)assignment.getRight();

        assertNotNull(newWhenExpression);

        for (JetWhenEntry entry : newWhenExpression.getEntries()) {
            JetBinaryExpression currAssignment = getFoldableBranchedAssignment(entry.getExpression());

            assertNotNull(currAssignment);

            JetExpression currRhs = currAssignment.getRight();

            assertNotNull(currRhs);

            currAssignment.replace(currRhs);
        }

        whenExpression.replace(assignment);
    }

    public static void foldWhenExpressionWithReturns(JetWhenExpression whenExpression) {
        assert !whenExpression.getEntries().isEmpty() : FOLD_WITHOUT_CHECK;

        JetReturnExpression newReturnExpression = JetPsiFactory(whenExpression).createReturn(whenExpression);
        JetWhenExpression newWhenExpression = (JetWhenExpression)newReturnExpression.getReturnedExpression();

        assertNotNull(newWhenExpression);

        //noinspection ConstantConditions
        for (JetWhenEntry entry : newWhenExpression.getEntries()) {
            JetReturnExpression currReturn = getFoldableBranchedReturn(entry.getExpression());

            assertNotNull(currReturn);

            JetExpression currExpr = currReturn.getReturnedExpression();

            assertNotNull(currExpr);

            //noinspection ConstantConditions
            currReturn.replace(currExpr);
        }

        whenExpression.replace(newReturnExpression);
    }
}
