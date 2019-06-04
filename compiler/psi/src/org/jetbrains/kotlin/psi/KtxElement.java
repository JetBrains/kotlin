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
import com.intellij.psi.impl.source.tree.PsiErrorElementImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;

import java.util.ArrayList;
import java.util.List;

public class KtxElement extends KtExpressionImpl implements KtStatementExpression {

    public KtxElement(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitKtxElement(this, data);
    }

    @Nullable
    public KtSimpleNameExpression getSimpleTagName() {
        return findChildByType(KtNodeTypes.REFERENCE_EXPRESSION);
    }

    @Nullable
    public KtDotQualifiedExpression getQualifiedTagName() {
        return findChildByType(KtNodeTypes.DOT_QUALIFIED_EXPRESSION);
    }

    @Nullable
    public KtSimpleNameExpression getSimpleClosingTagName() {
        List<KtSimpleNameExpression> simpleTagNames = findChildrenByType(KtNodeTypes.REFERENCE_EXPRESSION);
        return simpleTagNames.size() == 2 ? simpleTagNames.get(1) : null;
    }

    @Nullable
    public KtDotQualifiedExpression getQualifiedClosingTagName() {
        List<KtDotQualifiedExpression> qualifiedTagNames = findChildrenByType(KtNodeTypes.DOT_QUALIFIED_EXPRESSION);
        return qualifiedTagNames.size() == 2 ? qualifiedTagNames.get(1) : null;
    }

    @NotNull
    public List<KtxAttribute> getAttributes() {
        List<KtxAttribute> attributes = findChildrenByType(KtNodeTypes.KTX_ATTRIBUTE);
        if(attributes != null) return attributes;
        else return new ArrayList<KtxAttribute>();
    }

    @Nullable
    public KtLambdaExpression getBodyLambdaExpression() {
        return this.findChildByType(KtNodeTypes.KTX_BODY_LAMBDA);
    }

    @Nullable
    public List<KtExpression> getBody() {
        KtLambdaExpression fun = getBodyLambdaExpression();
        if (fun == null) return null;
        List<KtExpression> body = new ArrayList<KtExpression>();
        for(PsiElement element : fun.getBodyExpression().getChildren())
            if(element instanceof KtExpression) body.add((KtExpression)element);
            else if(element instanceof PsiErrorElementImpl) continue;
            else throw new Error("Unexpected node type: "+element.getClass());
        return body;
    }

    public List<PsiElement> getBracketsElements() {
        List<PsiElement> elements = new ArrayList<>(5);
        elements.addAll(findChildrenByType(KtTokens.LT));
        elements.addAll(findChildrenByType(KtTokens.GT));
        elements.addAll(findChildrenByType(KtTokens.DIV));
        return elements;
    }
}
