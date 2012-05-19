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

package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.List;

/**
 * @author abreslav
 */
public interface Call {

    // SAFE_ACCESS or DOT or so
    @Nullable
    ASTNode getCallOperationNode();

    @NotNull
    ReceiverDescriptor getExplicitReceiver();

    @NotNull
    ReceiverDescriptor getThisObject();

    @Nullable
    JetExpression getCalleeExpression();

    @Nullable
    JetValueArgumentList getValueArgumentList();

    @NotNull
    List<? extends ValueArgument> getValueArguments();

    @NotNull
    List<JetExpression> getFunctionLiteralArguments();

    @NotNull
    List<JetTypeProjection> getTypeArguments();

    @Nullable
    JetTypeArgumentList getTypeArgumentList();

    @NotNull
    PsiElement getCallElement();
}
