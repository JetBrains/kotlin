/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

/**
 * Represents an interface delegation in the super type list using the {@code by} keyword.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * class Foo(b: Base) : Base by b
 * //                   ^_______^
 * }</pre>
 */
public class KtDelegatedSuperTypeEntry extends KtSuperTypeListEntry {
    public KtDelegatedSuperTypeEntry(@NotNull ASTNode node) {
        super(node);
    }

    public KtDelegatedSuperTypeEntry(@NotNull KotlinPlaceHolderStub<? extends KtSuperTypeListEntry> stub) {
        super(stub, KtStubBasedElementTypes.DELEGATED_SUPER_TYPE_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitDelegatedSuperTypeEntry(this, data);
    }

    @Nullable @IfNotParsed
    public KtExpression getDelegateExpression() {
        return findChildByClass(KtExpression.class);
    }

    public ASTNode getByKeywordNode() {
        return getNode().findChildByType(KtTokens.BY_KEYWORD);
    }
}
