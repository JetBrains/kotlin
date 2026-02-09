/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
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
    public KtValueArgumentName(@NotNull ASTNode node) {
        super(node);
    }

    public KtValueArgumentName(@NotNull KotlinPlaceHolderStub<KtValueArgumentName> stub) {
        super(stub, KtStubBasedElementTypes.VALUE_ARGUMENT_NAME);
    }

    @Override
    @NotNull
    @SuppressWarnings("deprecation") // KT-78356
    public KtSimpleNameExpression getReferenceExpression() {
        return getStubOrPsiChild(KtStubBasedElementTypes.REFERENCE_EXPRESSION);
    }

    @NotNull
    @Override
    public Name getAsName() {
        return getReferenceExpression().getReferencedNameAsName();
    }
}
