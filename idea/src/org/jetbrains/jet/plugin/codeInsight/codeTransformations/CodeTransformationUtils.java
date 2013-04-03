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

package org.jetbrains.jet.plugin.codeInsight.codeTransformations;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.List;

public class CodeTransformationUtils {
    private static List<JetExpression> getIfExpressionOutcomes(@NotNull JetElement root) {
        return root.accept(
                new JetVisitor<List<JetExpression>, List<JetExpression>>() {
                    @Override
                    public List<JetExpression> visitExpression(JetExpression expression, List<JetExpression> data) {
                        data.add(expression);
                        return data;
                    }

                    @Override
                    public List<JetExpression> visitBlockExpression(
                            JetBlockExpression expression, List<JetExpression> data) {
                        int n = expression.getStatements().size();
                        if (n > 0) {
                            expression.getStatements().get(n - 1).accept(this, data);
                        } else {
                            data.add(expression);
                        }

                        return data;
                    }

                    @SuppressWarnings("ConstantConditions")
                    @Override
                    public List<JetExpression> visitIfExpression(
                            JetIfExpression expression, List<JetExpression> data) {
                        if (expression.getThen() != null)  {
                            expression.getThen().accept(this, data);
                        }
                        if (expression.getElse() != null)  {
                            expression.getElse().accept(this, data);
                        }
                        return data;
                    }
                },
                new ArrayList<JetExpression>()
        );
    }

    private static Boolean checkAllIfExpressionsAreComplete(@NotNull JetElement root) {
        return root.accept(
                new JetVisitor<Boolean, Boolean>() {
                    @Override
                    public Boolean visitJetElement(JetElement element, Boolean data) {
                        return data;
                    }

                    @SuppressWarnings("ConstantConditions")
                    @Override
                    public Boolean visitIfExpression(JetIfExpression expression, Boolean data) {
                        if (data && expression.getThen() != null)  {
                            data = expression.getThen().accept(this, data);
                        } else {
                            data = false;
                        }
                        if (data && expression.getElse() != null)  {
                            data = expression.getElse().accept(this, data);
                        } else {
                            data = false;
                        }

                        return data;
                    }
                },
                true
        );
    }

    public static boolean isAssignment(@NotNull PsiElement element) {
        if (!(element instanceof JetBinaryExpression)) return false;
        JetBinaryExpression binaryExpression = (JetBinaryExpression)element;
        if (binaryExpression.getOperationReference().getReferencedNameElementType() != JetTokens.EQ) return false;
        return true;
    }

    private static boolean checkAllOutcomesAreCompatibleAssignments(@NotNull List<JetExpression> outcomes) {
        JetExpression lastLhs = null;
        for (JetExpression outcome : outcomes) {
            if (!isAssignment(outcome)) return false;

            JetExpression currLhs = ((JetBinaryExpression)outcome).getLeft();
            if (!(currLhs instanceof JetSimpleNameExpression)) return false;

            if (lastLhs == null) {
                lastLhs = currLhs;
            } else if (!lastLhs.getText().equals(currLhs.getText())) return false;
        }

        return true;
    }

    static boolean checkIfStatementWithAssignments(@NotNull JetIfExpression ifExpression) {
        if (ifExpression.getParent() == null) return false;
        List<JetExpression> outcomes = getIfExpressionOutcomes(ifExpression);

        return !outcomes.isEmpty() && checkAllIfExpressionsAreComplete(ifExpression) && checkAllOutcomesAreCompatibleAssignments(outcomes);
    }

    static boolean checkAssignmentWithIfExpression(@NotNull PsiElement element) {
        if (!isAssignment(element)) return false;
        JetBinaryExpression assignment = (JetBinaryExpression)element;
        return (assignment.getLeft() instanceof JetSimpleNameExpression) &&
               (assignment.getRight() instanceof JetIfExpression);
    }

    @SuppressWarnings("ConstantConditions")
    static void transformIfStatementWithAssignmentsToExpression(@NotNull JetIfExpression ifExpression) {
        Project project = ifExpression.getProject();
        List<JetExpression> outcomes = getIfExpressionOutcomes(ifExpression);
        JetExpression lhs = ((JetBinaryExpression)outcomes.get(0)).getLeft();

        JetBinaryExpression assignment = (JetBinaryExpression)JetPsiFactory.createExpression(project, "a = b");

        assignment = (JetBinaryExpression)assignment.getLeft().replace(lhs).getParent();
        assignment = (JetBinaryExpression)assignment.getRight().replace(ifExpression).getParent();
        assignment = (JetBinaryExpression)ifExpression.replace(assignment);
        ifExpression = (JetIfExpression)assignment.getRight();

        for (JetExpression outcome : getIfExpressionOutcomes(ifExpression)) {
            outcome.replace(((JetBinaryExpression)outcome).getRight());
        }
    }

    @SuppressWarnings("ConstantConditions")
    static void transformAssignmentWithIfExpressionToStatement(@NotNull JetBinaryExpression assignment) {
        Project project = assignment.getProject();
        String varName = assignment.getLeft().getText();
        JetIfExpression ifExpression = (JetIfExpression)assignment.getRight();

        ifExpression = (JetIfExpression)assignment.replace(ifExpression);

        for (JetExpression outcome : getIfExpressionOutcomes(ifExpression)) {
            JetBinaryExpression localAssignment = (JetBinaryExpression)JetPsiFactory.createExpression(project, "a = b");
            localAssignment = (JetBinaryExpression)localAssignment.getLeft().replace(JetPsiFactory.createExpression(project, varName)).getParent();
            localAssignment = (JetBinaryExpression)localAssignment.getRight().replace(outcome).getParent();
            outcome.replace(localAssignment);
        }
    }

    private CodeTransformationUtils() {
    }
}
