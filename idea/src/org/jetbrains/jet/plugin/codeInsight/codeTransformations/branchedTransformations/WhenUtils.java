package org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

import static org.jetbrains.jet.lang.psi.JetPsiUnparsingUtils.*;

import java.util.List;

public class WhenUtils {
    private WhenUtils() {
    }

    public static final String TRANSFORM_WITHOUT_CHECK =
            "Expression must be checked before applying transformation";

    private static void assertNotNull(Object expression) {
        assert expression != null : TRANSFORM_WITHOUT_CHECK;
    }

    private static JetExpression getWhenConditionSubjectCandidate(JetExpression condition) {
        if (condition instanceof JetIsExpression) {
            return ((JetIsExpression) condition).getLeftHandSide();
        }

        if (condition instanceof JetBinaryExpression) {
            JetBinaryExpression binaryExpression = (JetBinaryExpression) condition;
            IElementType op = binaryExpression.getOperationToken();
            if (op == JetTokens.EQEQ || op == JetTokens.IN_KEYWORD || op == JetTokens.NOT_IN) {
                return ((JetBinaryExpression) condition).getLeft();
            }
        }

        return null;
    }

    private static JetExpression getWhenSubjectCandidate(@NotNull JetWhenExpression whenExpression) {
        if (whenExpression.getSubjectExpression() != null) return null;

        JetExpression lastCandidate = null;
        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JetWhenCondition[] conditions = entry.getConditions();

            if (!entry.isElse() && conditions.length == 0) return null;

            for (JetWhenCondition condition : conditions) {
                if (!(condition instanceof JetWhenConditionWithExpression)) return null;

                JetExpression currCandidate = getWhenConditionSubjectCandidate(((JetWhenConditionWithExpression) condition).getExpression());

                if (!(currCandidate instanceof JetSimpleNameExpression)) return null;

                if (lastCandidate == null) {
                    lastCandidate = currCandidate;
                }
                else if (!JetPsiMatcher.checkElementMatch(lastCandidate, currCandidate)) return null;
            }
        }

        return lastCandidate;
    }

    public static boolean checkFlattenWhen(@NotNull JetWhenExpression whenExpression) {
        JetExpression subject = whenExpression.getSubjectExpression();

        if (subject != null && !(subject instanceof JetSimpleNameExpression)) return false;

        if (!JetPsiUtil.checkWhenExpressionHasSingleElse(whenExpression)) return false;

        JetExpression elseBranch = whenExpression.getElseExpression();
        if (!(elseBranch instanceof JetWhenExpression)) return false;

        JetWhenExpression nestedWhenExpression = (JetWhenExpression) elseBranch;

        return  JetPsiUtil.checkWhenExpressionHasSingleElse(nestedWhenExpression) &&
                JetPsiMatcher.checkElementMatch(subject, nestedWhenExpression.getSubjectExpression());
    }

    public static boolean checkIntroduceWhenSubject(@NotNull JetWhenExpression whenExpression) {
        return getWhenSubjectCandidate(whenExpression) != null;
    }

    public static boolean checkEliminateWhenSubject(@NotNull JetWhenExpression whenExpression) {
        return whenExpression.getSubjectExpression() instanceof JetSimpleNameExpression;
    }

    public static void flattenWhen(@NotNull JetWhenExpression whenExpression) {
        JetExpression subjectExpression = whenExpression.getSubjectExpression();

        JetExpression elseBranch = whenExpression.getElseExpression();
        assert elseBranch instanceof JetWhenExpression : TRANSFORM_WITHOUT_CHECK;

        JetWhenExpression nestedWhenExpression = (JetWhenExpression) elseBranch;

        List<JetWhenEntry> outerEntries = whenExpression.getEntries();
        List<JetWhenEntry> innerEntries = nestedWhenExpression.getEntries();

        JetPsiFactory.WhenBuilder builder = new JetPsiFactory.WhenBuilder(subjectExpression);

        for (JetWhenEntry entry : outerEntries) {
            if (entry.isElse()) continue;

            builder.entry(entry);
        }

        for (JetWhenEntry entry : innerEntries) {
            builder.entry(entry);
        }

        whenExpression.replace(builder.toExpression(whenExpression.getProject()));
    }

    public static void introduceWhenSubject(@NotNull JetWhenExpression whenExpression) {
        JetExpression subject = getWhenSubjectCandidate(whenExpression);
        assertNotNull(subject);

        JetPsiFactory.WhenBuilder builder = new JetPsiFactory.WhenBuilder(subject);

        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JetExpression branchExpression = entry.getExpression();

            if (entry.isElse()) {
                builder.elseEntry(branchExpression);
                continue;
            }

            for (JetWhenCondition condition : entry.getConditions()) {
                assert condition instanceof JetWhenConditionWithExpression : TRANSFORM_WITHOUT_CHECK;

                JetExpression conditionExpression = ((JetWhenConditionWithExpression) condition).getExpression();

                if (conditionExpression instanceof JetIsExpression) {
                    JetIsExpression isExpression = (JetIsExpression) conditionExpression;
                    builder.pattern(isExpression.getTypeRef(), isExpression.isNegated());
                }
                else if (conditionExpression instanceof JetBinaryExpression) {
                    JetBinaryExpression binaryExpression = (JetBinaryExpression) conditionExpression;

                    JetExpression rhs = binaryExpression.getRight();

                    IElementType op = binaryExpression.getOperationToken();
                    if (op == JetTokens.IN_KEYWORD) {
                        builder.range(rhs, false);
                    }
                    else if (op == JetTokens.NOT_IN) {
                        builder.range(rhs, true);
                    }
                    else if (op == JetTokens.EQEQ) {
                        builder.condition(rhs);
                    }
                    else assert false : TRANSFORM_WITHOUT_CHECK;
                }
                else assert false : TRANSFORM_WITHOUT_CHECK;
            }

            builder.branchExpression(branchExpression);
        }

        whenExpression.replace(builder.toExpression(whenExpression.getProject()));
    }

    static String whenConditionToExpressionText(@NotNull JetWhenCondition condition, JetExpression subject) {
        if (condition instanceof JetWhenConditionIsPattern) {
            JetWhenConditionIsPattern patternCondition = (JetWhenConditionIsPattern) condition;
            return toBinaryExpression(subject, (patternCondition.isNegated() ? "!is" : "is"), patternCondition.getTypeRef());
        }

        if (condition instanceof JetWhenConditionInRange) {
            JetWhenConditionInRange rangeCondition = (JetWhenConditionInRange) condition;
            return toBinaryExpression(subject, rangeCondition.getOperationReference().getText(), rangeCondition.getRangeExpression());
        }

        assert condition instanceof JetWhenConditionWithExpression : TRANSFORM_WITHOUT_CHECK;

        JetExpression conditionExpression = ((JetWhenConditionWithExpression) condition).getExpression();

        if (subject != null) {
            return toBinaryExpression(parenthesizeIfNeeded(subject), "==", parenthesizeIfNeeded(conditionExpression));
        }
        return conditionExpression != null ? conditionExpression.getText() : "";
    }

    public static void eliminateWhenSubject(@NotNull JetWhenExpression whenExpression) {
        JetExpression subject = whenExpression.getSubjectExpression();
        assertNotNull(subject);

        JetPsiFactory.WhenBuilder builder = new JetPsiFactory.WhenBuilder();

        for (JetWhenEntry entry : whenExpression.getEntries()) {
            JetExpression branchExpression = entry.getExpression();

            if (entry.isElse()) {
                builder.elseEntry(branchExpression);

                continue;
            }

            for (JetWhenCondition condition : entry.getConditions()) {
                builder.condition(whenConditionToExpressionText(condition, subject));
            }

            builder.branchExpression(branchExpression);
        }

        whenExpression.replace(builder.toExpression(whenExpression.getProject()));
    }
}
