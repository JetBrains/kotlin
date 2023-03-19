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
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinClassLiteralExpressionStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public class KtClassLiteralExpression extends KtElementImplStub<KotlinClassLiteralExpressionStub> implements KtDoubleColonExpression {
    public KtClassLiteralExpression(KotlinClassLiteralExpressionStub stub) {
        super(stub, KtStubElementTypes.CLASS_LITERAL_EXPRESSION);
    }

    public KtClassLiteralExpression(@NotNull ASTNode node) {
        super(node);
    }


    private static final TokenSet CLASS_REFS = TokenSet.create(
            KtStubElementTypes.REFERENCE_EXPRESSION,
            KtStubElementTypes.DOT_QUALIFIED_EXPRESSION
    );

    @Nullable
    @Override
    public KtExpression getReceiverExpression() {
        KotlinClassLiteralExpressionStub stub = getStub();
        if (stub != null) {
            KtExpression[] expressions = stub.getChildrenByType(CLASS_REFS, KtExpression.EMPTY_ARRAY);
            if (expressions.length == 1) {
                return expressions[0];
            }
        }
        return KtDoubleColonExpression.super.getReceiverExpression();
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitClassLiteralExpression(this, data);
    }

    @Nullable
    @Override
    public PsiElement findColonColon() {
        ASTNode child = getNode().findChildByType(KtTokens.COLONCOLON);
        return child != null ? child.getPsi() : null;
    }
}
