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

        IElementType callType = treeParent.getElementType();
        if (shouldSkipStubCreation(callType)) return false;

        return super.shouldCreateStub(node);
    }

    private static boolean shouldSkipStubCreation(IElementType callType) {
        // Argument list is preserved for materialized calls (even empty)
        if (callType == KtStubElementTypes.ANNOTATION_ENTRY || callType == KtStubElementTypes.CALL_EXPRESSION) {
            return false;
        }

        // Unsupported call type
        return true;
    }
}
