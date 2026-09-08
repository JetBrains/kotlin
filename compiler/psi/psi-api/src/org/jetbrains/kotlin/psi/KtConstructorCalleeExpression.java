/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.resolution.KtResolvableCall;

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
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public class KtConstructorCalleeExpression extends KtExpressionImplStub<KotlinPlaceHolderStub<KtConstructorCalleeExpression>> implements KtResolvableCall {
    @KtImplementationDetail
    public KtConstructorCalleeExpression(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtConstructorCalleeExpression(@NotNull KotlinPlaceHolderStub<KtConstructorCalleeExpression> stub) {
        super(stub, KtNodeTypes.CONSTRUCTOR_CALLEE);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitConstructorCalleeExpression(this, data);
    }

    /** Returns the type reference naming the class being constructed, or {@code null} if it is absent in incomplete code. */
    @Nullable @IfNotParsed
    public KtTypeReference getTypeReference() {
        return getStubOrPsiChild(KtNodeTypes.TYPE_REFERENCE, KtTypeReference.class);
    }

    /**
     * Returns the reference expression naming the invoked constructor's class, or {@code null} if it cannot be determined (for example,
     * when the callee is not a simple user type).
     */
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
