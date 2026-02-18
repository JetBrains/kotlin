/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets;

import java.util.Arrays;
import java.util.List;

/**
 * Represents the initializer list for an {@code enum} entry with constructor arguments.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * enum class Color(val rgb: Int) {
 *     RED(0xFF0000)
 * //     ^________^
 * }
 * }</pre>
 */
public class KtInitializerList extends KtElementImplStub<KotlinPlaceHolderStub<KtInitializerList>> {
    public KtInitializerList(@NotNull ASTNode node) {
        super(node);
    }

    public KtInitializerList(@NotNull KotlinPlaceHolderStub<KtInitializerList> stub) {
        super(stub, KtStubBasedElementTypes.INITIALIZER_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitInitializerList(this, data);
    }

    @NotNull
    public List<KtSuperTypeListEntry> getInitializers() {
        return Arrays.asList(getStubOrPsiChildren(KtTokenSets.SUPER_TYPE_LIST_ENTRIES, KtSuperTypeListEntry.ARRAY_FACTORY));
    }
}
