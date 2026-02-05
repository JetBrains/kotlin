/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
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

    public KtImportList(@NotNull ASTNode node) {
        super(node);
    }

    public KtImportList(@NotNull KotlinPlaceHolderStub<KtImportList> stub) {
        super(stub, KtStubBasedElementTypes.IMPORT_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitImportList(this, data);
    }

    @NotNull
    public List<KtImportDirective> getImports() {
        return getStubOrPsiChildrenAsList(KtStubBasedElementTypes.IMPORT_DIRECTIVE);
    }
}
