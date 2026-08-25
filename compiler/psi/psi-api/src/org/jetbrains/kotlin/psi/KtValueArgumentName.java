/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

/**
 * Represents the name part of a named argument.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * greet(name = "World")
 * //    ^__^
 * }</pre>
 */
public class KtValueArgumentName extends KtElementImplStub<KotlinPlaceHolderStub<KtValueArgumentName>> implements ValueArgumentName {
    @KtImplementationDetail
    public KtValueArgumentName(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtValueArgumentName(@NotNull KotlinPlaceHolderStub<KtValueArgumentName> stub) {
        super(stub, KtNodeTypes.VALUE_ARGUMENT_NAME);
    }

    @Override
    @NotNull
    public KtSimpleNameExpression getReferenceExpression() {
        return getRequiredStubOrPsiChild(KtNodeTypes.REFERENCE_EXPRESSION, KtNameReferenceExpression.class);
    }

    @NotNull
    @Override
    public Name getAsName() {
        return getReferenceExpression().getReferencedNameAsName();
    }
}
