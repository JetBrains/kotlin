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

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;

import java.util.List;

public interface Call {

    // SAFE_ACCESS or DOT or so
    @Nullable
    ASTNode getCallOperationNode();

    default boolean isSemanticallyEquivalentToSafeCall() {
        return getCallOperationNode() != null && getCallOperationNode().getElementType() == KtTokens.SAFE_ACCESS;
    }

    @Nullable
    Receiver getExplicitReceiver();

    @Nullable
    ReceiverValue getDispatchReceiver();

    @Nullable
    KtExpression getCalleeExpression();

    @Nullable
    KtValueArgumentList getValueArgumentList();

    @ReadOnly
    @NotNull
    List<? extends ValueArgument> getValueArguments();

    @ReadOnly
    @NotNull
    List<? extends LambdaArgument> getFunctionLiteralArguments();

    @ReadOnly
    @NotNull
    List<KtTypeProjection> getTypeArguments();

    @Nullable
    KtTypeArgumentList getTypeArgumentList();

    @NotNull
    KtElement getCallElement();

    enum CallType {
        DEFAULT, ARRAY_GET_METHOD, ARRAY_SET_METHOD, INVOKE
    }

    @NotNull
    Call.CallType getCallType();
}
