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

package org.jetbrains.jet.lang.psi;

import java.util.List;

public class JetPsiMatcher {
    private JetPsiMatcher() {
    }

    private static boolean checkTypeReferenceMatch(JetTypeReference t1, JetTypeReference t2) {
        return (t1 == t2) || (t1 != null && t2 != null && t1.getText().equals(t2.getText()));
    }

    private static boolean checkStringTemplateEntryMatch(JetStringTemplateEntry e1, JetStringTemplateEntry e2) {
        if (e1 == e2) return true;
        if (e1 == null || e2 == null) return false;

        return e1.getClass() == e2.getClass() && checkExpressionMatch(e1.getExpression(), e2.getExpression());
    }

    private static boolean checkWhenConditionMatch(JetWhenCondition cond1, JetWhenCondition cond2) {
        if (cond1 == cond2) return true;
        if (cond1 == null || cond2 == null) return false;

        if (cond1.getClass() != cond2.getClass()) return false;

        if (cond1 instanceof JetWhenConditionInRange) {
            JetWhenConditionInRange inRange1 = (JetWhenConditionInRange) cond1;
            JetWhenConditionInRange inRange2 = (JetWhenConditionInRange) cond2;

            return inRange1.isNegated() == inRange2.isNegated() &&
                   checkExpressionMatch(inRange1.getRangeExpression(), inRange2.getRangeExpression()) &&
                   checkExpressionMatch(inRange1.getOperationReference(), inRange2.getOperationReference());
        }

        if (cond1 instanceof JetWhenConditionIsPattern) {
            JetWhenConditionIsPattern pattern1 = (JetWhenConditionIsPattern) cond1;
            JetWhenConditionIsPattern pattern2 = (JetWhenConditionIsPattern) cond2;

            return pattern1.isNegated() == pattern2.isNegated() &&
                   checkTypeReferenceMatch(pattern1.getTypeRef(), pattern2.getTypeRef());
        }

        if (cond1 instanceof JetWhenConditionWithExpression) {
            return checkExpressionMatch(((JetWhenConditionWithExpression) cond1).getExpression(), ((JetWhenConditionWithExpression) cond2).getExpression());
        }

        return false;
    }

    private static boolean checkWhenEntryMatch(JetWhenEntry e1, JetWhenEntry e2) {
        if (e1 == e2) return true;
        if (e1 == null || e2 == null) return false;

        if (!(e1.isElse() == e2.isElse() && checkExpressionMatch(e1.getExpression(), e2.getExpression()))) return false;

        JetWhenCondition[] conditions1 = e1.getConditions();
        JetWhenCondition[] conditions2 = e2.getConditions();

        int n = conditions1.length;
        if (conditions2.length != n) return false;

        for (int i = 0; i < n; i++) {
            if (!checkWhenConditionMatch(conditions1[i], conditions2[i])) return false;
        }

        return true;
    }

    public static boolean checkExpressionMatch(JetExpression e1, JetExpression e2) {
        if (e1 == e2) return true;
        if (e1 == null || e2 == null) return false;

        e1 = JetPsiUtil.deparenthesizeWithNoTypeResolution(e1);
        e2 = JetPsiUtil.deparenthesizeWithNoTypeResolution(e2);

        assert e1 != null && e2 != null;

        if (e1.getClass() != e2.getClass()) return false;

        if (e1 instanceof JetArrayAccessExpression) {
            JetArrayAccessExpression aae1 = (JetArrayAccessExpression) e1;
            JetArrayAccessExpression aae2 = (JetArrayAccessExpression) e2;

            if (!checkExpressionMatch(aae1.getArrayExpression(), aae2.getArrayExpression())) return false;

            List<JetExpression> indexes1 = aae1.getIndexExpressions();
            List<JetExpression> indexes2 = aae2.getIndexExpressions();

            int n = indexes1.size();
            if (indexes2.size() != n) return false;

            for (int i = 0; i < n; i++) {
                if (!checkExpressionMatch(indexes1.get(i), indexes2.get(i))) return false;
            }

            return true;
        }

        if (e1 instanceof JetBinaryExpression) {
            JetBinaryExpression be1 = (JetBinaryExpression) e1;
            JetBinaryExpression be2 = (JetBinaryExpression) e2;

            return be1.getOperationToken() == be2.getOperationToken() &&
                   checkExpressionMatch(be1.getLeft(), be2.getLeft()) &&
                   checkExpressionMatch(be1.getRight(), be2.getRight());
        }

        if (e1 instanceof JetBinaryExpressionWithTypeRHS) {
            JetBinaryExpressionWithTypeRHS bet1 = (JetBinaryExpressionWithTypeRHS) e1;
            JetBinaryExpressionWithTypeRHS bet2 = (JetBinaryExpressionWithTypeRHS) e2;

            return checkExpressionMatch(bet1.getLeft(), bet2.getLeft()) &&
                   checkTypeReferenceMatch(bet1.getRight(), bet2.getRight());
        }

        if (e1 instanceof JetCallExpression) {
            JetCallExpression call1 = (JetCallExpression) e1;
            JetCallExpression call2 = (JetCallExpression) e2;

            if (!checkExpressionMatch(call1.getCalleeExpression(), call2.getCalleeExpression())) return false;

            List<? extends ValueArgument> args1 = call1.getValueArguments();
            List<? extends ValueArgument> args2 = call2.getValueArguments();

            int argCount = args1.size();
            if (args2.size() != argCount) return false;

            for (int i = 0; i < argCount; i++) {
                if (!checkExpressionMatch(args1.get(i).getArgumentExpression(), args2.get(i).getArgumentExpression())) return false;
            }

            List<JetExpression> funLiterals1 = call1.getFunctionLiteralArguments();
            List<JetExpression> funLiterals2 = call2.getFunctionLiteralArguments();

            int funLiteralCount = funLiterals1.size();
            if (funLiterals2.size() != funLiteralCount) return false;

            for (int i = 0; i < argCount; i++) {
                if (!checkExpressionMatch(funLiterals1.get(i), funLiterals2.get(i))) return false;
            }

            return true;
        }

        if (e1 instanceof JetConstantExpression || e1 instanceof JetSimpleNameExpression) {
            return e1.getText().equals(e2.getText());
        }

        if (e1 instanceof JetConstructorCalleeExpression) {
            return checkTypeReferenceMatch(((JetConstructorCalleeExpression) e1).getTypeReference(), ((JetConstructorCalleeExpression) e2).getTypeReference());
        }

        if (e1 instanceof JetQualifiedExpression) {
            return  ((JetQualifiedExpression) e1).getOperationSign() == ((JetQualifiedExpression) e2).getOperationSign() &&
                    checkExpressionMatch(((JetQualifiedExpression) e1).getReceiverExpression(), ((JetQualifiedExpression) e2).getReceiverExpression()) &&
                    checkExpressionMatch(((JetQualifiedExpression) e1).getSelectorExpression(), ((JetQualifiedExpression) e2).getSelectorExpression());
        }

        if (e1 instanceof JetIfExpression) {
            return checkExpressionMatch(((JetIfExpression) e1).getCondition(), ((JetIfExpression) e2).getCondition()) &&
                   checkExpressionMatch(((JetIfExpression) e1).getThen(), ((JetIfExpression) e2).getThen()) &&
                   checkExpressionMatch(((JetIfExpression) e1).getElse(), ((JetIfExpression) e2).getElse());
        }

        if (e1 instanceof JetIsExpression) {
            return checkExpressionMatch(((JetIsExpression) e1).getLeftHandSide(), ((JetIsExpression) e2).getLeftHandSide()) &&
                   checkTypeReferenceMatch(((JetIsExpression) e1).getTypeRef(), ((JetIsExpression) e2).getTypeRef()) &&
                   ((JetIsExpression) e1).isNegated() == ((JetIsExpression) e2).isNegated();
        }

        if (e1 instanceof JetStringTemplateExpression) {
            JetStringTemplateExpression str1 = (JetStringTemplateExpression) e1;
            JetStringTemplateExpression str2 = (JetStringTemplateExpression) e2;

            JetStringTemplateEntry[] entries1 = str1.getEntries();
            JetStringTemplateEntry[] entries2 = str2.getEntries();

            int n = entries1.length;
            if (entries2.length != n) return false;

            for (int i = 0; i < n; i++) {
                if (!checkStringTemplateEntryMatch(entries1[i], entries2[i])) return false;
            }

            return true;
        }

        if (e1 instanceof JetThrowExpression) {
            return checkExpressionMatch(((JetThrowExpression) e1).getThrownExpression(), ((JetThrowExpression) e2).getThrownExpression());
        }

        if (e1 instanceof JetUnaryExpression) {
            JetUnaryExpression ue1 = (JetUnaryExpression) e1;
            JetUnaryExpression ue2 = (JetUnaryExpression) e2;

            return checkExpressionMatch(ue1.getBaseExpression(), ue2.getBaseExpression()) &&
                   checkExpressionMatch(ue1.getOperationReference(), ue2.getOperationReference());
        }

        if (e1 instanceof JetWhenExpression) {
            JetWhenExpression when1 = (JetWhenExpression) e1;
            JetWhenExpression when2 = (JetWhenExpression) e2;

            if (checkExpressionMatch(when1.getSubjectExpression(), when2.getSubjectExpression())) return false;

            List<JetWhenEntry> entries1 = when1.getEntries();
            List<JetWhenEntry> entries2 = when2.getEntries();

            int n = entries1.size();
            if (entries2.size() != n) return false;

            for (int i = 0; i < n; i++) {
                if (!checkWhenEntryMatch(entries1.get(i), entries2.get(i))) return false;
            }
        }

        if (e1 instanceof JetThisExpression) return true;

        if (e1 instanceof JetSuperExpression) {
            JetSuperExpression super1 = (JetSuperExpression) e1;
            JetSuperExpression super2 = (JetSuperExpression) e2;

            return checkTypeReferenceMatch(super1.getSuperTypeQualifier(), super2.getSuperTypeQualifier()) &&
                   checkExpressionMatch(super1.getInstanceReference(), super2.getInstanceReference()) &&
                   checkExpressionMatch(super1.getTargetLabel(), super2.getTargetLabel());
        }

        return false;
    }
}
