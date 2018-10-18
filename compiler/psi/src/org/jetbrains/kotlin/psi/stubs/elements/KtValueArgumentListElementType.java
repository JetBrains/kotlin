/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
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
        if (treeParent == null || treeParent.getElementType() != KtStubElementTypes.ANNOTATION_ENTRY) {
            return false;
        }

        KtValueArgumentList psi = node.getPsi(KtValueArgumentList.class);
        if (psi.getArguments().isEmpty()) return false;

        return super.shouldCreateStub(node);
    }
}
