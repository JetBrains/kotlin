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

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;

import java.util.Collections;
import java.util.List;

public class JetConstructorDelegationCall extends JetElementImplStub<KotlinPlaceHolderStub<? extends JetConstructorDelegationCall>> implements JetCallElement {
    public JetConstructorDelegationCall(
            @NotNull KotlinPlaceHolderStub<? extends JetConstructorDelegationCall> stub
    ) {
        super(stub, JetStubElementTypes.CONSTRUCTOR_DELEGATION_CALL);
    }

    public JetConstructorDelegationCall(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public JetValueArgumentList getValueArgumentList() {
        return (JetValueArgumentList) findChildByType(JetNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @Override
    @NotNull
    public List<? extends ValueArgument> getValueArguments() {
        JetValueArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<JetValueArgument>emptyList();
    }

    @NotNull
    @Override
    public List<JetFunctionLiteralArgument> getFunctionLiteralArguments() {
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

    @Nullable
    @Override
    public JetConstructorDelegationReferenceExpression getCalleeExpression() {
        return findChildByClass(JetConstructorDelegationReferenceExpression.class);
    }
}
