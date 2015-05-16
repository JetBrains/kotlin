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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;

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
    public List<JetParameter> getParameters() {
        return getStubOrPsiChildrenAsList(JetStubElementTypes.VALUE_PARAMETER);
    }

    @NotNull
    public JetParameter addParameter(@NotNull JetParameter parameter) {
        return EditCommaSeparatedListHelper.INSTANCE$.addItem(this, getParameters(), parameter);
    }

    @NotNull
    public JetParameter addParameterAfter(@NotNull JetParameter parameter, @Nullable JetParameter anchor) {
        return EditCommaSeparatedListHelper.INSTANCE$.addItemAfter(this, getParameters(), parameter, anchor);
    }

    @NotNull
    public JetParameter addParameterBefore(@NotNull JetParameter parameter, @Nullable JetParameter anchor) {
        return EditCommaSeparatedListHelper.INSTANCE$.addItemBefore(this, getParameters(), parameter, anchor);
    }

    public void removeParameter(@NotNull JetParameter parameter) {
        EditCommaSeparatedListHelper.INSTANCE$.removeItem(parameter);
    }

    // this method needed only for migrate lambda syntax
    @Deprecated
    public boolean isParenthesized() {
        PsiElement firstChild = getFirstChild();
        if (firstChild != null && firstChild.getNode() != null) {
            return firstChild.getNode().getElementType() == JetTokens.LPAR;
        }
        return false;
    }
}
