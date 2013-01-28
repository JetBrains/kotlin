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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.stubs.PsiJetTypeParameterListStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;

import java.util.Arrays;
import java.util.List;

public class JetTypeParameterList extends JetElementImplStub<PsiJetTypeParameterListStub> {
    public JetTypeParameterList(@NotNull ASTNode node) {
        super(node);
    }

    public JetTypeParameterList(@NotNull PsiJetTypeParameterListStub stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public List<JetTypeParameter> getParameters() {
        return Arrays.asList(getStubOrPsiChildren(JetStubElementTypes.TYPE_PARAMETER, JetTypeParameter.ARRAY_FACTORY));
    }

    @NotNull
    @Override
    public IStubElementType getElementType() {
        return JetStubElementTypes.TYPE_PARAMETER_LIST;
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitTypeParameterList(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameterList(this, data);
    }
}
