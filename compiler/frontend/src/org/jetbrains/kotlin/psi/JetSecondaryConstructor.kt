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
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;

public class JetSecondaryConstructor extends JetConstructor<JetSecondaryConstructor> {
    public JetSecondaryConstructor(@NotNull ASTNode node) {
        super(node);
    }

    public JetSecondaryConstructor(@NotNull KotlinPlaceHolderStub<JetSecondaryConstructor> stub) {
        super(stub, JetStubElementTypes.SECONDARY_CONSTRUCTOR);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitSecondaryConstructor(this, data);
    }

    @Override
    @NotNull
    public JetClassOrObject getClassOrObject() {
        return (JetClassOrObject) getParent().getParent();
    }

    @Nullable
    @Override
    public JetBlockExpression getBodyExpression() {
        return findChildByClass(JetBlockExpression.class);
    }

    @Override
    @NotNull
    public PsiElement getConstructorKeyword() {
        //noinspection ConstantConditions
        return notNullChild(super.getConstructorKeyword());
    }

    @NotNull
    public JetConstructorDelegationCall getDelegationCall() {
        return findNotNullChildByClass(JetConstructorDelegationCall.class);
    }

    public boolean hasImplicitDelegationCall() {
        return getDelegationCall().isImplicit();
    }

    @NotNull
    public JetConstructorDelegationCall replaceImplicitDelegationCallWithExplicit(boolean isThis) {
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        JetConstructorDelegationCall current = getDelegationCall();

        assert current.isImplicit()
                : "Method should not be called with explicit delegation call: " + getText();
        current.delete();

        PsiElement colon = addAfter(psiFactory.createColon(), getValueParameterList());

        String delegationName = isThis ? "this" : "super";

        return (JetConstructorDelegationCall) addAfter(psiFactory.createConstructorDelegationCall(delegationName + "()"), colon);
    }
}
