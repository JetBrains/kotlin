/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinClassLiteralExpressionStub;

/**
 * Represents a class literal expression that gets a class reference.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val clazz = String::class
 * //          ^__________^
 * }</pre>
 */
public class KtClassLiteralExpression extends KtElementImplStub<KotlinClassLiteralExpressionStub> implements KtDoubleColonExpression {
    public KtClassLiteralExpression(KotlinClassLiteralExpressionStub stub) {
        super(stub, KtStubBasedElementTypes.CLASS_LITERAL_EXPRESSION);
    }

    public KtClassLiteralExpression(@NotNull ASTNode node) {
        super(node);
    }


    private static final TokenSet CLASS_REFS = TokenSet.create(
            KtStubBasedElementTypes.REFERENCE_EXPRESSION,
            KtStubBasedElementTypes.DOT_QUALIFIED_EXPRESSION
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
