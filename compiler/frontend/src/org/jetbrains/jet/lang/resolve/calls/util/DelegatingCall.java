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

package org.jetbrains.jet.lang.resolve.calls.util;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;

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
    @NotNull
    public ReceiverValue getExplicitReceiver() {
        return delegate.getExplicitReceiver();
    }

    @NotNull
    @Override
    public ReceiverValue getThisObject() {
        return delegate.getThisObject();
    }

    @Override
    @Nullable
    public JetExpression getCalleeExpression() {
        return delegate.getCalleeExpression();
    }

    @Override
    @Nullable
    public JetValueArgumentList getValueArgumentList() {
        return delegate.getValueArgumentList();
    }

    @Override
    @NotNull
    public List<? extends ValueArgument> getValueArguments() {
        return delegate.getValueArguments();
    }

    @Override
    @NotNull
    public List<JetFunctionLiteralArgument> getFunctionLiteralArguments() {
        return delegate.getFunctionLiteralArguments();
    }

    @Override
    @NotNull
    public List<JetTypeProjection> getTypeArguments() {
        return delegate.getTypeArguments();
    }

    @Override
    @Nullable
    public JetTypeArgumentList getTypeArgumentList() {
        return delegate.getTypeArgumentList();
    }

    @NotNull
    @Override
    public JetElement getCallElement() {
        return delegate.getCallElement();
    }

    @NotNull
    @Override
    public CallType getCallType() {
        return delegate.getCallType();
    }
}
