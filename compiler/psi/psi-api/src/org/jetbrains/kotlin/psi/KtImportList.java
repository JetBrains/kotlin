/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

import java.util.List;

/**
 * Represents the list of {@code import} directives in a Kotlin file.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    import kotlin.collections.List
 *    import kotlin.io.println
 * // ^____________________________^
 * }</pre>
 */
public class KtImportList extends KtElementImplStub<KotlinPlaceHolderStub<KtImportList>> {
    /** A shared empty array, which can be reused to avoid unnecessary allocations. */
    public static final KtImportList[] EMPTY_ARRAY = new KtImportList[0];

    @KtImplementationDetail
    public KtImportList(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtImportList(@NotNull KotlinPlaceHolderStub<KtImportList> stub) {
        super(stub, KtNodeTypes.IMPORT_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitImportList(this, data);
    }

    /** Returns the import directives in this list, in source order; empty if there are none. */
    @NotNull
    public List<KtImportDirective> getImports() {
        return getStubOrPsiChildrenAsList(KtStubBasedElementTypes.IMPORT_DIRECTIVE);
    }
}
