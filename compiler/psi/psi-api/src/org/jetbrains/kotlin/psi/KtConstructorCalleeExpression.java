/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

/**
 * Represents the callee part in a constructor invocation, such as in annotations or super type calls.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    @MyAnnotation
 * // ^___________^
 * class Foo
 * }</pre>
 */
public class KtConstructorCalleeExpression extends KtExpressionImplStub<KotlinPlaceHolderStub<KtConstructorCalleeExpression>> {
    public KtConstructorCalleeExpression(@NotNull ASTNode node) {
        super(node);
    }

    public KtConstructorCalleeExpression(@NotNull KotlinPlaceHolderStub<KtConstructorCalleeExpression> stub) {
        super(stub, KtStubBasedElementTypes.CONSTRUCTOR_CALLEE);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitConstructorCalleeExpression(this, data);
    }

    @Nullable @IfNotParsed
    @SuppressWarnings("deprecation") // KT-78356
    public KtTypeReference getTypeReference() {
        return getStubOrPsiChild(KtStubBasedElementTypes.TYPE_REFERENCE);
    }

    @Nullable @IfNotParsed
    public KtSimpleNameExpression getConstructorReferenceExpression() {
        KtTypeReference typeReference = getTypeReference();
        if (typeReference == null) {
            return null;
        }
        KtTypeElement typeElement = typeReference.getTypeElement();
        if (!(typeElement instanceof KtUserType)) {
            return null;
        }
        return ((KtUserType) typeElement).getReferenceExpression();
    }

}
