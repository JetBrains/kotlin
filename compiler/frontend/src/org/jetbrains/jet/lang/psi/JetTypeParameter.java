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
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.ArrayFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.stubs.PsiJetTypeParameterStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lexer.JetTokens;

public class JetTypeParameter extends JetNamedDeclarationStub<PsiJetTypeParameterStub> {
    public static final JetTypeParameter[] EMPTY_ARRAY = new JetTypeParameter[0];

    public static final ArrayFactory<JetTypeParameter> ARRAY_FACTORY = new ArrayFactory<JetTypeParameter>() {
        @Override
        public JetTypeParameter[] create(int count) {
            return count == 0 ? EMPTY_ARRAY : new JetTypeParameter[count];
        }
    };

    public JetTypeParameter(@NotNull ASTNode node) {
        super(node);
    }

    public JetTypeParameter(@NotNull PsiJetTypeParameterStub stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitTypeParameter(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameter(this, data);
    }

    @NotNull
    public Variance getVariance() {
        PsiJetTypeParameterStub stub = getStub();
        if (stub != null) {
            if (stub.isOutVariance()) return Variance.OUT_VARIANCE;
            if (stub.isInVariance()) return Variance.IN_VARIANCE;
            return Variance.INVARIANT;
        }

        JetModifierList modifierList = getModifierList();
        if (modifierList == null) return Variance.INVARIANT;

        if (modifierList.hasModifier(JetTokens.OUT_KEYWORD)) return Variance.OUT_VARIANCE;
        if (modifierList.hasModifier(JetTokens.IN_KEYWORD)) return Variance.IN_VARIANCE;
        return Variance.INVARIANT;
    }

    @Nullable
    public JetTypeReference getExtendsBound() {
        return (JetTypeReference) findChildByType(JetNodeTypes.TYPE_REFERENCE);
    }

    @NotNull
    @Override
    public IStubElementType getElementType() {
        return JetStubElementTypes.TYPE_PARAMETER;
    }
}
