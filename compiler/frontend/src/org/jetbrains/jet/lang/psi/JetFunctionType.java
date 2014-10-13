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

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.stubs.PsiJetPlaceHolderStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JetFunctionType extends JetElementImplStub<PsiJetPlaceHolderStub<JetFunctionType>> implements JetTypeElement {

    public static final JetToken RETURN_TYPE_SEPARATOR = JetTokens.ARROW;

    public JetFunctionType(@NotNull ASTNode node) {
        super(node);
    }

    public JetFunctionType(@NotNull PsiJetPlaceHolderStub<JetFunctionType> stub) {
        super(stub, JetStubElementTypes.FUNCTION_TYPE);
    }

    @NotNull
    @Override
    public List<JetTypeReference> getTypeArgumentsAsTypes() {
        ArrayList<JetTypeReference> result = Lists.newArrayList();
        JetTypeReference receiverTypeRef = getReceiverTypeReference();
        if (receiverTypeRef != null) {
            result.add(receiverTypeRef);
        }
        for (JetParameter jetParameter : getParameters()) {
            result.add(jetParameter.getTypeReference());
        }
        JetTypeReference returnTypeRef = getReturnTypeReference();
        if (returnTypeRef != null) {
            result.add(returnTypeRef);
        }
        return result;
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitFunctionType(this, data);
    }

    @Nullable
    public JetParameterList getParameterList() {
        return getStubOrPsiChild(JetStubElementTypes.VALUE_PARAMETER_LIST);
    }

    @NotNull
    public List<JetParameter> getParameters() {
        JetParameterList list = getParameterList();
        return list != null ? list.getParameters() : Collections.<JetParameter>emptyList();
    }

    @Nullable
    public JetTypeReference getReceiverTypeReference() {
        JetFunctionTypeReceiver receiverDeclaration = getStubOrPsiChild(JetStubElementTypes.FUNCTION_TYPE_RECEIVER);
        if (receiverDeclaration == null) {
            return null;
        }
        return receiverDeclaration.getTypeReference();
    }

    @Nullable
    public JetTypeReference getReturnTypeReference() {
        return getStubOrPsiChild(JetStubElementTypes.TYPE_REFERENCE);
    }
}
