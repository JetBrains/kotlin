/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.util;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.Call.CallType;
import org.jetbrains.kotlin.psi.debugText.DebugTextUtilKt;
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CallMaker {

    private static class ExpressionValueArgument implements ValueArgument {

        private final KtExpression expression;

        private final KtElement reportErrorsOn;

        private final boolean isExternal;

        private ExpressionValueArgument(
                @Nullable KtExpression expression,
                @NotNull KtElement reportErrorsOn,
                boolean isExternal
        ) {
            this.expression = expression;
            this.reportErrorsOn = expression == null ? reportErrorsOn : expression;
            this.isExternal = isExternal;
        }

        @Override
        public boolean isExternal() {
            return isExternal;
        }

        @Override
        public KtExpression getArgumentExpression() {
            return expression;
        }

        @Override
        public ValueArgumentName getArgumentName() {
            return null;
        }

        @Override
        public boolean isNamed() {
            return false;
        }

        @NotNull
        @Override
        public KtElement asElement() {
            return reportErrorsOn;
        }

        @Override
        public LeafPsiElement getSpreadElement() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ExpressionValueArgument argument = (ExpressionValueArgument) o;

            if (expression != null ? !expression.equals(argument.expression) : argument.expression != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return expression != null ? expression.hashCode() : 0;
        }
    }

    private static class CallImpl implements Call {

        private final KtElement callElement;
        private final Receiver explicitReceiver;
        private final ASTNode callOperationNode;
        private final KtExpression calleeExpression;
        private final List<? extends ValueArgument> valueArguments;
        private final Call.CallType callType;
        private final boolean isSemanticallyEquivalentToSafeCall;

        protected CallImpl(
                @NotNull KtElement callElement,
                @NotNull Receiver explicitReceiver,
                @Nullable ASTNode callOperationNode,
                @Nullable KtExpression calleeExpression,
                @NotNull List<? extends ValueArgument> valueArguments
        ) {
            this(callElement, explicitReceiver, callOperationNode, calleeExpression, valueArguments, CallType.DEFAULT, false);
        }

        protected CallImpl(
                @NotNull KtElement callElement,
                @Nullable Receiver explicitReceiver,
                @Nullable ASTNode callOperationNode,
                @Nullable KtExpression calleeExpression,
                @NotNull List<? extends ValueArgument> valueArguments,
                @NotNull CallType callType,
                boolean isSemanticallyEquivalentToSafeCall
        ) {
            this.callElement = callElement;
            this.explicitReceiver = explicitReceiver;
            this.callOperationNode = callOperationNode;
            this.calleeExpression = calleeExpression;
            this.valueArguments = valueArguments;
            this.callType = callType;
            this.isSemanticallyEquivalentToSafeCall = isSemanticallyEquivalentToSafeCall;
        }

        @Override
        public ASTNode getCallOperationNode() {
            return callOperationNode;
        }

        @Override
        public boolean isSemanticallyEquivalentToSafeCall() {
            return isSemanticallyEquivalentToSafeCall || Call.super.isSemanticallyEquivalentToSafeCall();
        }

        @Nullable
        @Override
        public Receiver getExplicitReceiver() {
            return explicitReceiver;
        }

        @Nullable
        @Override
        public ReceiverValue getDispatchReceiver() {
            return null;
        }

        @Override
        public KtExpression getCalleeExpression() {
            return calleeExpression;
        }

        @NotNull
        @Override
        public List<? extends ValueArgument> getValueArguments() {
            return valueArguments;
        }

        @NotNull
        @Override
        public KtElement getCallElement() {
            return callElement;
        }

        @Override
        public KtValueArgumentList getValueArgumentList() {
            return null;
        }

        @NotNull
        @Override
        public List<LambdaArgument> getFunctionLiteralArguments() {
            return Collections.emptyList();
        }
        @NotNull
        @Override
        public List<KtTypeProjection> getTypeArguments() {
            return Collections.emptyList();
        }

        @Override
        public KtTypeArgumentList getTypeArgumentList() {
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

    @NotNull
    public static Call makeCallWithExpressions(@NotNull KtElement callElement, @Nullable Receiver explicitReceiver,
                                               @Nullable ASTNode callOperationNode, @NotNull KtExpression calleeExpression,
                                               @NotNull List<KtExpression> argumentExpressions) {
        return makeCallWithExpressions(callElement, explicitReceiver, callOperationNode, calleeExpression, argumentExpressions, CallType.DEFAULT,
                                       false);
    }

    @NotNull
    public static Call makeCallWithExpressions(@NotNull KtElement callElement, @Nullable Receiver explicitReceiver,
            @Nullable ASTNode callOperationNode, @NotNull KtExpression calleeExpression,
            @NotNull List<KtExpression> argumentExpressions, @NotNull CallType callType) {
        return makeCallWithExpressions(callElement, explicitReceiver, callOperationNode, calleeExpression, argumentExpressions, callType,
                                       false);
    }


    @NotNull
    public static Call makeCallWithExpressions(
            @NotNull KtElement callElement, @Nullable Receiver explicitReceiver,
            @Nullable ASTNode callOperationNode, @NotNull KtExpression calleeExpression,
            @NotNull List<KtExpression> argumentExpressions, @NotNull CallType callType,
            boolean isSemanticallyEquivalentToSafeCall
    ) {
        List<ValueArgument> arguments;
        if (argumentExpressions.isEmpty()) {
            arguments = Collections.emptyList();
        }
        else {
            arguments = new ArrayList<>(argumentExpressions.size());
            for (KtExpression argumentExpression : argumentExpressions) {
                arguments.add(makeValueArgument(argumentExpression, calleeExpression));
            }
        }
        return makeCall(
                callElement, explicitReceiver, callOperationNode, calleeExpression, arguments, callType, isSemanticallyEquivalentToSafeCall
        );
    }

    @NotNull
    public static Call makeCall(KtElement callElement, @Nullable Receiver explicitReceiver, @Nullable ASTNode callOperationNode, KtExpression calleeExpression, List<? extends ValueArgument> arguments) {
        return makeCall(callElement, explicitReceiver, callOperationNode, calleeExpression, arguments, CallType.DEFAULT);
    }

    @NotNull
    public static Call makeCall(
            KtElement callElement, @Nullable Receiver explicitReceiver, @Nullable ASTNode callOperationNode,
            KtExpression calleeExpression, List<? extends ValueArgument> arguments, CallType callType
    ) {
        return makeCall(callElement, explicitReceiver, callOperationNode, calleeExpression, arguments, callType, false);
    }

    @NotNull
    public static Call makeCall(
            KtElement callElement,
            @Nullable Receiver explicitReceiver,
            @Nullable ASTNode callOperationNode,
            KtExpression calleeExpression,
            List<? extends ValueArgument> arguments,
            CallType callType,
            boolean isSemanticallyEquivalentToSafeCall
    ) {
        return new CallImpl(callElement, explicitReceiver, callOperationNode, calleeExpression, arguments, callType, isSemanticallyEquivalentToSafeCall);
    }

    @NotNull
    public static Call makeCall(@NotNull ReceiverValue leftAsReceiver, KtBinaryExpression expression) {
        return makeCallWithExpressions(expression, leftAsReceiver, null, expression.getOperationReference(), Collections.singletonList(expression.getRight()));
    }

    @NotNull
    public static Call makeCall(@NotNull ReceiverValue baseAsReceiver, KtUnaryExpression expression) {
        return makeCall(expression, baseAsReceiver, null, expression.getOperationReference(), Collections.emptyList());
    }

    @NotNull
    public static Call makeArraySetCall(@NotNull ReceiverValue arrayAsReceiver, @NotNull KtArrayAccessExpression arrayAccessExpression,
            @NotNull KtExpression rightHandSide, @NotNull CallType callType) {
        List<KtExpression> arguments = Lists.newArrayList(arrayAccessExpression.getIndexExpressions());
        arguments.add(rightHandSide);
        return makeCallWithExpressions(arrayAccessExpression, arrayAsReceiver, null, arrayAccessExpression, arguments, callType);
    }

    @NotNull
    public static Call makeArrayGetCall(@NotNull ReceiverValue arrayAsReceiver, @NotNull KtArrayAccessExpression arrayAccessExpression,
            @NotNull CallType callType) {
        return makeCallWithExpressions(arrayAccessExpression, arrayAsReceiver, null, arrayAccessExpression, arrayAccessExpression.getIndexExpressions(), callType);
    }

    public static Call makeCallForCollectionLiteral(@NotNull KtCollectionLiteralExpression collectionLiteralExpression) {
        return makeCallWithExpressions(
                collectionLiteralExpression,
                null,
                null,
                collectionLiteralExpression,
                collectionLiteralExpression.getInnerExpressions(),
                CallType.DEFAULT);
    }

    @NotNull
    public static ValueArgument makeValueArgument(@NotNull KtExpression expression) {
        return makeValueArgument(expression, expression);
    }

    @NotNull
    public static ValueArgument makeValueArgument(@Nullable KtExpression expression, @NotNull KtElement reportErrorsOn) {
        return new ExpressionValueArgument(expression, reportErrorsOn, false);
    }

    @NotNull
    public static ValueArgument makeExternalValueArgument(@NotNull KtExpression expression) {
        return new ExpressionValueArgument(expression, expression, true);
    }

    @NotNull
    public static ValueArgument makeExternalValueArgument(@NotNull KtExpression expression, @NotNull KtElement reportErrorsOn) {
        return new ExpressionValueArgument(expression, reportErrorsOn, true);
    }


    @NotNull
    public static Call makePropertyCall(@Nullable Receiver explicitReceiver, @Nullable ASTNode callOperationNode, @NotNull KtSimpleNameExpression nameExpression) {
        return makeCallWithExpressions(nameExpression, explicitReceiver, callOperationNode, nameExpression, Collections.emptyList());
    }


    @NotNull
    public static Call makeConstructorCallWithoutTypeArguments(@NotNull KtCallElement callElement) {
        return new DelegatingCall(makeCall(null, null, callElement)) {
            @NotNull
            @Override
            public List<KtTypeProjection> getTypeArguments() {
                return Collections.emptyList();
            }

            @Nullable
            @Override
            public KtTypeArgumentList getTypeArgumentList() {
                return null;
            }
        };
    }

    @NotNull
    public static Call makeConstructorCallForEnumEntryWithoutInitializer(@NotNull KtSuperTypeCallEntry callElement) {
        return new Call() {
            @Nullable
            @Override
            public ASTNode getCallOperationNode() {
                return null;
            }

            @Nullable
            @Override
            public Receiver getExplicitReceiver() {
                return null;
            }

            @Nullable
            @Override
            public ReceiverValue getDispatchReceiver() {
                return null;
            }

            @Nullable
            @Override
            public KtExpression getCalleeExpression() {
                return callElement.getCalleeExpression();
            }

            @Nullable
            @Override
            public KtValueArgumentList getValueArgumentList() {
                return callElement.getValueArgumentList();
            }

            @NotNull
            @Override
            public List<? extends ValueArgument> getValueArguments() {
                return callElement.getValueArguments();
            }

            @NotNull
            @Override
            public List<? extends LambdaArgument> getFunctionLiteralArguments() {
                return Collections.emptyList();
            }

            @NotNull
            @Override
            public List<KtTypeProjection> getTypeArguments() {
                return Collections.emptyList();
            }

            @Nullable
            @Override
            public KtTypeArgumentList getTypeArgumentList() {
                return null;
            }

            @NotNull
            @Override
            public KtElement getCallElement() {
                return callElement;
            }

            @NotNull
            @Override
            public CallType getCallType() {
                return CallType.DEFAULT;
            }

            @Override
            public String toString() {
                return DebugTextUtilKt.getDebugText(callElement);
            }
        };
    }

    @NotNull
    public static Call makeCall(@Nullable Receiver explicitReceiver, @Nullable ASTNode callOperationNode, @NotNull KtCallElement callElement) {
        return new Call() {
            @Override
            public ASTNode getCallOperationNode() {
                return callOperationNode;
            }

            @Nullable
            @Override
            public Receiver getExplicitReceiver() {
                return explicitReceiver;
            }

            @Nullable
            @Override
            public ReceiverValue getDispatchReceiver() {
                return null;
            }

            @Override
            @Nullable
            public KtExpression getCalleeExpression() {
                return callElement.getCalleeExpression();
            }

            @Override
            @Nullable
            public KtValueArgumentList getValueArgumentList() {
                return callElement.getValueArgumentList();
            }

            @Override
            @NotNull
            public List<? extends ValueArgument> getValueArguments() {
                return callElement.getValueArguments();
            }

            @Override
            @NotNull
            public List<? extends LambdaArgument> getFunctionLiteralArguments() {
                return callElement.getLambdaArguments();
            }

            @Override
            @NotNull
            public List<KtTypeProjection> getTypeArguments() {
                return callElement.getTypeArguments();
            }

            @Override
            @Nullable
            public KtTypeArgumentList getTypeArgumentList() {
                return callElement.getTypeArgumentList();
            }

            @NotNull
            @Override
            public KtElement getCallElement() {
                return callElement;
            }

            @Override
            public String toString() {
                return DebugTextUtilKt.getDebugText(callElement);
            }

            @NotNull
            @Override
            public CallType getCallType() {
                return CallType.DEFAULT;
            }
        };
    }

    @NotNull
    public static Call makeCall(@NotNull KtElement callElement, @NotNull ReceiverValue explicitReceiver) {
        return new CallImpl(callElement, explicitReceiver, null, null, Collections.emptyList());
    }
}
