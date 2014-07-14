/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public abstract class JetExpressionImplStub<T extends StubElement> extends JetElementImplStub<T> implements JetExpression {
    public JetExpressionImplStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public JetExpressionImplStub(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitExpression(this, data);
    }

    //NOTE: duplicate with JetExpressionImpl
    @NotNull
    @Override
    public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
        PsiElement parent = getParent();
        if (parent instanceof JetExpression && newElement instanceof JetExpression &&
            JetPsiUtil.areParenthesesNecessary((JetExpression) newElement, this, (JetExpression) parent)) {
            return super.replace(JetPsiFactory(this).createExpression("(" + newElement.getText() + ")"));
        }
        return super.replace(newElement);
    }
}
