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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.stubs.PsiJetPlaceHolderStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;

import java.util.Collections;
import java.util.List;

public class JetDelegatorToSuperCall extends JetDelegationSpecifier implements JetCallElement {
    public JetDelegatorToSuperCall(@NotNull ASTNode node) {
        super(node);
    }

    public JetDelegatorToSuperCall(@NotNull PsiJetPlaceHolderStub<? extends JetDelegationSpecifier> stub) {
        super(stub, JetStubElementTypes.DELEGATOR_SUPER_CALL);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitDelegationToSuperCallSpecifier(this, data);
    }

    @NotNull
    @Override
    public JetConstructorCalleeExpression getCalleeExpression() {
        return getRequiredStubOrPsiChild(JetStubElementTypes.CONSTRUCTOR_CALLEE);
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

    @Override
    public JetTypeReference getTypeReference() {
        return getCalleeExpression().getTypeReference();
    }

    @NotNull
    @Override
    public List<JetTypeProjection> getTypeArguments() {
        JetTypeArgumentList typeArgumentList = getTypeArgumentList();
        if (typeArgumentList == null) {
            return Collections.emptyList();
        }
        return typeArgumentList.getArguments();
    }

    @Override
    public JetTypeArgumentList getTypeArgumentList() {
        JetUserType userType = getTypeAsUserType();
        return userType != null ? userType.getTypeArgumentList() : null;
    }

}
