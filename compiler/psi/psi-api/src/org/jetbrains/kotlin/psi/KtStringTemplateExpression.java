/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.*;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

public class KtStringTemplateExpression extends KtElementImplStub<KotlinPlaceHolderStub<KtStringTemplateExpression>>
        implements KtExpression, PsiLanguageInjectionHost, ContributedReferenceHost {
    private static final TokenSet CLOSE_QUOTE_TOKEN_SET = TokenSet.create(KtTokens.CLOSING_QUOTE);

    public KtStringTemplateExpression(@NotNull ASTNode node) {
        super(node);
    }

    public KtStringTemplateExpression(@NotNull KotlinPlaceHolderStub<KtStringTemplateExpression> stub) {
        super(stub, KtStubBasedElementTypes.STRING_TEMPLATE);
    }

    @Override
    public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
        return KtExpressionImpl.Companion.replaceExpression(this, newElement, true, super::replace);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitStringTemplateExpression(this, data);
    }

    private static final TokenSet STRING_ENTRIES_TYPES = TokenSet.create(
            KtStubBasedElementTypes.LONG_STRING_TEMPLATE_ENTRY,
            KtStubBasedElementTypes.SHORT_STRING_TEMPLATE_ENTRY,
            KtStubBasedElementTypes.LITERAL_STRING_TEMPLATE_ENTRY,
            KtStubBasedElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY
    );

    /**
     * @return The interpolation prefix if it is defined
     *
     * @see KtStringInterpolationPrefix
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public KtStringInterpolationPrefix getInterpolationPrefix() {
        return getStubOrPsiChild(KtStubBasedElementTypes.STRING_INTERPOLATION_PREFIX);
    }

    @NotNull
    public KtStringTemplateEntry[] getEntries() {
        return getStubOrPsiChildren(STRING_ENTRIES_TYPES, KtStringTemplateEntry.EMPTY_ARRAY);
    }

    @Override
    public boolean isValidHost() {
        return getNode().getChildren(CLOSE_QUOTE_TOKEN_SET).length != 0;
    }

    @Override
    public PsiLanguageInjectionHost updateText(@NotNull String text) {
        KtExpression newExpression = new KtPsiFactory(getProject()).createExpressionIfPossible(text);
        if (newExpression instanceof KtStringTemplateExpression) return (KtStringTemplateExpression) replace(newExpression);
        return ElementManipulators.handleContentChange(this, text);
    }

    @NotNull
    @Override
    public LiteralTextEscaper<? extends PsiLanguageInjectionHost> createLiteralTextEscaper() {
        return new KotlinStringLiteralTextEscaper(this);
    }

    public boolean hasInterpolation() {
        for (PsiElement child : getChildren()) {
            if (child instanceof KtSimpleNameStringTemplateEntry || child instanceof KtBlockStringTemplateEntry) {
                return true;
            }
        }

        return false;
    }
}
