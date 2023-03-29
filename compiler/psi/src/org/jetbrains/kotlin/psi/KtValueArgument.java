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
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.KotlinValueArgumentStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public class KtValueArgument extends KtElementImplStub<KotlinValueArgumentStub<? extends KtValueArgument>> implements ValueArgument {
    public KtValueArgument(@NotNull ASTNode node) {
        super(node);
    }

    public KtValueArgument(@NotNull KotlinValueArgumentStub<KtValueArgument> stub) {
        super(stub, KtStubElementTypes.VALUE_ARGUMENT);
    }

    protected KtValueArgument(KotlinValueArgumentStub<? extends KtValueArgument> stub, IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitArgument(this, data);
    }

    private static final TokenSet STRING_TEMPLATE_EXPRESSIONS_TYPES = TokenSet.create(
            KtStubElementTypes.STRING_TEMPLATE
    );

    @Override
    @Nullable @IfNotParsed
    public KtExpression getArgumentExpression() {
        KotlinPlaceHolderStub<? extends KtValueArgument> stub = getStub();
        if (stub != null) {
            KtExpression[] constantExpressions = stub.getChildrenByType(KtStubElementTypes.CONSTANT_EXPRESSIONS_TYPES, KtExpression.EMPTY_ARRAY);
            if (constantExpressions.length != 0) {
                return constantExpressions[0];
            }
        }

        return findChildByClass(KtExpression.class);
    }

    @Nullable
    public KtStringTemplateExpression getStringTemplateExpression() {
        KotlinPlaceHolderStub<? extends KtValueArgument> stub = getStub();
        KtExpression expression;
        if (stub != null) {
            KtExpression[] stringTemplateExpressions = stub.getChildrenByType(STRING_TEMPLATE_EXPRESSIONS_TYPES, KtExpression.EMPTY_ARRAY);
            expression = stringTemplateExpressions.length != 0 ? stringTemplateExpressions[0] : null;
        } else {
            expression = findChildByClass(KtExpression.class);
        }
        return expression instanceof KtStringTemplateExpression ? (KtStringTemplateExpression)expression : null;
    }

    @Override
    @Nullable
    public KtValueArgumentName getArgumentName() {
        return getStubOrPsiChild(KtStubElementTypes.VALUE_ARGUMENT_NAME);
    }

    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(KtTokens.EQ);
    }

    @Override
    public boolean isNamed() {
        return getArgumentName() != null;
    }

    @NotNull
    @Override
    public KtElement asElement() {
        return this;
    }

    @Override
    public LeafPsiElement getSpreadElement() {
        KotlinValueArgumentStub stub = getStub();
        if (stub != null && !stub.isSpread()) {
            return null;
        }

        ASTNode node = getNode().findChildByType(KtTokens.MUL);
        return node == null ? null : (LeafPsiElement) node.getPsi();
    }

    public boolean isSpread() {
        KotlinValueArgumentStub stub = getStub();
        if (stub != null) {
            return stub.isSpread();
        }

        return getSpreadElement() != null;
    }

    @Override
    public boolean isExternal() {
        return false;
    }
}
