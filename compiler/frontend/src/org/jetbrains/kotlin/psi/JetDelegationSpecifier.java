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
import com.intellij.util.ArrayFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;

public class JetDelegationSpecifier extends JetElementImplStub<KotlinPlaceHolderStub<? extends JetDelegationSpecifier>> {

    private static final JetDelegationSpecifier[] EMPTY_ARRAY = new JetDelegationSpecifier[0];

    public static ArrayFactory<JetDelegationSpecifier> ARRAY_FACTORY = new ArrayFactory<JetDelegationSpecifier>() {
        @NotNull
        @Override
        public JetDelegationSpecifier[] create(int count) {
            return count == 0 ? EMPTY_ARRAY : new JetDelegationSpecifier[count];
        }
    };

    public JetDelegationSpecifier(@NotNull ASTNode node) {
        super(node);
    }

    public JetDelegationSpecifier(
            @NotNull KotlinPlaceHolderStub<? extends JetDelegationSpecifier> stub,
            @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitDelegationSpecifier(this, data);
    }

    @Nullable
    public JetTypeReference getTypeReference() {
        return getStubOrPsiChild(JetStubElementTypes.TYPE_REFERENCE);
    }

    @Nullable
    public JetUserType getTypeAsUserType() {
        JetTypeReference reference = getTypeReference();
        if (reference != null) {
            JetTypeElement element = reference.getTypeElement();
            if (element instanceof JetUserType) {
                return ((JetUserType) element);
            }
        }
        return null;
    }
}
