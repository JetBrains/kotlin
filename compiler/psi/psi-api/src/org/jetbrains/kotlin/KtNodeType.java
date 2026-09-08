/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtElement;

import java.util.function.Function;

public class KtNodeType extends IElementType {
    private final @NotNull Function<ASTNode, KtElement> myPsiFactory;

    public KtNodeType(@NotNull @NonNls String debugName, @NotNull Function<ASTNode, KtElement> psiFactory) {
        super(debugName, KotlinLanguage.INSTANCE);
        myPsiFactory = psiFactory;
    }

    public KtElement createPsi(ASTNode node) {
        assert node.getElementType() == this;
        return myPsiFactory.apply(node);
    }

    public static class KtLeftBoundNodeType extends KtNodeType {
        public KtLeftBoundNodeType(@NotNull @NonNls String debugName, @NotNull Function<ASTNode, KtElement> psiFactory) {
            super(debugName, psiFactory);
        }

        @Override
        public boolean isLeftBound() {
            return true;
        }
    }
}
