package org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.List;

public class IfWhenUtils {

    public static final String TRANSFORM_WITHOUT_CHECK =
            "Expression must be checked before applying transformation";

    private IfWhenUtils() {
    }

    public static boolean checkIfToWhen(@NotNull JetIfExpression ifExpression) {
        return ifExpression.getCondition() != null && ifExpression.getThen() != null && ifExpression.getElse() != null;
    }

    public static boolean checkWhenToIf(@NotNull JetWhenExpression whenExpression) {
        return !whenExpression.getEntries().isEmpty() && JetPsiUtil.checkWhenExpressionHasSingleElse(whenExpression);
    }

    private static void assertNotNull(JetExpression expression) {
        assert expression != null : TRANSFORM_WITHOUT_CHECK;
    }

    private static List<JetExpression> splitExpressionToOrBranches(JetExpression expression) {
        final List<JetExpression> branches = new ArrayList<JetExpression>();

        expression.accept(
                new JetVisitorVoid() {
                    @Override
                    public void visitBinaryExpression(JetBinaryExpression expression) {
                        if (expression.getOperationToken() == JetTokens.OROR) {
                            JetExpression left = expression.getLeft();
                            JetExpression right = expression.getRight();

                            if (left != null) {
                                left.accept(this);
                            }

                            if (right != null) {
                                right.accept(this);
                            }
                        } else {
                            visitExpression(expression);
                        }
                    }

                    @Override
                    public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
                        JetExpression baseExpression = expression.getExpression();

                        if (baseExpression != null) {
                            baseExpression.accept(this);
                        }
                    }

                    @Override
                    public void visitExpression(JetExpression expression) {
                        branches.add(expression);
                    }
                }
        );

        return branches;
    }

    public static void transformIfToWhen(@NotNull JetIfExpression ifExpression) {
        List<MultiGuardedExpression> positiveBranches = new ArrayList<MultiGuardedExpression>();
        JetExpression elseExpression = null;

        JetIfExpression currIfExpression = ifExpression;
        do {
            JetExpression condition = currIfExpression.getCondition();
            JetExpression thenBranch = currIfExpression.getThen();
            JetExpression elseBranch = currIfExpression.getElse();

            assertNotNull(condition);
            assertNotNull(thenBranch);
            assertNotNull(elseBranch);

            //noinspection ConstantConditions
            positiveBranches.add(new MultiGuardedExpression(splitExpressionToOrBranches(condition), thenBranch));

            if (elseBranch instanceof JetIfExpression) {
                currIfExpression = (JetIfExpression) elseBranch;
            }
            else {
                currIfExpression = null;
                elseExpression = elseBranch;
            }
        } while (currIfExpression != null);

        JetPsiFactory.WhenTemplateBuilder builder = new JetPsiFactory.WhenTemplateBuilder(false);
        for (MultiGuardedExpression positiveBranch : positiveBranches) {
            builder.addBranchWithMultiCondition(positiveBranch.getConditions().size());
        }

        JetWhenExpression whenExpression = builder.toExpression(ifExpression.getProject());

        int i = 0;
        List<JetWhenEntry> entries = whenExpression.getEntries();
        for (JetWhenEntry entry : entries) {
            if (entry.isElse()) {
                //noinspection ConstantConditions
                entry.getExpression().replace(elseExpression);
                break;
            }

            MultiGuardedExpression branch = positiveBranches.get(i++);

            //noinspection ConstantConditions
            entry.getExpression().replace(branch.getBaseExpression());

            int j = 0;
            JetWhenCondition[] conditions = entry.getConditions();
            for (JetWhenCondition condition : conditions) {
                assert condition instanceof JetWhenConditionWithExpression : TRANSFORM_WITHOUT_CHECK;

                JetExpression conditionExpression = ((JetWhenConditionWithExpression) condition).getExpression();
                assertNotNull(conditionExpression);

                //noinspection ConstantConditions
                conditionExpression.replace(branch.getConditions().get(j++));
            }
        }

        ifExpression.replace(whenExpression);
    }

    @SuppressWarnings("ConstantConditions")
    private static JetExpression combineWhenConditions(Project project, JetWhenCondition[] conditions, JetExpression subject) {
        int n = conditions.length;
        assert n > 0 : TRANSFORM_WITHOUT_CHECK;

        JetWhenCondition condition = conditions[n - 1];
        assert condition != null : TRANSFORM_WITHOUT_CHECK;

        JetExpression resultExpr = WhenUtils.whenConditionToExpression(condition, subject);
        if (n > 1) {
            resultExpr = JetPsiFactory.createParenthesizedExpressionIfNeeded(project, resultExpr);
        }

        for (int i = n - 2; i >= 0; i--) {
            JetWhenCondition currCondition = conditions[i];

            assert currCondition != null : TRANSFORM_WITHOUT_CHECK;

            resultExpr = JetPsiFactory.createBinaryExpression(
                    project,
                    JetPsiFactory.createParenthesizedExpressionIfNeeded(project, WhenUtils.whenConditionToExpression(currCondition, subject)),
                    "||",
                    resultExpr);
        }

        return resultExpr;
    }

    public static void transformWhenToIf(@NotNull JetWhenExpression whenExpression) {
        Project project = whenExpression.getProject();

        JetExpression elseExpression = null;
        List<GuardedExpression> positiveBranches = new ArrayList<GuardedExpression>();

        List<JetWhenEntry> entries = whenExpression.getEntries();
        for (JetWhenEntry entry : entries) {
            JetExpression branch = entry.getExpression();

            assertNotNull(branch);

            if (entry.isElse()) {
                elseExpression = branch;
            } else {
                JetExpression branchCondition = combineWhenConditions(project, entry.getConditions(), whenExpression.getSubjectExpression());
                JetExpression branchExpression = entry.getExpression();

                assertNotNull(branchExpression);

                //noinspection ConstantConditions
                positiveBranches.add(new GuardedExpression(branchCondition, branchExpression));
            }
        }

        assertNotNull(elseExpression);
        assert !positiveBranches.isEmpty() : TRANSFORM_WITHOUT_CHECK;

        JetExpression outerExpression = elseExpression;

        for (int i = positiveBranches.size() - 1; i >= 0; i--) {
            GuardedExpression branch = positiveBranches.get(i);

            outerExpression = JetPsiFactory.createIf(
                    project,
                    branch.getCondition(), branch.getBaseExpression(), outerExpression,
                    !(branch.getBaseExpression() instanceof JetBlockExpression), !(outerExpression instanceof JetBlockExpression));
        }

        //noinspection ConstantConditions
        whenExpression.replace(outerExpression);
    }
}
