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
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.typeRefHelpers.TypeRefHelpersPackage;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

abstract public class JetFunctionNotStubbed extends JetTypeParameterListOwnerNotStubbed
        implements JetFunction {

    public JetFunctionNotStubbed(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public JetParameterList getValueParameterList() {
        return (JetParameterList) findChildByType(JetNodeTypes.VALUE_PARAMETER_LIST);
    }

    @Override
    @NotNull
    public List<JetParameter> getValueParameters() {
        JetParameterList list = getValueParameterList();
        return list != null ? list.getParameters() : Collections.<JetParameter>emptyList();
    }

    @Override
    @Nullable
    public JetExpression getBodyExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    public boolean hasDeclaredReturnType() {
        return getTypeReference() != null;
    }

    @Override
    @Nullable
    public JetTypeReference getReceiverTypeReference() {
        PsiElement child = getFirstChild();
        while (child != null) {
            IElementType tt = child.getNode().getElementType();
            if (tt == JetTokens.LPAR || tt == JetTokens.COLON) break;
            if (child instanceof JetTypeReference) {
                return (JetTypeReference) child;
            }
            child = child.getNextSibling();
        }

        return null;
    }

    @Override
    @Nullable
    public JetTypeReference getTypeReference() {
        return TypeRefHelpersPackage.getTypeReference(this);
    }

    @Nullable
    @Override
    public JetTypeReference setTypeReference(@Nullable JetTypeReference typeRef) {
        return TypeRefHelpersPackage.setTypeReference(this, getValueParameterList(), typeRef);
    }

    @Override
    public boolean isLocal() {
        PsiElement parent = getParent();
        return !(parent instanceof JetFile || parent instanceof JetClassBody);
    }
}
