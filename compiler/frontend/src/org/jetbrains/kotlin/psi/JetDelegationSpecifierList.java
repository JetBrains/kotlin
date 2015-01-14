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
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;

import java.util.Arrays;
import java.util.List;

public class JetDelegationSpecifierList extends JetElementImplStub<KotlinPlaceHolderStub<JetDelegationSpecifierList>> {
    public JetDelegationSpecifierList(@NotNull ASTNode node) {
        super(node);
    }

    public JetDelegationSpecifierList(@NotNull KotlinPlaceHolderStub<JetDelegationSpecifierList> stub) {
        super(stub, JetStubElementTypes.DELEGATION_SPECIFIER_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitDelegationSpecifierList(this, data);
    }

    public List<JetDelegationSpecifier> getDelegationSpecifiers() {
        return Arrays.asList(getStubOrPsiChildren(JetStubElementTypes.DELEGATION_SPECIFIER_TYPES, JetDelegationSpecifier.ARRAY_FACTORY));
    }
}
