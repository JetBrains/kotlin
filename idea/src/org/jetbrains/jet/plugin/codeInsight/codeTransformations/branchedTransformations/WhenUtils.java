package org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations;

import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;

public class WhenUtils {
    private WhenUtils() {
    }

    public static final String TRANSFORM_WITHOUT_CHECK =
            "Expression must be checked before applying transformation";

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
                else if (!JetPsiMatcher.checkExpressionMatch(lastCandidate, currCandidate)) return null;
            }
        }

        return lastCandidate;
    }

    public static boolean checkFlattenWhen(@NotNull JetWhenExpression whenExpression) {
        JetExpression subject = whenExpression.getSubjectExpression();

        if (subject != null && !(subject instanceof JetSimpleNameExpression)) return false;

        if (!JetPsiUtil.checkWhenExpressionHasSingleElse(whenExpression)) return false;

        JetExpression elseBranch = JetPsiUtil.getWhenElseBranch(whenExpression);
        if (!(elseBranch instanceof JetWhenExpression)) return false;

        JetWhenExpression nestedWhenExpression = (JetWhenExpression) elseBranch;

        return  JetPsiUtil.checkWhenExpressionHasSingleElse(nestedWhenExpression) &&
                JetPsiMatcher.checkExpressionMatch(subject, nestedWhenExpression.getSubjectExpression());
    }

    public static boolean checkIntroduceWhenSubject(@NotNull JetWhenExpression whenExpression) {
        return getWhenSubjectCandidate(whenExpression) != null;
    }

    public static boolean checkEliminateWhenSubject(@NotNull JetWhenExpression whenExpression) {
        return whenExpression.getSubjectExpression() instanceof JetSimpleNameExpression;
    }

    public static void flattenWhen(@NotNull JetWhenExpression whenExpression) {
        JetExpression subjectExpression = whenExpression.getSubjectExpression();
        boolean hasSubject = subjectExpression != null;

        JetExpression elseBranch = JetPsiUtil.getWhenElseBranch(whenExpression);
        assert elseBranch instanceof JetWhenExpression : TRANSFORM_WITHOUT_CHECK;

        JetWhenExpression nestedWhenExpression = (JetWhenExpression) elseBranch;

        List<JetWhenEntry> outerEntries = whenExpression.getEntries();
        List<JetWhenEntry> innerEntries = nestedWhenExpression.getEntries();

        JetWhenExpression newWhenExpression = new JetPsiFactory.WhenTemplateBuilder(hasSubject)
                .addBranchesWithSingleCondition(outerEntries.size() + innerEntries.size() - 2)
                .toExpression(whenExpression.getProject());

        if (hasSubject) {
            JetExpression dummySubjectExpression = newWhenExpression.getSubjectExpression();
            assert dummySubjectExpression != null : TRANSFORM_WITHOUT_CHECK;

            dummySubjectExpression.replace(subjectExpression);
        }

        List<JetWhenEntry> newEntries = newWhenExpression.getEntries();

        int i = 0;
        for (JetWhenEntry entry : outerEntries) {
            if (!entry.isElse()) {
                newEntries.get(i++).replace(entry);
            }
        }
        for (JetWhenEntry entry : innerEntries) {
            newEntries.get(i++).replace(entry);
        }

        whenExpression.replace(newWhenExpression);
    }

    private static JetWhenExpression createWhenTemplateWithSubject(@NotNull JetWhenExpression whenExpression) {
        JetPsiFactory.WhenTemplateBuilder builder = new JetPsiFactory.WhenTemplateBuilder(true);

        for (JetWhenEntry entry : whenExpression.getEntries()) {
            if (entry.isElse()) continue;

            for (JetWhenCondition condition : entry.getConditions()) {
                assert condition instanceof JetWhenConditionWithExpression : TRANSFORM_WITHOUT_CHECK;

                JetExpression conditionExpression = ((JetWhenConditionWithExpression) condition).getExpression();

                if (conditionExpression instanceof JetIsExpression) {
                    builder.addIsCondition(((JetIsExpression) conditionExpression).isNegated());
                }
                else if (conditionExpression instanceof JetBinaryExpression) {
                    JetBinaryExpression binaryExpression = (JetBinaryExpression) conditionExpression;

                    IElementType op = binaryExpression.getOperationToken();
                    if (op == JetTokens.IN_KEYWORD) {
                        builder.addInCondition(false);
                    }
                    else if (op == JetTokens.NOT_IN) {
                        builder.addInCondition(true);
                    }
                    else if (op == JetTokens.EQEQ) {
                        builder.addExpressionCondition();
                    }
                    else assert false : TRANSFORM_WITHOUT_CHECK;
                }
                else assert false : TRANSFORM_WITHOUT_CHECK;
            }

            builder.finishBranch();
        }

        return builder.toExpression(whenExpression.getProject());
    }

    public static void introduceWhenSubject(@NotNull JetWhenExpression whenExpression) {
        JetExpression subject = getWhenSubjectCandidate(whenExpression);
        assert subject != null : TRANSFORM_WITHOUT_CHECK;

        JetWhenExpression newWhenExpression = createWhenTemplateWithSubject(whenExpression);

        JetExpression newSubject = newWhenExpression.getSubjectExpression();
        assert newSubject != null : TRANSFORM_WITHOUT_CHECK;

        newSubject.replace(subject);

        int i = 0;
        List<JetWhenEntry> entries = whenExpression.getEntries();
        List<JetWhenEntry> newEntries = newWhenExpression.getEntries();
        for (JetWhenEntry newEntry : newEntries) {
            JetWhenEntry entry = entries.get(i++);

            JetExpression branchExpression = entry.getExpression();
            assert branchExpression != null : TRANSFORM_WITHOUT_CHECK;

            JetExpression newBranchExpression = newEntry.getExpression();
            assert newBranchExpression != null : TRANSFORM_WITHOUT_CHECK;

            newBranchExpression.replace(branchExpression);

            int j = 0;
            JetWhenCondition[] conditions = entry.getConditions();
            JetWhenCondition[] newConditions = newEntry.getConditions();

            for (JetWhenCondition newCondition : newConditions) {
                JetWhenCondition condition = conditions[j++];

                assert condition instanceof JetWhenConditionWithExpression : TRANSFORM_WITHOUT_CHECK;

                JetExpression conditionExpression = ((JetWhenConditionWithExpression) condition).getExpression();

                if (conditionExpression instanceof JetIsExpression) {
                    assert newCondition instanceof JetWhenConditionIsPattern : TRANSFORM_WITHOUT_CHECK;

                    JetTypeReference typeReference = ((JetIsExpression) conditionExpression).getTypeRef();
                    assert typeReference != null : TRANSFORM_WITHOUT_CHECK;

                    JetTypeReference newTypeReference = ((JetWhenConditionIsPattern) newCondition).getTypeRef();
                    assert newTypeReference != null : TRANSFORM_WITHOUT_CHECK;

                    newTypeReference.replace(typeReference);
                }
                else if (conditionExpression instanceof JetBinaryExpression) {
                    JetBinaryExpression binaryExpression = (JetBinaryExpression) conditionExpression;

                    JetExpression rhs = binaryExpression.getRight();
                    assert rhs != null : TRANSFORM_WITHOUT_CHECK;

                    IElementType op = binaryExpression.getOperationToken();
                    if (op == JetTokens.IN_KEYWORD || op == JetTokens.NOT_IN) {
                        assert newCondition instanceof JetWhenConditionInRange : TRANSFORM_WITHOUT_CHECK;

                        JetExpression newRangeExpression = ((JetWhenConditionInRange) newCondition).getRangeExpression();
                        assert newRangeExpression != null : TRANSFORM_WITHOUT_CHECK;

                        newRangeExpression.replace(rhs);
                    }
                    else if (op == JetTokens.EQEQ) {
                        assert newCondition instanceof JetWhenConditionWithExpression : TRANSFORM_WITHOUT_CHECK;

                        JetExpression newConditionExpression = ((JetWhenConditionWithExpression) newCondition).getExpression();
                        assert newConditionExpression != null : TRANSFORM_WITHOUT_CHECK;

                        newConditionExpression.replace(rhs);
                    }
                    else assert false : TRANSFORM_WITHOUT_CHECK;
                }
                else assert false : TRANSFORM_WITHOUT_CHECK;
            }
        }

        whenExpression.replace(newWhenExpression);
    }

    private static JetWhenExpression createWhenTemplateWithoutSubject(@NotNull JetWhenExpression whenExpression) {
        JetPsiFactory.WhenTemplateBuilder builder = new JetPsiFactory.WhenTemplateBuilder(false);

        for (JetWhenEntry entry : whenExpression.getEntries()) {
            if (!entry.isElse()) {
                builder.addBranchWithMultiCondition(entry.getConditions().length);
            }
        }

        return builder.toExpression(whenExpression.getProject());
    }

    static JetExpression whenConditionToExpression(@NotNull JetWhenCondition condition, JetExpression subject) {
        Project project = condition.getProject();

        if (condition instanceof JetWhenConditionIsPattern) {
            JetWhenConditionIsPattern patternCondition = (JetWhenConditionIsPattern) condition;

            JetTypeReference typeReference = patternCondition.getTypeRef();
            assert typeReference != null : TRANSFORM_WITHOUT_CHECK;

            assert subject != null : TRANSFORM_WITHOUT_CHECK;

            return JetPsiFactory.createIsExpression(project, subject, typeReference, patternCondition.isNegated());
        }

        if (condition instanceof JetWhenConditionInRange) {
            JetWhenConditionInRange rangeCondition = (JetWhenConditionInRange) condition;

            JetExpression rangeExpression = rangeCondition.getRangeExpression();
            assert rangeExpression != null : TRANSFORM_WITHOUT_CHECK;

            assert subject != null : TRANSFORM_WITHOUT_CHECK;

            return JetPsiFactory.createBinaryExpression(project, subject, rangeCondition.getOperationReference().getText(), rangeExpression);
        }

        assert condition instanceof JetWhenConditionWithExpression : TRANSFORM_WITHOUT_CHECK;

        JetExpression conditionExpression = ((JetWhenConditionWithExpression) condition).getExpression();
        assert conditionExpression != null : TRANSFORM_WITHOUT_CHECK;

        return subject != null ? JetPsiFactory.createBinaryExpression(project, subject, "==", conditionExpression) : conditionExpression;
    }

    public static void eliminateWhenSubject(@NotNull JetWhenExpression whenExpression) {
        JetExpression subject = whenExpression.getSubjectExpression();
        assert subject != null : TRANSFORM_WITHOUT_CHECK;

        JetWhenExpression newWhenExpression = createWhenTemplateWithoutSubject(whenExpression);

        int i = 0;
        List<JetWhenEntry> entries = whenExpression.getEntries();
        List<JetWhenEntry> newEntries = newWhenExpression.getEntries();
        for (JetWhenEntry newEntry : newEntries) {
            JetWhenEntry entry = entries.get(i++);

            JetExpression branchExpression = entry.getExpression();
            assert branchExpression != null : TRANSFORM_WITHOUT_CHECK;

            JetExpression newBranchExpression = newEntry.getExpression();
            assert newBranchExpression != null : TRANSFORM_WITHOUT_CHECK;

            newBranchExpression.replace(branchExpression);

            int j = 0;
            JetWhenCondition[] conditions = entry.getConditions();
            JetWhenCondition[] newConditions = newEntry.getConditions();

            for (JetWhenCondition newCondition : newConditions) {
                JetWhenCondition condition = conditions[j++];

                JetExpression newConditionExpression = ((JetWhenConditionWithExpression) newCondition).getExpression();
                assert newConditionExpression != null : TRANSFORM_WITHOUT_CHECK;

                newConditionExpression.replace(whenConditionToExpression(condition, subject));
            }
        }

        whenExpression.replace(newWhenExpression);
    }
}
