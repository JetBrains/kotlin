/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.util.OperatorNameConventions;

import java.util.Collections;
import java.util.List;

public class CallTransformer {
    private CallTransformer() {}

    public static Call stripCallArguments(@NotNull Call call) {
        return new DelegatingCall(call) {
            @Override
            public KtValueArgumentList getValueArgumentList() {
                return null;
            }

            @NotNull
            @Override
            public List<? extends ValueArgument> getValueArguments() {
                return Collections.emptyList();
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

            @NotNull
            @Override
            public KtElement getCallElement() {
                KtExpression calleeExpression = getCalleeExpression();
                assert calleeExpression != null : "No callee expression: " + getCallElement().getText();

                return calleeExpression;
            }
        };
    }

    public static Call stripReceiver(@NotNull Call variableCall) {
        return new DelegatingCall(variableCall) {
            @Nullable
            @Override
            public ASTNode getCallOperationNode() {
                return null;
            }

            @Nullable
            @Override
            public ReceiverValue getExplicitReceiver() {
                return null;
            }
        };
    }

    public static class CallForImplicitInvoke extends DelegatingCall {
        private final Call outerCall;
        private final Receiver explicitExtensionReceiver;
        private final ExpressionReceiver calleeExpressionAsDispatchReceiver;
        private final KtSimpleNameExpression fakeInvokeExpression;
        public final boolean itIsVariableAsFunctionCall;

        public CallForImplicitInvoke(
                @Nullable Receiver explicitExtensionReceiver,
                @NotNull ExpressionReceiver calleeExpressionAsDispatchReceiver,
                @NotNull Call call,
                boolean functionCall
        ) {
            super(call);
            this.outerCall = call;
            this.explicitExtensionReceiver = explicitExtensionReceiver;
            this.calleeExpressionAsDispatchReceiver = calleeExpressionAsDispatchReceiver;
            this.fakeInvokeExpression =
                    (KtSimpleNameExpression) KtPsiFactoryKt.KtPsiFactory(call.getCallElement())
                            .createExpression(OperatorNameConventions.INVOKE.asString());
            itIsVariableAsFunctionCall = functionCall;
        }

        @Nullable
        @Override
        public ASTNode getCallOperationNode() {
            // if an explicit receiver corresponds to the implicit invoke, there is a corresponding call operation node:
            // a.b() or a?.b() (where b has an extension function type);
            // otherwise it's implicit
            return explicitExtensionReceiver != null ? super.getCallOperationNode() : null;
        }

        @Nullable
        @Override
        public Receiver getExplicitReceiver() {
            return explicitExtensionReceiver;
        }

        @NotNull
        @Override
        public ExpressionReceiver getDispatchReceiver() {
            return calleeExpressionAsDispatchReceiver;
        }

        @Override
        public KtExpression getCalleeExpression() {
            return fakeInvokeExpression;
        }

        @NotNull
        @Override
        public CallType getCallType() {
            return CallType.INVOKE;
        }

        @NotNull
        public Call getOuterCall() {
            return outerCall;
        }
    }
}
