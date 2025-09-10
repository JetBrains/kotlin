/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

public class KtEmptyValueArgument extends KtElementImplStub<KotlinPlaceHolderStub<KtEmptyValueArgument>> implements KtExpression {
    public KtEmptyValueArgument(@NotNull ASTNode node) {
        super(node);
    }

    public KtEmptyValueArgument(@NotNull KotlinPlaceHolderStub<KtEmptyValueArgument> stub) {
        super(stub, KtStubBasedElementTypes.EMPTY_VALUE_ARGUMENT);
    }
}
