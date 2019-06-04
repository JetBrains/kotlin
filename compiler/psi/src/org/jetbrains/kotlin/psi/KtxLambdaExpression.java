/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.SharedImplUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.IncorrectOperationException;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;

import java.util.Objects;

public class KtxLambdaExpression extends KtLambdaExpression {

    @NotNull ASTNode node;

    public KtxLambdaExpression(@NotNull ASTNode node) {
        super(null);
        this.node = node;
    }

    @Override
    public PsiElement getParent() {
        return SharedImplUtil.getParent(getNode());
    }

    @NotNull
    @Override
    public PsiElement[] getChildren() {
        ASTNode[] nodes = node.getChildren(null);
        PsiElement[] results = new PsiElement[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            results[i] = nodes[i].getPsi();
        }
        return results;
    }

    @Override
    public PsiElement getFirstChild() {
        return node.getFirstChildNode().getPsi();
    }

    @Override
    public PsiElement getLastChild() {
        return node.getLastChildNode().getPsi();
    }

    @Override
    public PsiElement getNextSibling() {
        return SharedImplUtil.getNextSibling(node);
    }

    @Override
    public PsiElement getPrevSibling() {
        return SharedImplUtil.getPrevSibling(node);
    }

    @NotNull
    @Override
    public String getText() {
        return node.getText();
    }

    @Override
    public boolean isParsed() {
        return true;
    }

    @Override
    public LeafElement findLeafElementAt(int offset) {
        ASTNode leaf = node.findLeafElementAt(offset);
        if (leaf instanceof LeafElement) {
            return (LeafElement)leaf;
        } else {
            throw new RuntimeException("expected LeafElement");
        }
    }

    @Nullable
    @Override
    public PsiElement findPsiChildByType(IElementType type) {
        ASTNode child = node.findChildByType(type);
        if (child == null) return null;
        return child.getPsi();
    }

    @Nullable
    @Override
    public PsiElement findPsiChildByType(TokenSet types) {
        ASTNode child = node.findChildByType(types);
        if (child == null) return null;
        return child.getPsi();
    }

    @Override
    public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
        return KtxUtilsKt.replaceImpl(this, newElement);
    }

    @Override
    public ASTNode findChildByType(IElementType type) {
        return node.findChildByType(type);
    }

    @Override
    public ASTNode findChildByType(IElementType type, ASTNode anchor) {
        return node.findChildByType(type, anchor);
    }

    @Nullable
    @Override
    public ASTNode findChildByType(@NotNull TokenSet types) {
        return node.findChildByType(types);
    }

    @Nullable
    @Override
    public ASTNode findChildByType(@NotNull TokenSet typesSet, ASTNode anchor) {
        return node.findChildByType(typesSet, anchor);
    }

    @NotNull
    @Override
    public ASTNode[] getChildren(@Nullable TokenSet filter) {
        return node.getChildren(filter);
    }

    @Override
    public TextRange getTextRange() {
        return node.getTextRange();
    }

    @Override
    public int getStartOffset() {
        return node.getStartOffset();
    }

    @Override
    public PsiManagerEx getManager() {
        return (PsiManagerEx)getParent().getManager();
    }

    @Override
    @NotNull
    public ASTNode getNode() {
        return node;
    }

    @NotNull
    @Override
    public KtFunctionLiteral getFunctionLiteral() {
        return Objects.requireNonNull(node.findChildByType(KtNodeTypes.FUNCTION_LITERAL)).getPsi(KtFunctionLiteral.class);
    }

    @Override
    public PsiFile getContainingFile() {
        return SharedImplUtil.getContainingFile(node);
    }

    @Override
    public boolean isValid() {
        PsiFile file = getContainingFile();
        return file != null && file.isValid();
    }
}
