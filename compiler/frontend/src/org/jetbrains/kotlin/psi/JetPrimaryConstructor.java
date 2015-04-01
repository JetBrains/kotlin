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
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.addRemoveModifier.AddRemoveModifierPackage;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;

import java.util.Collections;
import java.util.List;

public class JetPrimaryConstructor extends JetDeclarationStub<KotlinPlaceHolderStub<JetPrimaryConstructor>> {
    public JetPrimaryConstructor(@NotNull ASTNode node) {
        super(node);
    }

    public JetPrimaryConstructor(@NotNull KotlinPlaceHolderStub<JetPrimaryConstructor> stub) {
        super(stub, JetStubElementTypes.PRIMARY_CONSTRUCTOR);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitPrimaryConstructor(this, data);
    }

    @Nullable
    public JetParameterList getValueParameterList() {
        return getStubOrPsiChild(JetStubElementTypes.VALUE_PARAMETER_LIST);
    }

    @NotNull
    public List<JetParameter> getValueParameters() {
        JetParameterList list = getValueParameterList();
        return list != null ? list.getParameters() : Collections.<JetParameter>emptyList();
    }

    @Override
    public void addModifier(@NotNull JetModifierKeywordToken modifier) {
        JetModifierList modifierList = getModifierList();
        if (modifierList != null) {
            AddRemoveModifierPackage.addModifier(modifierList, modifier, JetTokens.PUBLIC_KEYWORD);
        }
        else {
            if (modifier == JetTokens.PUBLIC_KEYWORD) return;

            JetParameterList parameterList = getValueParameterList();
            assert parameterList != null;
            JetPsiFactory psiFactory = new JetPsiFactory(getProject());
            JetModifierList newModifierList = psiFactory.createModifierList(modifier);
            addBefore(newModifierList, parameterList);
        }
    }

    @Nullable
    public JetClass getContainingClassOrNull() {
        JetClassOrObject classOrObject = (JetClassOrObject) getParent();
        return classOrObject instanceof JetClass ? (JetClass) classOrObject : null;
    }

    @NotNull
    public JetClass getContainingClass() {
        JetClass classOrNull = getContainingClassOrNull();
        assert classOrNull != null : "This method should be called when parent is JetClass";
        return classOrNull;
    }
}
