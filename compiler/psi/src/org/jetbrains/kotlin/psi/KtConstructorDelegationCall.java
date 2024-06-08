/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.parsing.KotlinParsing;

import java.util.Collections;
import java.util.List;

/**
 * The code example:
 * <pre>{@code
 * class SimpleClass(i: Int) {
 *     constructor(s: String): this(s.toInt())
 * //                         ^______________^
 * }
 * }</pre>
 */
public class KtConstructorDelegationCall extends KtElementImpl implements KtCallElement {
    public KtConstructorDelegationCall(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitConstructorDelegationCall(this, data);
    }

    @Override
    @Nullable
    public KtValueArgumentList getValueArgumentList() {
        return (KtValueArgumentList) findChildByType(KtNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @Override
    @NotNull
    public List<? extends ValueArgument> getValueArguments() {
        KtValueArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<KtValueArgument>emptyList();
    }

    @NotNull
    @Override
    public List<KtLambdaArgument> getLambdaArguments() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<KtTypeProjection> getTypeArguments() {
        return Collections.emptyList();
    }

    @Override
    public KtTypeArgumentList getTypeArgumentList() {
        return null;
    }

    @Nullable
    @Override
    public KtConstructorDelegationReferenceExpression getCalleeExpression() {
        return findChildByClass(KtConstructorDelegationReferenceExpression.class);
    }

    /**
     * @return true if this delegation call is not present in the source code. Note that we always parse delegation calls
     * for secondary constructors, even if there's no explicit call in the source (see {@link KotlinParsing#parseSecondaryConstructor}).
     *
     *     class Foo {
     *         constructor(name: String)   // <--- implicit constructor delegation call (empty element after RPAR)
     *     }
     */
    public boolean isImplicit() {
        KtConstructorDelegationReferenceExpression callee = getCalleeExpression();
        return callee != null && callee.getFirstChild() == null;
    }

    public boolean isCallToThis() {
        KtConstructorDelegationReferenceExpression callee = getCalleeExpression();
        return callee != null && callee.isThis();
    }
}
