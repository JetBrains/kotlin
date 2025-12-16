/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.IncorrectOperationException;
import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.kotlin.KtNodeTypes.BLOCK;

@SuppressWarnings("deprecation")
public class KtBlockExpression extends LazyParseablePsiElement implements KtElement, KtExpression, KtStatementExpression {

    public KtBlockExpression(@Nullable CharSequence text) {
        super(BLOCK, text);
    }

    @SuppressWarnings({"unused", "MethodMayBeStatic"}) //keep for compatibility with potential plugins
    public boolean shouldChangeModificationCount(PsiElement place) {
        // To prevent OutOfBlockModification increase from JavaCodeBlockModificationListener
        return false;
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }

    @NotNull
    @Override
    public KtFile getContainingKtFile() {
        return PsiUtilsKt.getContainingKtFile(this);
    }

    @Override
    public <D> void acceptChildren(@NotNull KtVisitor<Void, D> visitor, D data) {
        KtPsiUtil.visitChildren(this, visitor, data);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitBlockExpression(this, data);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof KtVisitor) {
            accept((KtVisitor) visitor, null);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @Override
    public void delete() throws IncorrectOperationException {
        KtElementUtilsKt.deleteSemicolon(this);
        super.delete();
    }

    @Override
    @NotNull
    public PsiElement[] getChildren() {
        PsiElement psiChild = getFirstChild();

        List<PsiElement> result = null;
        while (psiChild != null) {
            if (psiChild.getNode() instanceof CompositeElement) {
                if(result == null) result = new ArrayList<>();
                result.add(psiChild);
            }
            psiChild = psiChild.getNextSibling();
        }
        return result == null ? PsiElement.EMPTY_ARRAY : PsiUtilCore.toPsiElementArray(result);
    }

    @NotNull
    @Override
    public KtElement getPsiOrParent() {
        return this;
    }

    @Nullable
    public KtExpression getFirstStatement() {
        return findChildByClass(KtExpression.class);
    }

    @ReadOnly
    @NotNull
    public List<KtExpression> getStatements() {
        return Arrays.asList(findChildrenByClass(KtExpression.class));
    }

    @Nullable
    public TextRange getLastBracketRange() {
        PsiElement rBrace = getRBrace();
        return rBrace != null ? rBrace.getTextRange() : null;
    }

    @Nullable
    public PsiElement getRBrace() {
        return findPsiChildByType(KtTokens.RBRACE);
    }

    @Nullable
    public PsiElement getLBrace() {
        return findPsiChildByType(KtTokens.LBRACE);
    }
}
