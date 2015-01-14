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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.stubs.KotlinTypeParameterStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.kotlin.types.Variance;
import org.jetbrains.kotlin.lexer.JetTokens;

public class JetTypeParameter extends JetNamedDeclarationStub<KotlinTypeParameterStub> {

    public JetTypeParameter(@NotNull ASTNode node) {
        super(node);
    }

    public JetTypeParameter(@NotNull KotlinTypeParameterStub stub) {
        super(stub, JetStubElementTypes.TYPE_PARAMETER);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameter(this, data);
    }

    @NotNull
    public Variance getVariance() {
        KotlinTypeParameterStub stub = getStub();
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
        return getStubOrPsiChild(JetStubElementTypes.TYPE_REFERENCE);
    }
}
