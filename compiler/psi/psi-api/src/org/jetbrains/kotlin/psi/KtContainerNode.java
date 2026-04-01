/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an auxiliary container node that wraps other PSI elements without adding semantic meaning.
 * Used internally by the parser to group related elements in the syntax tree.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * if (condition) statement else alternative
 * //             ^_______^
 * }</pre>
 */
public class KtContainerNode extends KtElementImpl {
    public KtContainerNode(@NotNull ASTNode node) {
        super(node);
    }

    @Override // for visibility
    protected <T> T findChildByClass(Class<T> aClass) {
        return super.findChildByClass(aClass);
    }

    @Override // for visibility
    protected <T extends PsiElement> T findChildByType(IElementType type) {
        return super.findChildByType(type);
    }
}
