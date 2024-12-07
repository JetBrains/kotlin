/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinCollectionLiteralExpressionStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.Arrays;
import java.util.List;

import static org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt.getTrailingCommaByClosingElement;

public class KtCollectionLiteralExpression extends KtElementImplStub<KotlinCollectionLiteralExpressionStub> implements KtReferenceExpression {
    public KtCollectionLiteralExpression(@NotNull KotlinCollectionLiteralExpressionStub stub) {
        super(stub, KtStubElementTypes.COLLECTION_LITERAL_EXPRESSION);
    }

    public KtCollectionLiteralExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitCollectionLiteralExpression(this, data);
    }

    @Nullable
    public PsiElement getLeftBracket() {
        ASTNode astNode = getNode().findChildByType(KtTokens.LBRACKET);
        return astNode != null ? astNode.getPsi() : null;
    }

    @Nullable
    public PsiElement getRightBracket() {
        ASTNode astNode = getNode().findChildByType(KtTokens.RBRACKET);
        return astNode != null ? astNode.getPsi() : null;
    }

    @Nullable
    public PsiElement getTrailingComma() {
        PsiElement rightBracket = getRightBracket();
        return getTrailingCommaByClosingElement(rightBracket);
    }

    public List<KtExpression> getInnerExpressions() {
        KotlinCollectionLiteralExpressionStub stub = getStub();
        if (stub != null) {
            return Arrays.asList(stub.getChildrenByType(KtStubElementTypes.CONSTANT_EXPRESSIONS_TYPES, KtExpression.EMPTY_ARRAY));
        }
        return PsiTreeUtil.getChildrenOfTypeAsList(this, KtExpression.class);
    }
}