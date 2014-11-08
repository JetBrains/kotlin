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
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.jet.lang.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;

import java.util.List;

public class JetParameterList extends JetElementImplStub<KotlinPlaceHolderStub<JetParameterList>> {
    public JetParameterList(@NotNull ASTNode node) {
        super(node);
    }

    public JetParameterList(@NotNull KotlinPlaceHolderStub<JetParameterList> stub) {
        super(stub, JetStubElementTypes.VALUE_PARAMETER_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitParameterList(this, data);
    }

    @NotNull
    @ReadOnly
    public List<JetParameter> getParameters() {
        return getStubOrPsiChildrenAsList(JetStubElementTypes.VALUE_PARAMETER);
    }
}
