/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtValueArgumentList;

public class KtValueArgumentListElementType extends KtPlaceHolderStubElementType<KtValueArgumentList> {
    public KtValueArgumentListElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtValueArgumentList.class);
    }

    @Override
    public boolean shouldCreateStub(ASTNode node) {
        ASTNode treeParent = node.getTreeParent();
        if (treeParent == null) return false;

        IElementType elementType = treeParent.getElementType();
        if (elementType == KtStubElementTypes.ANNOTATION_ENTRY) {
            KtValueArgumentList psi = node.getPsi(KtValueArgumentList.class);
            // Empty argument list is not preserved in annotations
            if (psi.getArguments().isEmpty()) return false;
        } else if (elementType == KtStubElementTypes.CALL_EXPRESSION) {

        } else {
            return false;
        }

        return super.shouldCreateStub(node);
    }
}
