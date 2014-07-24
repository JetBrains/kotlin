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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.intentions.declarations.DeclarationUtils;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

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
            JetBinaryExpression assignment = (JetBinaryExpression) root;

            if (assignment.getLeft() == null) return null;

            JetExpression rhs = assignment.getRight();
            if (rhs instanceof JetIfExpression) return UnfoldableKind.ASSIGNMENT_TO_IF;
            if (rhs instanceof JetWhenExpression && JetPsiUtil.checkWhenExpressionHasSingleElse((JetWhenExpression) rhs)) {
                return UnfoldableKind.ASSIGNMENT_TO_WHEN;
            }
        }
        else if (root instanceof JetReturnExpression) {
            JetExpression resultExpr = ((JetReturnExpression) root).getReturnedExpression();

            if (resultExpr instanceof JetIfExpression) return UnfoldableKind.RETURN_TO_IF;
            if (resultExpr instanceof JetWhenExpression && JetPsiUtil.checkWhenExpressionHasSingleElse((JetWhenExpression) resultExpr)) {
                return UnfoldableKind.RETURN_TO_WHEN;
            }
        }
        else if (root instanceof JetProperty) {
            JetProperty property = (JetProperty) root;
            if (!property.isLocal()) return null;

            JetExpression initializer = property.getInitializer();

            if (initializer instanceof JetIfExpression) return UnfoldableKind.PROPERTY_TO_IF;
            if (initializer instanceof JetWhenExpression && JetPsiUtil.checkWhenExpressionHasSingleElse((JetWhenExpression) initializer)) {
                return UnfoldableKind.PROPERTY_TO_WHEN;
            }
        }

        return null;
    }

    public static final String UNFOLD_WITHOUT_CHECK = "Expression must be checked before unfolding";

    private static void assertNotNull(Object value) {
        assert value != null : UNFOLD_WITHOUT_CHECK;
    }

    public static void unfoldAssignmentToIf(@NotNull JetBinaryExpression assignment, @NotNull Editor editor) {
        String op = assignment.getOperationReference().getText();
        JetExpression lhs = assignment.getLeft();
        JetIfExpression ifExpression = (JetIfExpression) assignment.getRight();

        assertNotNull(ifExpression);

        //noinspection ConstantConditions
        JetIfExpression newIfExpression = (JetIfExpression) ifExpression.copy();

        JetExpression thenExpr = getOutermostLastBlockElement(newIfExpression.getThen());
        JetExpression elseExpr = getOutermostLastBlockElement(newIfExpression.getElse());

        assertNotNull(thenExpr);
        assertNotNull(elseExpr);

        //noinspection ConstantConditions
        JetPsiFactory psiFactory = JetPsiFactory(assignment);
        thenExpr.replace(psiFactory.createBinaryExpression(lhs, op, thenExpr));
        elseExpr.replace(psiFactory.createBinaryExpression(lhs, op, elseExpr));

        PsiElement resultElement = assignment.replace(newIfExpression);

        editor.getCaretModel().moveToOffset(resultElement.getTextOffset());
    }

    public static void unfoldAssignmentToWhen(@NotNull JetBinaryExpression assignment, @NotNull Editor editor) {
        String op = assignment.getOperationReference().getText();
        JetExpression lhs = assignment.getLeft();
        JetWhenExpression whenExpression = (JetWhenExpression) assignment.getRight();

        assertNotNull(whenExpression);

        //noinspection ConstantConditions
        JetWhenExpression newWhenExpression = (JetWhenExpression) whenExpression.copy();

        for (JetWhenEntry entry : newWhenExpression.getEntries()) {
            JetExpression currExpr = getOutermostLastBlockElement(entry.getExpression());

            assertNotNull(currExpr);

            //noinspection ConstantConditions
            currExpr.replace(JetPsiFactory(assignment).createBinaryExpression(lhs, op, currExpr));
        }

        PsiElement resultElement = assignment.replace(newWhenExpression);

        editor.getCaretModel().moveToOffset(resultElement.getTextOffset());
    }

    public static void unfoldPropertyToIf(@NotNull JetProperty property, @NotNull Editor editor) {
        JetBinaryExpression assignment = DeclarationUtils.splitPropertyDeclaration(property);
        unfoldAssignmentToIf(assignment, editor);
    }

    public static void unfoldPropertyToWhen(@NotNull JetProperty property, @NotNull Editor editor) {
        JetBinaryExpression assignment = DeclarationUtils.splitPropertyDeclaration(property);
        unfoldAssignmentToWhen(assignment, editor);
    }

    public static void unfoldReturnToIf(@NotNull JetReturnExpression returnExpression) {
        JetIfExpression ifExpression = (JetIfExpression) returnExpression.getReturnedExpression();

        assertNotNull(ifExpression);

        //noinspection ConstantConditions
        JetIfExpression newIfExpression = (JetIfExpression) ifExpression.copy();

        JetExpression thenExpr = getOutermostLastBlockElement(newIfExpression.getThen());
        JetExpression elseExpr = getOutermostLastBlockElement(newIfExpression.getElse());

        assertNotNull(thenExpr);
        assertNotNull(elseExpr);

        JetPsiFactory psiFactory = JetPsiFactory(returnExpression);
        thenExpr.replace(psiFactory.createReturn(thenExpr));
        elseExpr.replace(psiFactory.createReturn(elseExpr));

        returnExpression.replace(newIfExpression);
    }

    public static void unfoldReturnToWhen(@NotNull JetReturnExpression returnExpression) {
        JetWhenExpression whenExpression = (JetWhenExpression) returnExpression.getReturnedExpression();

        assertNotNull(whenExpression);

        //noinspection ConstantConditions
        JetWhenExpression newWhenExpression = (JetWhenExpression) whenExpression.copy();

        for (JetWhenEntry entry : newWhenExpression.getEntries()) {
            JetExpression currExpr = getOutermostLastBlockElement(entry.getExpression());

            assertNotNull(currExpr);

            currExpr.replace(JetPsiFactory(returnExpression).createReturn(currExpr));
        }

        returnExpression.replace(newWhenExpression);
    }
}
