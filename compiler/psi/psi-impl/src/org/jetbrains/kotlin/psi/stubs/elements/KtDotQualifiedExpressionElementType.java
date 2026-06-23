/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression;
import org.jetbrains.kotlin.psi.KtImplementationDetail;
import org.jetbrains.kotlin.psi.stubs.StubUtils;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinPlaceHolderStubFactory;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl;

@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public class KtDotQualifiedExpressionElementType extends KtPlaceHolderStubElementType<KtDotQualifiedExpression> {
    private final StubElementFactory<KotlinPlaceHolderStubImpl<KtDotQualifiedExpression>, KtDotQualifiedExpression> stubFactory =
            new KotlinPlaceHolderStubFactory<KtDotQualifiedExpression>(this) {
                @Override
                public boolean shouldCreateStub(@NotNull ASTNode node) {
                    ASTNode treeParent = node.getTreeParent();
                    if (treeParent == null) return false;

                    IElementType parentElementType = treeParent.getElementType();
                    if (parentElementType == KtStubElementTypes.IMPORT_DIRECTIVE ||
                        parentElementType == KtStubElementTypes.PACKAGE_DIRECTIVE ||
                        parentElementType == KtStubElementTypes.DOT_QUALIFIED_EXPRESSION ||
                        StubUtils.isDeclaredInsideValueArgument$org_jetbrains_kotlin_psi_impl(node)
                    ) {
                        return super.shouldCreateStub(node);
                    }

                    return false;
                }
            };

    public KtDotQualifiedExpressionElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtDotQualifiedExpression.class);
    }

    @KtImplementationDetail
    @Override
    public StubElementFactory<KotlinPlaceHolderStubImpl<KtDotQualifiedExpression>, KtDotQualifiedExpression> getStubFactory() {
        return stubFactory;
    }
}
