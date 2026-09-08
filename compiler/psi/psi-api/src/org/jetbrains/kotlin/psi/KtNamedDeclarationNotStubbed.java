/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;

/**
 * Base implementation of {@link KtNamedDeclaration} backed directly by the AST tree, for named declarations that do not yet have a
 * stub-based representation.
 *
 * <p>This is an internal implementation base class of the Kotlin PSI.
 *
 * @deprecated a transitional base class kept only until all named declarations get stubs; prefer the stub-capable
 * {@link KtNamedDeclarationStub}.
 */
// TODO: Remove when all named declarations get stubs
@Deprecated
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
abstract class KtNamedDeclarationNotStubbed extends KtDeclarationImpl implements KtNamedDeclaration {
    public KtNamedDeclarationNotStubbed(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        PsiElement identifier = getNameIdentifier();
        if (identifier != null) {
            String text = identifier.getText();
            return text != null ? KtPsiUtil.unquoteIdentifier(text) : null;
        }
        else {
            return null;
        }
    }

    @Override
    public Name getNameAsName() {
        String name = getName();
        return name != null ? Name.identifier(name) : null;
    }

    @Override
    @NotNull
    public Name getNameAsSafeName() {
        return KtPsiUtil.safeName(getName());
    }

    @Override
    public PsiElement getNameIdentifier() {
        return findChildByType(KtTokens.IDENTIFIER);
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        return KtPsiMutationService.getInstance().setNamedDeclarationName(this, name);
    }

    @Override
    public int getTextOffset() {
        PsiElement identifier = getNameIdentifier();
        return identifier != null ? identifier.getTextRange().getStartOffset() : getTextRange().getStartOffset();
    }
}
