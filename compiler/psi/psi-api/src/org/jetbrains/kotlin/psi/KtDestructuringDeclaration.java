/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;

import java.util.List;

import static org.jetbrains.kotlin.lexer.KtTokens.*;

/**
 * Represents a destructuring declaration that unpacks an object into multiple variables.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    val (x, y) = pair
 * // ^_______________^
 * }</pre>
 *
 * @see KtDestructuringDeclarationEntry
 */
public class KtDestructuringDeclaration extends KtDeclarationImpl
        implements KtValVarKeywordOwner, KtDeclarationWithInitializer, KtDeclarationWithReturnType {

    public KtDestructuringDeclaration(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitDestructuringDeclaration(this, data);
    }

    @NotNull
    public List<KtDestructuringDeclarationEntry> getEntries() {
        return findChildrenByType(KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY);
    }

    @Nullable
    @Override
    public KtExpression getInitializer() {
        ASTNode eqNode = getNode().findChildByType(EQ);
        if (eqNode == null) {
            return null;
        }
        return PsiTreeUtil.getNextSiblingOfType(eqNode.getPsi(), KtExpression.class);
    }

    @Override
    public boolean hasInitializer() {
        return getInitializer() != null;
    }

    public boolean isVar() {
        return getNode().findChildByType(KtTokens.VAR_KEYWORD) != null;
    }

    @Override
    @Nullable
    public PsiElement getValOrVarKeyword() {
        return findChildByType(VAL_VAR);
    }

    /**
     * Example:
     * <code>
     *     (val x, var y) = ...
     * </code>
     *
     * @return true when this destructuring declaration uses the full form with the val or var keywords inside the entries.
     * See <a href="https://kotl.in/name-based-destructuring">KEEP</a>.
     */
    public boolean isFullForm() {
        List<KtDestructuringDeclarationEntry> entries = getEntries();
        return !entries.isEmpty() && entries.get(0).getOwnValOrVarKeyword() != null;
    }

    private static final TokenSet OPENING_BRACES = TokenSet.create(LPAR, LBRACKET);
    private static final TokenSet CLOSING_BRACES = TokenSet.create(RPAR, RBRACKET);

    @Nullable
    public PsiElement getRPar() {
        return findChildByType(CLOSING_BRACES);
    }

    @Nullable
    public PsiElement getLPar() {
        return findChildByType(OPENING_BRACES);
    }

    /**
     * @return true when this destructuring declaration uses square brackets.
     */
    public boolean hasSquareBrackets() {
        return getNode().findChildByType(LBRACKET) != null;
    }

    @Nullable
    public PsiElement getTrailingComma() {
        return KtPsiUtilKt.getTrailingCommaByClosingElement(getRPar());
    }

    @Nullable
    @Override
    public KtTypeReference getTypeReference() {
        return null;
    }
}
