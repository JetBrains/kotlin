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

import com.intellij.lang.ASTNode;
import kotlin.jvm.KotlinSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;

import java.util.List;

public interface Call {

    // SAFE_ACCESS or DOT or so
    @Nullable
    ASTNode getCallOperationNode();

    @NotNull
    ReceiverValue getExplicitReceiver();

    @NotNull
    ReceiverValue getThisObject();

    @Nullable
    JetExpression getCalleeExpression();

    @Nullable
    JetValueArgumentList getValueArgumentList();

    @KotlinSignature("fun getValueArguments(): List<out ValueArgument?>")
    @NotNull
    List<? extends ValueArgument> getValueArguments();

    @KotlinSignature("fun getFunctionLiteralArguments(): List<JetExpression>")
    @NotNull
    List<JetExpression> getFunctionLiteralArguments();

    @KotlinSignature("fun getTypeArguments(): List<JetTypeProjection>")
    @NotNull
    List<JetTypeProjection> getTypeArguments();

    @Nullable
    JetTypeArgumentList getTypeArgumentList();

    @NotNull
    JetElement getCallElement();

    enum CallType {
        DEFAULT, ARRAY_GET_METHOD, ARRAY_SET_METHOD, INVOKE
    }

    @NotNull
    Call.CallType getCallType();
}
