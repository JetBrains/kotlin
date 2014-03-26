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
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.stubs.PsiJetUserTypeStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

public class JetUserType extends JetElementImplStub<PsiJetUserTypeStub> implements JetTypeElement {
    public JetUserType(@NotNull ASTNode node) {
        super(node);
    }

    public JetUserType(@NotNull PsiJetUserTypeStub stub) {
        super(stub, JetStubElementTypes.USER_TYPE);
    }

    public boolean isAbsoluteInRootPackage() {
        return findChildByType(JetTokens.PACKAGE_KEYWORD) != null;
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitUserType(this, data);
    }

    public JetTypeArgumentList getTypeArgumentList() {
        return (JetTypeArgumentList) findChildByType(JetNodeTypes.TYPE_ARGUMENT_LIST);
    }

    @NotNull
    public List<JetTypeProjection> getTypeArguments() {
        // TODO: empty elements in PSI
        JetTypeArgumentList typeArgumentList = getTypeArgumentList();
        return typeArgumentList == null ? Collections.<JetTypeProjection>emptyList() : typeArgumentList.getArguments();
    }

    @NotNull
    @Override
    public List<JetTypeReference> getTypeArgumentsAsTypes() {
        List<JetTypeReference> result = Lists.newArrayList();
        for (JetTypeProjection projection : getTypeArguments()) {
            result.add(projection.getTypeReference());
        }
        return result;
    }

    @Nullable @IfNotParsed
    public JetNameReferenceExpression getReferenceExpression() {
        return (JetNameReferenceExpression) findChildByType(JetNodeTypes.REFERENCE_EXPRESSION);
    }

    @Nullable
    public JetUserType getQualifier() {
        return (JetUserType) findChildByType(JetNodeTypes.USER_TYPE);
    }

    @Nullable
    public String getReferencedName() {
        JetNameReferenceExpression referenceExpression = getReferenceExpression();
        return referenceExpression == null ? null : referenceExpression.getReferencedName();
    }
}
