/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayFactory;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.resolution.KtResolvable;

/**
 * Represents a single entry in a super type list.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * class Foo : Bar
 * //          ^_^
 * }</pre>
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public class KtSuperTypeListEntry extends KtElementImplStub<KotlinPlaceHolderStub<? extends KtSuperTypeListEntry>> implements KtResolvable {
    /** A shared empty array, which can be reused to avoid unnecessary allocations. */
    public static final KtSuperTypeListEntry[] EMPTY_ARRAY = new KtSuperTypeListEntry[0];

    /** A factory for creating arrays of {@link KtSuperTypeListEntry}, used by the PSI child-access machinery. */
    public static ArrayFactory<KtSuperTypeListEntry> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new KtSuperTypeListEntry[count];

    @KtImplementationDetail
    public KtSuperTypeListEntry(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtSuperTypeListEntry(
            @NotNull KotlinPlaceHolderStub<? extends KtSuperTypeListEntry> stub,
            @NotNull IElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitSuperTypeListEntry(this, data);
    }

    /** Returns the type reference of the supertype named by this entry, or {@code null} if it is absent in incomplete code. */
    @Nullable
    public KtTypeReference getTypeReference() {
        return getStubOrPsiChild(KtNodeTypes.TYPE_REFERENCE, KtTypeReference.class);
    }

    /**
     * Returns the supertype as a {@link KtUserType}, or {@code null} if the type reference is absent or is not a user type (for example, a
     * function type).
     */
    @Nullable
    public KtUserType getTypeAsUserType() {
        KtTypeReference reference = getTypeReference();
        if (reference != null) {
            KtTypeElement element = reference.getTypeElement();
            if (element instanceof KtUserType) {
                return ((KtUserType) element);
            }
        }
        return null;
    }
}
