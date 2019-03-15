/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.lang.ASTNode;
import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;

import java.util.List;

public class DelegatingCall implements Call {

    private final Call delegate;

    public DelegatingCall(@NotNull Call delegate) {
        this.delegate = delegate;
    }

    @Override
    @Nullable
    public ASTNode getCallOperationNode() {
        return delegate.getCallOperationNode();
    }

    @Override
    @Nullable
    public Receiver getExplicitReceiver() {
        return delegate.getExplicitReceiver();
    }

    @Nullable
    @Override
    public ReceiverValue getDispatchReceiver() {
        return delegate.getDispatchReceiver();
    }

    @Override
    @Nullable
    public KtExpression getCalleeExpression() {
        return delegate.getCalleeExpression();
    }

    @Override
    @Nullable
    public KtValueArgumentList getValueArgumentList() {
        return delegate.getValueArgumentList();
    }

    @Override
    @NotNull
    @ReadOnly
    public List<? extends ValueArgument> getValueArguments() {
        return delegate.getValueArguments();
    }

    @Override
    @NotNull
    public List<? extends LambdaArgument> getFunctionLiteralArguments() {
        return delegate.getFunctionLiteralArguments();
    }

    @Override
    @NotNull
    public List<KtTypeProjection> getTypeArguments() {
        return delegate.getTypeArguments();
    }

    @Override
    @Nullable
    public KtTypeArgumentList getTypeArgumentList() {
        return delegate.getTypeArgumentList();
    }

    @NotNull
    @Override
    public KtElement getCallElement() {
        return delegate.getCallElement();
    }

    @NotNull
    @Override
    public CallType getCallType() {
        return delegate.getCallType();
    }

    @Override
    public String toString() {
        return "*" + delegate.toString();
    }
}
