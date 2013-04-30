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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class JetPsiMatcher {
    private JetPsiMatcher() {
    }

    private static boolean checkTypeReferenceMatch(JetTypeReference t1, JetTypeReference t2) {
        return (t1 == t2) || (t1 != null && t2 != null && t1.getText().equals(t2.getText()));
    }

    private interface Predicate2<A, B> {
        boolean apply(A a, B b);
    }

    private static final Predicate2<JetElement, JetElement> DEFAULT_CHECKER = new Predicate2<JetElement, JetElement>() {
        @Override
        public boolean apply(JetElement element1, JetElement element2) {
            return checkElementMatch(element1, element2);
        }
    };

    private static final Predicate2<ValueArgument, ValueArgument> VALUE_ARGUMENT_CHECKER = new Predicate2<ValueArgument, ValueArgument>() {
        @Override
        public boolean apply(ValueArgument a1, ValueArgument a2) {
            if (a1 == a2) return true;
            if (a1 == null || a2 == null) return false;

            if (a1.getClass() != a2.getClass()) return false;

            if (a1.isNamed() != a2.isNamed()) return false;

            if (!checkElementMatch(a1.getArgumentExpression(), a2.getArgumentExpression())) return false;

            if (a1.isNamed()) {
                return checkElementMatch(a1.getArgumentName(), a2.getArgumentName());
            }

            return true;
        }
    };

    private static <T> boolean checkListMatch(List<? extends T> list1, List<? extends T> list2, Predicate2<T, T> checker) {
        int n = list1.size();
        if (list2.size() != n) return false;

        for (int i = 0; i < n; i++) {
            if (!checker.apply(list1.get(i), list2.get(i))) return false;
        }

        return true;
    }

    private static <T extends JetElement> boolean checkListMatch(List<? extends T> list1, List<? extends T> list2) {
        return checkListMatch(list1, list2, DEFAULT_CHECKER);
    }

    private static final JetVisitor<Boolean, JetElement> VISITOR = new JetVisitor<Boolean, JetElement>() {
        @Override
        public Boolean visitJetElement(JetElement element, JetElement data) {
            return false;
        }

        @Override
        public Boolean visitArrayAccessExpression(JetArrayAccessExpression aae1, JetElement data) {
            JetArrayAccessExpression aae2 = (JetArrayAccessExpression) data;

            return checkElementMatch(aae1.getArrayExpression(), aae2.getArrayExpression()) &&
                   checkListMatch(aae1.getIndexExpressions(), aae2.getIndexExpressions());
        }

        @Override
        public Boolean visitBinaryExpression(JetBinaryExpression be1, JetElement data) {
            JetBinaryExpression be2 = (JetBinaryExpression) data;

            return be1.getOperationToken() == be2.getOperationToken() &&
                   checkElementMatch(be1.getLeft(), be2.getLeft()) &&
                   checkElementMatch(be1.getRight(), be2.getRight());
        }

        @Override
        public Boolean visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS bet1, JetElement data) {
            JetBinaryExpressionWithTypeRHS bet2 = (JetBinaryExpressionWithTypeRHS) data;

            return checkElementMatch(bet1.getLeft(), bet2.getLeft()) &&
                   checkTypeReferenceMatch(bet1.getRight(), bet2.getRight());
        }

        @Override
        public Boolean visitCallExpression(JetCallExpression call1, JetElement data) {
            JetCallExpression call2 = (JetCallExpression) data;

            if (!checkElementMatch(call1.getCalleeExpression(), call2.getCalleeExpression())) return false;

            return checkListMatch(call1.getValueArguments(), call2.getValueArguments(), VALUE_ARGUMENT_CHECKER) &&
                   checkListMatch(call1.getFunctionLiteralArguments(), call2.getFunctionLiteralArguments());
        }

        @Override
        public Boolean visitSimpleNameExpression(JetSimpleNameExpression expression, JetElement data) {
            return expression.getText().equals(data.getText());
        }

        @Override
        public Boolean visitConstantExpression(JetConstantExpression expression, JetElement data) {
            return expression.getText().equals(data.getText());
        }

        @Override
        public Boolean visitIsExpression(JetIsExpression is1, JetElement data) {
            JetIsExpression is2 = (JetIsExpression) data;

            return checkElementMatch(is1.getLeftHandSide(), is2.getLeftHandSide()) &&
                   checkTypeReferenceMatch(is1.getTypeRef(), is2.getTypeRef()) &&
                   is1.isNegated() == is2.isNegated();
        }

        @Override
        public Boolean visitQualifiedExpression(JetQualifiedExpression qe1, JetElement data) {
            JetQualifiedExpression qe2 = (JetQualifiedExpression) data;

            return qe1.getOperationSign() == qe2.getOperationSign() &&
                   checkElementMatch(qe1.getReceiverExpression(), qe2.getReceiverExpression()) &&
                   checkElementMatch(qe1.getSelectorExpression(), qe2.getSelectorExpression());
        }

        @Override
        public Boolean visitStringTemplateExpression(JetStringTemplateExpression expression, JetElement data) {
            return checkListMatch(
                    Arrays.asList(expression.getEntries()),
                    Arrays.asList(((JetStringTemplateExpression) data).getEntries())
            );
        }

        @Override
        public Boolean visitStringTemplateEntryWithExpression(JetStringTemplateEntryWithExpression entry, JetElement data) {
            return checkElementMatch(entry.getExpression(), ((JetStringTemplateEntryWithExpression) data).getExpression());
        }

        @Override
        public Boolean visitLiteralStringTemplateEntry(JetLiteralStringTemplateEntry entry, JetElement data) {
            return entry.getText().equals(data.getText());
        }

        @Override
        public Boolean visitEscapeStringTemplateEntry(JetEscapeStringTemplateEntry entry, JetElement data) {
            return entry.getText().equals(data.getText());
        }

        @Override
        public Boolean visitSuperExpression(JetSuperExpression super1, JetElement data) {
            JetSuperExpression super2 = (JetSuperExpression) data;

            return checkTypeReferenceMatch(super1.getSuperTypeQualifier(), super2.getSuperTypeQualifier()) &&
                   checkElementMatch(super1.getInstanceReference(), super2.getInstanceReference()) &&
                   checkElementMatch(super1.getTargetLabel(), super2.getTargetLabel());
        }

        @Override
        public Boolean visitThrowExpression(JetThrowExpression expression, JetElement data) {
            return checkElementMatch(expression.getThrownExpression(), ((JetThrowExpression) data).getThrownExpression());
        }

        @Override
        public Boolean visitThisExpression(JetThisExpression this1, JetElement data) {
            return checkElementMatch(this1.getTargetLabel(), ((JetThisExpression) data).getTargetLabel());
        }

        @Override
        public Boolean visitUnaryExpression(JetUnaryExpression ue1, JetElement data) {
            JetUnaryExpression ue2 = (JetUnaryExpression) data;

            return checkElementMatch(ue1.getBaseExpression(), ue2.getBaseExpression()) &&
                   checkElementMatch(ue1.getOperationReference(), ue2.getOperationReference());
        }

        @Override
        public Boolean visitTypeReference(JetTypeReference typeReference, JetElement data) {
            return checkElementMatch(typeReference.getTypeElement(), ((JetTypeReference) data).getTypeElement());
        }

        @Override
        public Boolean visitFunctionType(JetFunctionType type1, JetElement data) {
            JetFunctionType type2 = (JetFunctionType) data;

            return checkListMatch(type1.getTypeArgumentsAsTypes(), type2.getTypeArgumentsAsTypes());
        }

        @Override
        public Boolean visitUserType(JetUserType type1, JetElement data) {
            JetUserType type2 = (JetUserType) data;

            return checkElementMatch(type1.getReferenceExpression(), type2.getReferenceExpression()) &&
                   checkElementMatch(type1.getQualifier(), type2.getQualifier()) &&
                   checkListMatch(type1.getTypeArgumentsAsTypes(), type2.getTypeArgumentsAsTypes());
        }

        @Override
        public Boolean visitSelfType(JetSelfType type, JetElement data) {
            return true;
        }

        @Override
        public Boolean visitNullableType(JetNullableType nullableType, JetElement data) {
            return checkElementMatch(nullableType.getInnerType(), ((JetNullableType) data).getInnerType());
        }
    };

    private static JetElement unwrap(JetElement e) {
        if (e instanceof JetExpression) {
            return JetPsiUtil.deparenthesizeWithNoTypeResolution((JetExpression) e);
        }
        return e;
    }

    public static boolean checkElementMatch(@Nullable JetElement e1, @Nullable JetElement e2) {
        e1 = unwrap(e1);
        e2 = unwrap(e2);

        if (e1 == e2) return true;
        if (e1 == null || e2 == null) return false;

        if (e1.getClass() != e2.getClass()) return false;

        return e1.accept(VISITOR, e2);
    }
}
