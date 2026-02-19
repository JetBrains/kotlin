/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

/**
 * Represents the receiver type in a function type with receiver.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val block: String.() -> Int = { length }
 * //         ^_____^
 * }</pre>
 */
public class KtFunctionTypeReceiver extends KtElementImplStub<KotlinPlaceHolderStub<KtFunctionTypeReceiver>> {
    public KtFunctionTypeReceiver(@NotNull ASTNode node) {
        super(node);
    }

    public KtFunctionTypeReceiver(@NotNull KotlinPlaceHolderStub<KtFunctionTypeReceiver> stub) {
        super(stub, KtStubBasedElementTypes.FUNCTION_TYPE_RECEIVER);
    }

    @NotNull
    @SuppressWarnings("deprecation") // KT-78356
    public KtTypeReference getTypeReference() {
        return getRequiredStubOrPsiChild(KtStubBasedElementTypes.TYPE_REFERENCE);
    }
}
