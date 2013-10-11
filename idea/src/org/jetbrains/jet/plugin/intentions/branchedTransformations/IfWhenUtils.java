package org.jetbrains.jet.plugin.intentions.branchedTransformations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.psi.JetPsiUnparsingUtils.*;

public class IfWhenUtils {

    public static final String TRANSFORM_WITHOUT_CHECK =
            "Expression must be checked before applying transformation";

    private IfWhenUtils() {
    }

    public static boolean checkIfToWhen(@NotNull JetIfExpression ifExpression) {
        return ifExpression.getThen() != null;
    }

    public static boolean checkWhenToIf(@NotNull JetWhenExpression whenExpression) {
        return !whenExpression.getEntries().isEmpty();
    }

    private static void assertNotNull(JetExpression expression) {
        assert expression != null : TRANSFORM_WITHOUT_CHECK;
    }

    private static List<JetExpression> splitExpressionToOrBranches(@Nullable JetExpression expression) {
        if (expression == null) return Collections.emptyList();

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
        JetPsiFactory.WhenBuilder builder = new JetPsiFactory.WhenBuilder();

        JetIfExpression currIfExpression = ifExpression;
        do {
            JetExpression condition = currIfExpression.getCondition();
            JetExpression thenBranch = currIfExpression.getThen();
            JetExpression elseBranch = currIfExpression.getElse();

            assertNotNull(thenBranch);

            List<JetExpression> orBranches = splitExpressionToOrBranches(condition);

            if (orBranches.isEmpty()) {
                builder.condition("");
            } else {
                for (JetExpression orBranch : orBranches) {
                    builder.condition(orBranch);
                }
            }

            builder.branchExpression(thenBranch);

            if (elseBranch instanceof JetIfExpression) {
                currIfExpression = (JetIfExpression) elseBranch;
            }
            else {
                currIfExpression = null;
                if (elseBranch != null) {
                    builder.elseEntry(elseBranch);
                }
            }
        } while (currIfExpression != null);

        JetWhenExpression whenExpression = builder.toExpression(ifExpression.getProject());
        if (WhenUtils.checkIntroduceWhenSubject(whenExpression)) {
            whenExpression = WhenUtils.introduceWhenSubject(whenExpression);
        }

        ifExpression.replace(whenExpression);
    }

    private static String combineWhenConditions(JetWhenCondition[] conditions, JetExpression subject) {
        int n = conditions.length;
        if (n == 0) return "";

        JetWhenCondition condition = conditions[0];
        assert condition != null : TRANSFORM_WITHOUT_CHECK;

        StringBuilder sb = new StringBuilder();

        String text = WhenUtils.whenConditionToExpressionText(condition, subject);
        if (n > 1) {
            text = parenthesizeTextIfNeeded(text);
        }
        sb.append(text);

        for (int i = 1; i < n; i++) {
            JetWhenCondition currCondition = conditions[i];
            assert currCondition != null : TRANSFORM_WITHOUT_CHECK;

            sb.append(" || ").append(parenthesizeTextIfNeeded(WhenUtils.whenConditionToExpressionText(currCondition, subject)));
        }

        return sb.toString();
    }

    public static void transformWhenToIf(@NotNull JetWhenExpression whenExpression) {
        JetPsiFactory.IfChainBuilder builder = new JetPsiFactory.IfChainBuilder();

        List<JetWhenEntry> entries = whenExpression.getEntries();
        for (JetWhenEntry entry : entries) {
            JetExpression branch = entry.getExpression();

            if (entry.isElse()) {
                builder.elseBranch(branch);
            } else {
                String branchConditionText = combineWhenConditions(entry.getConditions(), whenExpression.getSubjectExpression());
                builder.ifBranch(branchConditionText, JetPsiUtil.getText(branch));
            }
        }

        whenExpression.replace(builder.toExpression(whenExpression.getProject()));
    }
}
