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
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public class KtClassInitializer extends KtDeclarationStub<KotlinPlaceHolderStub<KtClassInitializer>> implements KtStatementExpression {
    public KtClassInitializer(@NotNull ASTNode node) {
        super(node);
    }

    public KtClassInitializer(@NotNull KotlinPlaceHolderStub<KtClassInitializer> stub) {
        super(stub, KtStubElementTypes.ANONYMOUS_INITIALIZER);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitAnonymousInitializer(this, data);
    }

    @Nullable
    public KtExpression getBody() {
        return findChildByClass(KtExpression.class);
    }

    @Nullable
    public PsiElement getOpenBraceNode() {
        KtExpression body = getBody();
        return (body instanceof KtBlockExpression) ? ((KtBlockExpression) body).getLBrace() : null;
    }

    @NotNull
    public PsiElement getOpenBraceNodeOrSelf() {
        PsiElement result = getOpenBraceNode();
        return result != null ? result : this;
    }

    @Nullable
    public PsiElement getInitKeyword() {
        return findChildByType(KtTokens.INIT_KEYWORD);
    }
}
