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
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.IncorrectOperationException;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

public abstract class KtExpressionImplStub<T extends StubElement<?>> extends KtElementImplStub<T> implements KtExpression {
    public KtExpressionImplStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public KtExpressionImplStub(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitExpression(this, data);
    }

    @NotNull
    @Override
    public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
        return KtExpressionImpl.Companion.replaceExpression(this, newElement, new Function1<PsiElement, PsiElement>() {
            @Override
            public PsiElement invoke(PsiElement element) {
                return rawReplace(element);
            }
        });
    }

    @NotNull
    private PsiElement rawReplace(@NotNull PsiElement newElement) {
        return super.replace(newElement);
    }

    @Override
    public PsiElement getParent() {
        T stub = getStub();
        if (stub != null) {
            //noinspection unchecked
            return stub.getParentStub().getPsi();
        }
        return super.getParent();
    }
}
