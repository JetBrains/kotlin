/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.SpecialNames;

/**
 * Represents the body of a lambda expression, containing parameters and the function body.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val sum = { x: Int, y: Int -> x + y }
 * //        ^_________________________^
 * }</pre>
 */
public class KtFunctionLiteral extends KtFunctionNotStubbed {
    public KtFunctionLiteral(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public boolean hasBlockBody() {
        return false;
    }

    @Override
    public String getName() {
        return SpecialNames.ANONYMOUS_STRING;
    }

    @Override
    public PsiElement getNameIdentifier() {
        return null;
    }

    /**
     * Returns {@code true} if the lambda declares its parameters explicitly (there is an {@code ->} arrow), as opposed
     * to using the implicit {@code it} parameter.
     */
    public boolean hasParameterSpecification() {
        return findChildByType(KtTokens.ARROW) != null;
    }

    @Override
    public KtBlockExpression getBodyExpression() {
        return (KtBlockExpression) super.getBodyExpression();
    }

    @Nullable
    @Override
    public PsiElement getEqualsToken() {
        return null;
    }

    /**
     * Returns the opening brace {@code &#123;} of the lambda.
     */
    @NotNull
    public PsiElement getLBrace() {
        return findChildByType(KtTokens.LBRACE);
    }

    /**
     * Returns the closing brace {@code &#125;} of the lambda, or {@code null} if it is absent in incomplete code.
     */
    @Nullable
    @IfNotParsed
    public PsiElement getRBrace() {
        return findChildByType(KtTokens.RBRACE);
    }

    /**
     * Returns the {@code ->} arrow separating the parameters from the body, or {@code null} if the lambda declares no
     * explicit parameters.
     */
    @Nullable
    public PsiElement getArrow() {
        return findChildByType(KtTokens.ARROW);
    }

    @Nullable
    @Override
    public FqName getFqName() {
        return null;
    }

    @Override
    public boolean hasBody() {
        return getBodyExpression() != null;
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        return new LocalSearchScope(this);
    }
}
