/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls.util;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.psi.Call.CallType;

import java.util.Collections;
import java.util.List;

public class CallMaker {

    private static class ExpressionValueArgument implements ValueArgument {

        private final JetExpression expression;

        private final PsiElement reportErrorsOn;

        private ExpressionValueArgument(@Nullable JetExpression expression, @NotNull PsiElement reportErrorsOn) {
            this.expression = expression;
            this.reportErrorsOn = expression == null ? reportErrorsOn : expression;
        }

        @Override
        public JetExpression getArgumentExpression() {
            return expression;
        }

        @Override
        public JetValueArgumentName getArgumentName() {
            return null;
        }

        @Override
        public boolean isNamed() {
            return false;
        }

        @NotNull
        @Override
        public PsiElement asElement() {
            return reportErrorsOn;
        }

        @Override
        public LeafPsiElement getSpreadElement() {
            return null;
        }
    }

    private static class CallImpl implements Call {

        private final PsiElement callElement;
        private final ReceiverValue explicitReceiver;
        private final ASTNode callOperationNode;
        private final JetExpression calleeExpression;
        private final List<? extends ValueArgument> valueArguments;
        private final Call.CallType callType;

        protected CallImpl(@NotNull PsiElement callElement, @NotNull ReceiverValue explicitReceiver, @Nullable ASTNode callOperationNode, @Nullable JetExpression calleeExpression, @NotNull List<? extends ValueArgument> valueArguments) {
            this(callElement, explicitReceiver, callOperationNode, calleeExpression, valueArguments, CallType.DEFAULT);
        }

        protected CallImpl(@NotNull PsiElement callElement, @NotNull ReceiverValue explicitReceiver, @Nullable ASTNode callOperationNode,
                @Nullable JetExpression calleeExpression, @NotNull List<? extends ValueArgument> valueArguments, @NotNull CallType callType) {
            this.callElement = callElement;
            this.explicitReceiver = explicitReceiver;
            this.callOperationNode = callOperationNode;
            this.calleeExpression = calleeExpression;
            this.valueArguments = valueArguments;
            this.callType = callType;
        }

        @Override
        public ASTNode getCallOperationNode() {
            return callOperationNode;
        }

        @NotNull
        @Override
        public ReceiverValue getExplicitReceiver() {
            return explicitReceiver;
        }

        @NotNull
        @Override
        public ReceiverValue getThisObject() {
            return ReceiverValue.NO_RECEIVER;
        }

        @Override
        public JetExpression getCalleeExpression() {
            return calleeExpression;
        }

        @NotNull
        @Override
        public List<? extends ValueArgument> getValueArguments() {
            return valueArguments;
        }

        @NotNull
        @Override
        public PsiElement getCallElement() {
            return callElement;
        }

        @Override
        public JetValueArgumentList getValueArgumentList() {
            return null;
        }

        @NotNull
        @Override
        public List<JetExpression> getFunctionLiteralArguments() {
            return Collections.emptyList();
        }
        @NotNull
        @Override
        public List<JetTypeProjection> getTypeArguments() {
            return Collections.emptyList();
        }

        @Override
        public JetTypeArgumentList getTypeArgumentList() {
            return null;
        }

        @Override
        public String toString() {
            return getCallElement().getText();
        }

        @NotNull
        @Override
        public CallType getCallType() {
            return callType;
        }
    }

    public static Call makeCallWithExpressions(@NotNull JetElement callElement, @NotNull ReceiverValue explicitReceiver,
                                               @Nullable ASTNode callOperationNode, @NotNull JetExpression calleeExpression,
                                               @NotNull List<JetExpression> argumentExpressions) {
        return makeCallWithExpressions(callElement, explicitReceiver, callOperationNode, calleeExpression, argumentExpressions, CallType.DEFAULT);
    }

    public static Call makeCallWithExpressions(@NotNull JetElement callElement, @NotNull ReceiverValue explicitReceiver,
                                               @Nullable ASTNode callOperationNode, @NotNull JetExpression calleeExpression,
                                               @NotNull List<JetExpression> argumentExpressions, @NotNull CallType callType) {
        List<ValueArgument> arguments = Lists.newArrayList();
        for (JetExpression argumentExpression : argumentExpressions) {
            arguments.add(makeValueArgument(argumentExpression, calleeExpression));
        }
        return makeCall(callElement, explicitReceiver, callOperationNode, calleeExpression, arguments, callType);
    }

    public static Call makeCall(JetElement callElement, ReceiverValue explicitReceiver, @Nullable ASTNode callOperationNode, JetExpression calleeExpression, List<? extends ValueArgument> arguments) {
        return makeCall(callElement, explicitReceiver, callOperationNode, calleeExpression, arguments, CallType.DEFAULT);
    }

    public static Call makeCall(JetElement callElement, ReceiverValue explicitReceiver, @Nullable ASTNode callOperationNode,
            JetExpression calleeExpression, List<? extends ValueArgument> arguments, CallType callType) {
        return new CallImpl(callElement, explicitReceiver, callOperationNode, calleeExpression, arguments, callType);
    }

    public static Call makeCall(@NotNull ReceiverValue leftAsReceiver, JetBinaryExpression expression) {
        return makeCallWithExpressions(expression, leftAsReceiver, null, expression.getOperationReference(), Collections.singletonList(expression.getRight()));
    }

    public static Call makeCall(@NotNull ReceiverValue baseAsReceiver, JetUnaryExpression expression) {
        return makeCall(expression, baseAsReceiver, null, expression.getOperationReference(), Collections.<ValueArgument>emptyList());
    }

    public static Call makeArraySetCall(@NotNull ReceiverValue arrayAsReceiver, @NotNull JetArrayAccessExpression arrayAccessExpression,
            @NotNull JetExpression rightHandSide, @NotNull CallType callType) {
        List<JetExpression> arguments = Lists.newArrayList(arrayAccessExpression.getIndexExpressions());
        arguments.add(rightHandSide);
        return makeCallWithExpressions(arrayAccessExpression, arrayAsReceiver, null, arrayAccessExpression, arguments, callType);
    }

    public static Call makeArrayGetCall(@NotNull ReceiverValue arrayAsReceiver, @NotNull JetArrayAccessExpression arrayAccessExpression,
            @NotNull CallType callType) {
        return makeCallWithExpressions(arrayAccessExpression, arrayAsReceiver, null, arrayAccessExpression, arrayAccessExpression.getIndexExpressions(), callType);
    }

    public static ValueArgument makeValueArgument(@NotNull JetExpression expression) {
        return makeValueArgument(expression, expression);
    }

    public static ValueArgument makeValueArgument(@Nullable JetExpression expression, @NotNull PsiElement reportErrorsOn) {
        return new ExpressionValueArgument(expression, reportErrorsOn);
    }

    public static Call makePropertyCall(@NotNull ReceiverValue explicitReceiver, @Nullable ASTNode callOperationNode, @NotNull JetSimpleNameExpression nameExpression) {
        return makeCallWithExpressions(nameExpression, explicitReceiver, callOperationNode, nameExpression, Collections.<JetExpression>emptyList());
    }

    public static Call makeCall(@NotNull final ReceiverValue explicitReceiver, @Nullable final ASTNode callOperationNode, @NotNull final JetCallElement callElement) {
        return new Call() {
            @Override
            public ASTNode getCallOperationNode() {
                return callOperationNode;
            }

            @NotNull
            @Override
            public ReceiverValue getExplicitReceiver() {
                return explicitReceiver;
            }

            @NotNull
            @Override
            public ReceiverValue getThisObject() {
                return ReceiverValue.NO_RECEIVER;
            }

            @Override
            @Nullable
            public JetExpression getCalleeExpression() {
                return callElement.getCalleeExpression();
            }

            @Override
            @Nullable
            public JetValueArgumentList getValueArgumentList() {
                return callElement.getValueArgumentList();
            }

            @Override
            @NotNull
            public List<? extends ValueArgument> getValueArguments() {
                return callElement.getValueArguments();
            }

            @Override
            @NotNull
            public List<JetExpression> getFunctionLiteralArguments() {
                return callElement.getFunctionLiteralArguments();
            }

            @Override
            @NotNull
            public List<JetTypeProjection> getTypeArguments() {
                return callElement.getTypeArguments();
            }

            @Override
            @Nullable
            public JetTypeArgumentList getTypeArgumentList() {
                return callElement.getTypeArgumentList();
            }

            @NotNull
            @Override
            public PsiElement getCallElement() {
                return callElement;
            }

            @Override
            public String toString() {
                return callElement.getText();
            }

            @NotNull
            @Override
            public CallType getCallType() {
                return CallType.DEFAULT;
            }
        };
    }

    public static Call makeCall(@NotNull JetElement callElement, @NotNull ReceiverValue explicitReceiver) {
        return new CallImpl(callElement, explicitReceiver, null, null, Collections.<ValueArgument>emptyList());
    }
}
